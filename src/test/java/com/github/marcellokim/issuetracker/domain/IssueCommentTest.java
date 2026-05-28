package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue comments")
class IssueCommentTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final User developer = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("adding a comment records comment and COMMENTED history")
    void addCommentAndCommentedHistory() {
        var issue = IssueTestFactory.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment("C-1", "I will check it.", developer, commentedAt);

        assertEquals("C-1", comment.getCommentId());
        assertEquals("I will check it.", comment.getContent());
        assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
        assertSame(developer, comment.getWriter());
        assertEquals(commentedAt, comment.getCreatedDate());
        assertEquals(commentedAt, comment.getUpdatedDate());
        assertEquals(1, issue.getComments().size());
        assertSame(comment, issue.getComments().getFirst());

        assertEquals(2, issue.getHistories().size());
        var history = issue.getHistories().get(1);
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals("I will check it.", history.getNewValue());
        assertEquals("comment added", history.getMessage());
        assertSame(developer, history.getChangedBy());
        assertEquals(commentedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("status-change comments keep a distinct purpose")
    void addStatusChangeReasonComment() {
        var issue = IssueTestFactory.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment(
                "C-1",
                "Fixed with regression tests.",
                developer,
                commentedAt,
                CommentPurpose.STATUS_CHANGE
        );

        assertEquals(CommentPurpose.STATUS_CHANGE, comment.getPurpose());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertNull(issue.getHistories().getLast().getPreviousValue());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getNewValue());
        assertEquals("Fixed with regression tests.", issue.getHistories().getLast().getMessage());
    }

    @Test
    @DisplayName("deleting a comment records previous content and null new value")
    void recordCommentDeletionHistory() {
        var issue = IssueTestFactory.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var comment = Comment.fromPersistence(
                11L,
                100L,
                developer.getLoginId(),
                "Outdated investigation note",
                CommentPurpose.GENERAL,
                createdAt.plusMinutes(5),
                createdAt.plusMinutes(5));
        var deletedAt = createdAt.plusMinutes(30);

        issue.recordCommentDeletion(comment, developer, deletedAt);

        var history = issue.getHistories().getLast();
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertEquals("Outdated investigation note", history.getPreviousValue());
        assertNull(history.getNewValue());
        assertEquals("comment deleted", history.getMessage());
        assertSame(developer, history.getChangedBy());
        assertEquals(deletedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("comment content and writer are required")
    void rejectInvalidCommentArguments() {
        var issue = IssueTestFactory.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.addComment("", "content", developer, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> issue.addComment("C-1", "", developer, createdAt));
        assertThrows(NullPointerException.class,
                () -> issue.addComment("C-1", "content", null, createdAt));
        assertThrows(NullPointerException.class,
                () -> issue.addComment("C-1", "content", developer, null));
    }

    @Test
    @DisplayName("persisted comment keeps database fields")
    void persistedCommentKeepsDatabaseFields() {
        var updatedAt = createdAt.plusHours(2);
        var comment = Comment.fromPersistence(
                11L, 100L, "dev1", "I fixed this.", CommentPurpose.GENERAL, createdAt, updatedAt);

        assertEquals(11L, comment.id());
        assertEquals(100L, comment.issueId());
        assertEquals("11", comment.getCommentId());
        assertEquals("dev1", comment.writerId());
        assertEquals("I fixed this.", comment.content());
        assertEquals("I fixed this.", comment.getContent());
        assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
        assertEquals(createdAt, comment.createdDate());
        assertEquals(createdAt, comment.getCreatedDate());
        assertEquals(updatedAt, comment.updatedDate());
        assertEquals(updatedAt, comment.getUpdatedDate());
        assertNull(comment.getWriter());
    }

    @Test
    @DisplayName("comment content change updates content and updatedDate")
    void changeContentUpdatesContentAndUpdatedDate() {
        var comment = Comment.create(
                "C-2", "Please verify again.", developer, CommentPurpose.GENERAL, createdAt);
        var changedAt = createdAt.plusHours(3);

        comment.changeContent("Please verify the regression case.", changedAt);

        assertEquals("Please verify the regression case.", comment.content());
        assertEquals("Please verify the regression case.", comment.getContent());
        assertEquals(createdAt, comment.getCreatedDate());
        assertEquals(changedAt, comment.updatedDate());
        assertEquals(changedAt, comment.getUpdatedDate());
    }

    @Test
    @DisplayName("domain-created comment keeps writer fields")
    void domainCommentKeepsWriterFields() {
        var comment = Comment.create(
                "C-2", "Please verify again.", developer, CommentPurpose.GENERAL, createdAt.plusMinutes(15));

        assertEquals(0L, comment.id());
        assertEquals(0L, comment.issueId());
        assertEquals("C-2", comment.getCommentId());
        assertEquals("dev1", comment.writerId());
        assertEquals("Please verify again.", comment.content());
        assertEquals(CommentPurpose.GENERAL, comment.getPurpose());
        assertSame(developer, comment.getWriter());
        assertEquals(createdAt.plusMinutes(15), comment.createdDate());
        assertEquals(createdAt.plusMinutes(15), comment.updatedDate());
    }

    @Test
    @DisplayName("rejects invalid persisted comment arguments")
    void rejectInvalidPersistedCommentArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Comment.fromPersistence(1L, 100L, "", "content", CommentPurpose.GENERAL,
                        createdAt, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> Comment.fromPersistence(1L, 100L, "dev1", " ", CommentPurpose.GENERAL,
                        createdAt, createdAt));
        assertThrows(NullPointerException.class,
                () -> Comment.fromPersistence(1L, 100L, "dev1", "content", CommentPurpose.GENERAL,
                        null, createdAt));
        assertThrows(NullPointerException.class,
                () -> Comment.fromPersistence(1L, 100L, "dev1", "content", CommentPurpose.GENERAL,
                        createdAt, null));
        var comment = Comment.create("C-1", "content", developer, CommentPurpose.GENERAL, createdAt);
        assertThrows(IllegalArgumentException.class, () -> comment.changeContent(" ", createdAt.plusMinutes(1)));
        assertThrows(NullPointerException.class, () -> comment.changeContent("updated", null));
    }
}
