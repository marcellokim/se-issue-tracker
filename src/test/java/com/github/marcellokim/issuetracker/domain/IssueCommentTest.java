package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 댓글")
class IssueCommentTest {

    // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
    private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
    private final User developer = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("댓글을 추가하면 댓글 목록과 COMMENTED 이력이 함께 기록된다")
    void addCommentAndCommentedHistory() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var commentedAt = createdAt.plusMinutes(5);

        var comment = issue.addComment("C-1", "I will check it.", developer, commentedAt);

        assertEquals("C-1", comment.getCommentId());
        assertEquals("I will check it.", comment.getContent());
        assertSame(developer, comment.getWriter());
        assertEquals(commentedAt, comment.getCreatedDate());
        assertEquals(1, issue.getComments().size());
        assertSame(comment, issue.getComments().getFirst());

        assertEquals(2, issue.getHistories().size());
        var history = issue.getHistories().get(1);
        assertEquals(ActionType.COMMENTED, history.getAction());
        assertEquals("C-1", history.getNewValue());
        assertEquals("I will check it.", history.getMessage());
        assertSame(developer, history.getChangedBy());
        assertEquals(commentedAt, history.getChangedDate());
    }

    @Test
    @DisplayName("댓글 내용과 작성자는 필수 값이다")
    void rejectInvalidCommentArguments() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);

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
        var comment = new Comment(11L, 100L, "dev1", "I fixed this.", createdAt);

        assertEquals(11L, comment.id());
        assertEquals(100L, comment.issueId());
        assertEquals("11", comment.getCommentId());
        assertEquals("dev1", comment.writerId());
        assertEquals("I fixed this.", comment.content());
        assertEquals("I fixed this.", comment.getContent());
        assertEquals(createdAt, comment.createdDate());
        assertEquals(createdAt, comment.getCreatedDate());
        assertNull(comment.getWriter());
    }

    @Test
    @DisplayName("domain-created comment keeps writer fields")
    void domainCommentKeepsWriterFields() {
        var comment = Comment.create("C-2", "Please verify again.", developer, createdAt.plusMinutes(15));

        assertEquals(0L, comment.id());
        assertEquals(0L, comment.issueId());
        assertEquals("C-2", comment.getCommentId());
        assertEquals("dev1", comment.writerId());
        assertEquals("Please verify again.", comment.content());
        assertSame(developer, comment.getWriter());
        assertEquals(createdAt.plusMinutes(15), comment.createdDate());
    }

    @Test
    @DisplayName("rejects invalid persisted comment arguments")
    void rejectInvalidPersistedCommentArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new Comment(1L, 100L, "", "content", createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> new Comment(1L, 100L, "dev1", " ", createdAt));
        assertThrows(NullPointerException.class,
                () -> new Comment(1L, 100L, "dev1", "content", null));
    }
}
