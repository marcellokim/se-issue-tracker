package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue fix and resolve")
class IssueFixResolveTest {

        private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null,
                        null);
        private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
        private final User otherDeveloper = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, null,
                        null);
        private final User otherTester = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true,
                        null, null);
        private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("assignee marks an issue fixed")
        void markAssignedIssueFixed() {
                var issue = assignedIssue();
                var fixedAt = createdAt.plusMinutes(20);

                issue.markFixed(assignee, "Fix completed", fixedAt);

                assertSame(assignee, issue.getFixer());
                assertEquals(IssueStatus.FIXED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.ASSIGNED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.FIXED.name(), history.getNewValue());
                assertEquals("Fix completed", history.getMessage());
                assertSame(assignee, history.getChangedBy());
        }

        @Test
        @DisplayName("verifier resolves a fixed issue")
        void resolveFixedIssue() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                var resolvedAt = createdAt.plusMinutes(30);

                issue.resolve(verifier, "Verified", resolvedAt);

                assertSame(assignee, issue.getFixer());
                assertSame(verifier, issue.getResolver());
                assertNull(issue.getAssignee());
                assertNull(issue.getVerifier());
                assertNull(issue.assigneeId());
                assertNull(issue.verifierId());
                assertEquals(IssueStatus.RESOLVED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.RESOLVED.name(), history.getNewValue());
                assertEquals("Verified", history.getMessage());
                assertSame(verifier, history.getChangedBy());
        }

        @Test
        @DisplayName("only current assignee can mark fixed")
        void onlyCurrentAssigneeCanMarkFixed() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(otherDeveloper, "Fix completed", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("only current verifier can resolve")
        void onlyCurrentVerifierCanResolve() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(otherTester, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("fixer and resolver have the right roles")
        void rejectsWrongFixerAndResolverRoles() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(verifier, "Fix completed", createdAt.plusMinutes(20)));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(assignee, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("wrong status cannot move forward")
        void rejectsWrongSourceStatus() {
                var newIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                var assignedIssue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> newIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20)));
                assertThrows(IllegalStateException.class,
                                () -> assignedIssue.resolve(verifier, "Verified", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("status changes need a comment")
        void rejectsBlankStatusComment() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(assignee, "", createdAt.plusMinutes(20)));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(verifier, " ", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("inactive users cannot fix or resolve")
        void rejectsInactiveFixerAndResolver() {
                var inactiveFixer = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
                var inactiveResolver = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null,
                                null);
                inactiveFixer.deactivate(createdAt.plusMinutes(1));
                inactiveResolver.deactivate(createdAt.plusMinutes(1));

                var issueForFixer = assignedIssue();
                assertThrows(IllegalArgumentException.class,
                                () -> issueForFixer.markFixed(inactiveFixer, "Fix completed",
                                                createdAt.plusMinutes(20)));

                var issueForResolver = assignedIssue();
                issueForResolver.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issueForResolver.resolve(inactiveResolver, "Verified",
                                                createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("verifier can send a fixed issue back")
        void verifierSendsFixedIssueBack() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                issue.rejectFix(verifier, "Not fixed properly", createdAt.plusMinutes(30));

                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.ASSIGNED.name(), history.getNewValue());
        }

        @Test
        @DisplayName("only current verifier can reject a fix")
        void onlyCurrentVerifierCanRejectFix() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.rejectFix(otherTester, "Not fixed", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("only fixed issues can be sent back")
        void onlyFixedIssuesCanBeSentBack() {
                var issue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> issue.rejectFix(verifier, "Not fixed", createdAt.plusMinutes(20)));
        }

        private Issue assignedIssue() {
                return assignedIssue("ISSUE-1");
        }

        private Issue assignedIssue(String issueId) {
                var issue = IssueFixtures.create(issueId, "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                return issue;
        }
}
