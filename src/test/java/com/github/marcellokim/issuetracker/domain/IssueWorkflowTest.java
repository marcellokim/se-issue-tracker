package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue workflow")
class IssueWorkflowTest {
        private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null,
                        null);
        private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
        private final User otherDeveloper = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, null,
                        null);
        private final User otherTester = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true,
                        null,
                        null);
        private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("new issue gets assignee and verifier")
        void assignNewIssue() {
                var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                var assignedAt = createdAt.plusMinutes(10);

                issue.assignFromNew(assignee, verifier, pl, assignedAt);

                assertSame(assignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
                assertEquals(3, issue.getHistories().size());

                var assignmentHistory = findHistory(issue, ActionType.ASSIGNMENT_CHANGED);
                assertEquals(ActionType.ASSIGNMENT_CHANGED, assignmentHistory.getAction());

                var statusHistory = findHistory(issue, ActionType.STATUS_CHANGED);
                assertEquals(IssueStatus.NEW.name(), statusHistory.getPreviousValue());
                assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
        }

        @Test
        @DisplayName("reopened issue can be assigned again")
        void assignReopenedIssue() {
                var issue = reopenedIssue();
                var previousFixer = issue.getFixer();
                var previousResolver = issue.getResolver();

                issue.assignReopened(assignee, verifier, pl, createdAt.plusMinutes(40));

                assertSame(assignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertSame(previousFixer, issue.getFixer());
                assertSame(previousResolver, issue.getResolver());
                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        }

        @Test
        @DisplayName("assigned issue changes assignee only")
        void reassignAssigneeOnly() {
                var issue = assignedIssue();
                var reassignedAt = createdAt.plusMinutes(20);

                issue.reassignAssignee(otherDeveloper, pl, reassignedAt);

                assertSame(otherDeveloper, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());

                var history = issue.getHistories().getLast();
                assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
                assertEquals(assignee.getLoginId(), history.getPreviousValue());
                assertEquals(otherDeveloper.getLoginId(), history.getNewValue());
                assertEquals(reassignedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("fixed issue changes verifier only")
        void changeVerifierOnly() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                var changedAt = createdAt.plusMinutes(30);

                issue.changeVerifier(otherTester, pl, changedAt);

                assertSame(assignee, issue.getAssignee());
                assertSame(otherTester, issue.getVerifier());
                assertSame(assignee, issue.getFixer());
                assertEquals(IssueStatus.FIXED, issue.getStatus());

                var history = issue.getHistories().getLast();
                assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
                assertEquals(verifier.getLoginId(), history.getPreviousValue());
                assertEquals(otherTester.getLoginId(), history.getNewValue());
                assertEquals(changedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("assignee is dev and verifier is tester")
        void assignmentNeedsDevAndTester() {
                var invalidAssigneeIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null,
                                reporter,
                                createdAt);
                var invalidVerifierIssue = IssueFixtures.create("ISSUE-2", "Signup fails", "Cannot sign up", null,
                                reporter,
                                createdAt);

                assertThrows(IllegalArgumentException.class,
                                () -> invalidAssigneeIssue.assignFromNew(verifier, verifier, pl, createdAt));
                assertThrows(IllegalArgumentException.class,
                                () -> invalidVerifierIssue.assignFromNew(assignee, assignee, pl, createdAt));
        }

        @Test
        @DisplayName("inactive users are not assigned")
        void inactiveUsersAreNotAssigned() {
                var inactiveAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
                var inactiveVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null,
                                null);
                inactiveAssignee.deactivate(createdAt.plusMinutes(1));
                inactiveVerifier.deactivate(createdAt.plusMinutes(1));

                var issueForAssignee = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                assertThrows(IllegalArgumentException.class,
                                () -> issueForAssignee.assignFromNew(inactiveAssignee, verifier,
                                                pl, createdAt.plusMinutes(10)));

                var issueForVerifier = IssueFixtures.create("ISSUE-2", "Signup fails", "Cannot sign up", null, reporter,
                                createdAt);
                assertThrows(IllegalArgumentException.class,
                                () -> issueForVerifier.assignFromNew(assignee, inactiveVerifier,
                                                pl, createdAt.plusMinutes(10)));
        }

        @Test
        @DisplayName("first assignment is only for new issues")
        void rejectAssignAgain() {
                var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));

                assertThrows(IllegalStateException.class,
                                () -> issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("assignment edits need the right status")
        void rejectWrongAssignmentEdit() {
                var newIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                var issue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> newIssue.reassignAssignee(otherDeveloper, pl, createdAt.plusMinutes(10)));
                assertThrows(IllegalStateException.class,
                                () -> issue.changeVerifier(otherTester, pl, createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("same assignee is not reassigned")
        void sameAssigneeIsNotReassigned() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.reassignAssignee(assignee, pl, createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("same verifier is not changed again")
        void rejectSameVerifier() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.changeVerifier(verifier, pl, createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("assignment edits check role and activity")
        void rejectBadAssignmentEdit() {
                var assignedIssue = assignedIssue();
                var fixedIssue = assignedIssue();
                fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                var inactiveDev = User.fromPersistence("dev3", "Dev Three", "hash", Role.DEV, true, null, null);
                inactiveDev.deactivate(createdAt.plusMinutes(1));

                assertThrows(IllegalArgumentException.class,
                                () -> assignedIssue.reassignAssignee(verifier, pl, createdAt.plusMinutes(20)));
                assertThrows(IllegalArgumentException.class,
                                () -> assignedIssue.reassignAssignee(inactiveDev, pl, createdAt.plusMinutes(20)));
                assertThrows(IllegalArgumentException.class,
                                () -> fixedIssue.changeVerifier(assignee, pl, createdAt.plusMinutes(30)));
        }

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
        void rejectWrongFixer() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(otherDeveloper, "Fix completed", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("only current verifier can resolve")
        void rejectWrongResolver() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(otherTester, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("fixer and resolver have the right roles")
        void rejectBadFixerAndResolver() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(verifier, "Fix completed", createdAt.plusMinutes(20)));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(assignee, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("workflow does not skip steps")
        void rejectSkippedWorkflow() {
                var newIssue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                var issue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> newIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20)));
                assertThrows(IllegalStateException.class,
                                () -> issue.resolve(verifier, "Verified", createdAt.plusMinutes(20)));
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
        void rejectInactiveFixerAndResolver() {
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
        void rejectWrongRejecter() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.rejectFix(otherTester, "Not fixed", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("only fixed issues can be sent back")
        void rejectSendBackFromAssigned() {
                var issue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> issue.rejectFix(verifier, "Not fixed", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("reopening resolved issue clears active assignment")
        void reopenResolvedIssue() {
                var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
                var changedAt = createdAt.plusMinutes(40);

                issue.reopen(pl, "Needs more work", changedAt);

                assertEquals(IssueStatus.REOPENED, issue.getStatus());
                assertNull(issue.getAssignee());
                assertNull(issue.getVerifier());
                assertSame(assignee, issue.getFixer());
                assertSame(verifier, issue.getResolver());
                assertEquals(6, issue.getHistories().size());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.RESOLVED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.REOPENED.name(), history.getNewValue());
                assertEquals("Needs more work", history.getMessage());
                assertEquals(changedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("closing resolved issue clears active assignment")
        void closeResolvedIssue() {
                var issue = resolvedIssue();
                var closedAt = createdAt.plusMinutes(40);

                issue.close(pl, "Release completed", closedAt);

                assertEquals(IssueStatus.CLOSED, issue.getStatus());
                assertNull(issue.getAssignee());
                assertNull(issue.getVerifier());
                assertSame(assignee, issue.getFixer());
                assertSame(verifier, issue.getResolver());

                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.RESOLVED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.CLOSED.name(), history.getNewValue());
                assertEquals("Release completed", history.getMessage());
                assertEquals(closedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("close needs resolved status, PL, and comment")
        void rejectInvalidClose() {
                var fixedIssue = assignedIssue();
                fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalStateException.class,
                                () -> fixedIssue.close(pl, "Release completed", createdAt.plusMinutes(30)));
                assertThrows(IllegalArgumentException.class,
                                () -> resolvedIssue().close(reporter, "Release completed", createdAt.plusMinutes(40)));
                assertThrows(IllegalArgumentException.class,
                                () -> resolvedIssue().close(pl, "", createdAt.plusMinutes(40)));
        }

        @Test
        @DisplayName("closed issue can be reopened")
        void reopenClosedIssue() {
                var issue = resolvedIssue();
                issue.close(pl, "Release completed", createdAt.plusMinutes(40));

                issue.reopen(pl, "Found regression", createdAt.plusMinutes(50));

                assertEquals(IssueStatus.REOPENED, issue.getStatus());
        }

        @Test
        @DisplayName("new issue is not reopened")
        void rejectReopenFromNewIssue() {
                var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

                assertThrows(IllegalStateException.class,
                                () -> issue.reopen(pl, "Needs more work", createdAt));
        }

        private Issue assignedIssue() {
                return assignedIssue("ISSUE-1");
        }

        private Issue assignedIssue(String issueId) {
                var issue = IssueFixtures.create(issueId, "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                return issue;
        }

        private Issue resolvedIssue() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
                return issue;
        }

        private Issue reopenedIssue() {
                var issue = resolvedIssue();
                issue.reopen(pl, "Needs more work", createdAt.plusMinutes(35));
                return issue;
        }

        private static IssueHistory findHistory(Issue issue, ActionType action) {
                return issue.getHistories().stream()
                                .filter(history -> history.getAction() == action)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("History not found for action " + action));
        }
}
