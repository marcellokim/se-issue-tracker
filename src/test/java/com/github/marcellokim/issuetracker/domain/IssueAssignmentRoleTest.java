package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("?댁뒋 諛곗젙 ??븷")
class IssueAssignmentRoleTest {

        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User assignee = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);
        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User anotherAssignee = User.create("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User verifier = User.create("tester2", "Tester Two", "hash", Role.TESTER, true, null, null);
        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User anotherVerifier = User.create("tester3", "Tester Three", "hash", Role.TESTER, true, null,
                        null);
        // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
        private final User pl = User.create("pl1", "PL One", "hash", Role.PL, true, null, null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("NEW ?댁뒋瑜?諛곗젙?섎㈃ assignee/verifier媛 ?ㅼ젙?섍퀬 ASSIGNED ?곹깭媛 ?쒕떎")
        void assignFromNewIssue() {
                var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
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
        @DisplayName("REOPENED ?댁뒋???щ같?뺥븯硫?ASSIGNED ?곹깭媛 ?쒕떎")
        void assignReopenedIssue() {
                var issue = reopenedIssue();

                issue.assignReopened(assignee, verifier, pl, createdAt.plusMinutes(40));

                assertSame(assignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        }

        @Test
        @DisplayName("assigned issue changes assignee only")
        void reassignAssigneeOnly() {
                var issue = assignedIssue();
                var reassignedAt = createdAt.plusMinutes(20);

                issue.reassignAssignee(anotherAssignee, pl, reassignedAt);

                assertSame(anotherAssignee, issue.getAssignee());
                assertSame(verifier, issue.getVerifier());
                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());

                var history = issue.getHistories().getLast();
                assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
                assertEquals(assignee.getLoginId(), history.getPreviousValue());
                assertEquals(anotherAssignee.getLoginId(), history.getNewValue());
                assertEquals(reassignedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("fixed issue changes verifier only")
        void changeVerifierOnly() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                var changedAt = createdAt.plusMinutes(30);

                issue.changeVerifier(anotherVerifier, pl, changedAt);

                assertSame(assignee, issue.getAssignee());
                assertSame(anotherVerifier, issue.getVerifier());
                assertSame(assignee, issue.getFixer());
                assertEquals(IssueStatus.FIXED, issue.getStatus());

                var history = issue.getHistories().getLast();
                assertEquals(ActionType.ASSIGNMENT_CHANGED, history.getAction());
                assertEquals(verifier.getLoginId(), history.getPreviousValue());
                assertEquals(anotherVerifier.getLoginId(), history.getNewValue());
                assertEquals(changedAt, history.getChangedDate());
        }

        @Test
        @DisplayName("assignee??DEV, verifier??TESTER?ъ빞 ?쒕떎")
        void rejectInvalidAssignmentRoles() {
                var invalidAssigneeIssue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                var invalidVerifierIssue = Issue.create("ISSUE-2", "Signup fails", "Cannot sign up", null, reporter,
                                createdAt);

                assertThrows(IllegalArgumentException.class,
                                () -> invalidAssigneeIssue.assignFromNew(verifier, verifier, pl, createdAt));
                assertThrows(IllegalArgumentException.class,
                                () -> invalidVerifierIssue.assignFromNew(assignee, assignee, pl, createdAt));
        }

        @Test
        @DisplayName("鍮꾪솢???ъ슜?먮뒗 assignee ?먮뒗 verifier媛 ?????녿떎")
        void rejectInactiveAssignmentParticipants() {
                // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
                var inactiveAssignee = User.create("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
                // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
                var inactiveVerifier = User.create("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
                inactiveAssignee.deactivate();
                inactiveVerifier.deactivate();

                var issueForAssignee = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter,
                                createdAt);
                assertThrows(IllegalArgumentException.class,
                                () -> issueForAssignee.assignFromNew(inactiveAssignee, verifier, pl,
                                                createdAt.plusMinutes(10)));

                var issueForVerifier = Issue.create("ISSUE-2", "Signup fails", "Cannot sign up", null, reporter,
                                createdAt);
                assertThrows(IllegalArgumentException.class,
                                () -> issueForVerifier.assignFromNew(assignee, inactiveVerifier, pl,
                                                createdAt.plusMinutes(10)));
        }

        @Test
        @DisplayName("NEW媛 ?꾨땶 ?댁뒋??assignFromNew濡?諛곗젙?????녿떎")
        void rejectAssignFromNewWhenStatusIsNotNew() {
                var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));

                assertThrows(IllegalStateException.class,
                                () -> issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("assignment changes require matching status branch")
        void rejectAssignmentChangesForInvalidStatus() {
                var newIssue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                var assignedIssue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> newIssue.reassignAssignee(anotherAssignee, pl, createdAt.plusMinutes(10)));
                assertThrows(IllegalStateException.class,
                                () -> assignedIssue.changeVerifier(anotherVerifier, pl, createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("諛곗젙 蹂寃???곷룄 ?щ컮瑜???븷怨??쒖꽦 ?곹깭?ъ빞 ?쒕떎")
        void rejectInvalidReassignmentParticipants() {
                var assignedIssue = assignedIssue();
                var fixedIssue = assignedIssue();
                fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                // userId ?쒓굅: 5-param ??7-param ?듯빀 (DCD ver1 湲곗?)
                var inactiveDev = User.create("dev3", "Dev Three", "hash", Role.DEV, true, null, null);
                inactiveDev.deactivate();

                assertThrows(IllegalArgumentException.class,
                                () -> assignedIssue.reassignAssignee(verifier, pl, createdAt.plusMinutes(20)));
                assertThrows(IllegalArgumentException.class,
                                () -> assignedIssue.reassignAssignee(inactiveDev, pl, createdAt.plusMinutes(20)));
                assertThrows(IllegalArgumentException.class,
                                () -> fixedIssue.changeVerifier(assignee, pl, createdAt.plusMinutes(30)));
        }

        private Issue assignedIssue() {
                var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                return issue;
        }

        private Issue reopenedIssue() {
                var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
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
