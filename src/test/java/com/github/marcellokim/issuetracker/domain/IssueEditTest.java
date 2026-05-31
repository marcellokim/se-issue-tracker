package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue edit")
class IssueEditTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
    private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, null,
            null);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("priority change leaves history")
    void priorityChangeLeavesHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
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
    @DisplayName("same priority is rejected")
    void rejectSamePriority() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.changePriority(Priority.MAJOR, pl, createdAt));
    }

    @Test
    @DisplayName("reporter can edit a new issue")
    void reporterCanEditNewIssue() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var changedAt = createdAt.plusMinutes(10);

        issue.updateTitleAndDescription("Login fixed", "Updated description", reporter, changedAt);

        assertEquals("Login fixed", issue.getTitle());
        assertEquals("Updated description", issue.getDescription());
        var history = issue.getHistories().getLast();
        assertEquals(ActionType.TITLE_DESCRIPTION_UPDATED, history.getAction());
        assertEquals("Login fails\nCannot log in", history.getPreviousValue());
        assertEquals("Login fixed\nUpdated description", history.getNewValue());
        assertEquals(changedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("reopened issue can be edited again")
    void editReopenedIssue() {
        var issue = resolvedIssue();
        issue.reopen(pl, "Needs more work", createdAt.plusMinutes(40));

        issue.updateTitleAndDescription("Revised title", "Revised desc", reporter, createdAt.plusMinutes(50));

        assertEquals("Revised title", issue.getTitle());
        assertEquals("Revised desc", issue.getDescription());
    }

    @Test
    @DisplayName("only reporter can edit the issue")
    void onlyReporterCanEditIssue() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("New", "Desc", pl, createdAt.plusMinutes(10)));
    }

    @Test
    @DisplayName("assigned issue can not be edited")
    void assignedIssueCannotBeEdited() {
        var issue = assignedIssue();

        assertThrows(IllegalStateException.class,
                () -> issue.updateTitleAndDescription("New", "Desc", reporter, createdAt.plusMinutes(20)));
    }

    @Test
    @DisplayName("blank title or description is rejected")
    void rejectBlankTitleOrDescription() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("", "Desc", reporter, createdAt.plusMinutes(10)));
        assertThrows(IllegalArgumentException.class,
                () -> issue.updateTitleAndDescription("Title", "", reporter, createdAt.plusMinutes(10)));
    }

    @Test
    @DisplayName("comment leaves history")
    void commentLeavesHistory() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment("C-1", "I will check it.", assignee, commentedAt);

        assertEquals(1, issue.getComments().size());
        assertSame(comment, issue.getComments().getFirst());

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertEquals("I will check it.", history.getNewValue());
        assertEquals("comment added", history.getMessage());
        assertSame(assignee, history.getChangedBy());
    }

    @Test
    @DisplayName("status comment keeps its message")
    void statusCommentKeepsItsMessage() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment(
                "C-1",
                "Fixed with regression tests.",
                assignee,
                commentedAt,
                CommentPurpose.STATUS_CHANGE);

        assertEquals(CommentPurpose.STATUS_CHANGE, comment.getPurpose());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getNewValue());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getMessage());
    }

    @Test
    @DisplayName("deleted comment leaves history")
    void deletedCommentLeavesHistory() {
        var issue = storedIssue();
        var comment = Comment.fromPersistence(
                11L,
                100L,
                assignee.getLoginId(),
                "Outdated investigation note",
                CommentPurpose.GENERAL,
                createdAt.plusMinutes(5),
                createdAt.plusMinutes(5));
        var deletedAt = createdAt.plusMinutes(30);

        issue.recordCommentDeletion(comment, assignee, deletedAt);

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertEquals("Outdated investigation note", history.getPreviousValue());
        assertNull(history.getNewValue());
        assertEquals("comment deleted", history.getMessage());
        assertSame(assignee, history.getChangedBy());
        assertEquals(deletedAt, history.getChangedDate());
    }

    private Issue assignedIssue() {
        var issue = IssueFixtures.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
        return issue;
    }

    private Issue storedIssue() {
        return Issue.fromPersistence(Issue.persistedState(1L, "Login fails", "Cannot log in", reporter)
                .id(100L)
                .issueId("ISSUE-1")
                .reportedDate(createdAt)
                .updatedAt(createdAt));
    }

    private Issue resolvedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        return issue;
    }
}
