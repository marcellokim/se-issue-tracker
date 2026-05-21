package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue assignment roles")
class IssueAssignmentRoleTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
    private final User anotherAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
    private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, null, null);
    private final User anotherVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null,
            null);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("NEW issue assignment sets assignee and verifier")
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
    @DisplayName("REOPENED issue assignment moves to ASSIGNED")
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
    @DisplayName("assignee must be DEV and verifier must be TESTER")
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
    @DisplayName("inactive users cannot be assignee or verifier")
    void rejectInactiveAssignmentParticipants() {
        var inactiveAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        var inactiveVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
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
    @DisplayName("assignFromNew requires NEW status")
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
    @DisplayName("같은 assignee로 재배정할 수 없다")
    void rejectSameAssigneeReassignment() {
        var issue = assignedIssue();

        assertThrows(IllegalArgumentException.class,
                () -> issue.reassignAssignee(assignee, pl, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("같은 verifier로 변경할 수 없다")
    void rejectSameVerifierChange() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> issue.changeVerifier(verifier, pl, createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("assignment changes require valid active participants")
    void rejectInvalidReassignmentParticipants() {
        var assignedIssue = assignedIssue();
        var fixedIssue = assignedIssue();
        fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        var inactiveDev = User.fromPersistence("dev3", "Dev Three", "hash", Role.DEV, true, null, null);
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
