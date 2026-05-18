package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 변경")
class IssueChangeTest {

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User pl = new User("U-2", "pl1", "PL One", "hash", Role.PL);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("우선순위를 변경하면 PRIORITY_CHANGED 이력이 기록된다")
    void changePriorityAndRecordHistory() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var changedAt = createdAt.plusMinutes(10);

        issue.changePriority(Priority.CRITICAL, pl, changedAt);

        assertEquals(Priority.CRITICAL, issue.getPriority());
        assertEquals(2, issue.getHistories().size());
        var history = issue.getHistories().get(1);
        assertEquals(ActionType.PRIORITY_CHANGED, history.getAction());
        assertEquals(Priority.MAJOR.name(), history.getPreviousValue());
        assertEquals(Priority.CRITICAL.name(), history.getNewValue());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("resolved 이슈를 reopen하면 assignment가 비워지고 STATUS_CHANGED 이력이 기록된다")
    void reopenResolvedIssueAndRecordHistory() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var assignee = new User("U-3", "dev1", "Dev One", "hash", Role.DEV);
        var verifier = new User("U-4", "tester2", "Tester Two", "hash", Role.TESTER);
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
    @DisplayName("같은 우선순위로는 변경할 수 없고 NEW 이슈는 reopen할 수 없다")
    void rejectNoOpChanges() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.changePriority(Priority.MAJOR, pl, createdAt));
        assertThrows(IllegalStateException.class,
                () -> issue.reopen(pl, "Needs more work", createdAt));
    }
}
