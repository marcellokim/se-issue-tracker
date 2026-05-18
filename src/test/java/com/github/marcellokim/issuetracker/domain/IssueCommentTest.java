package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 댓글")
class IssueCommentTest {

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User developer = new User("U-2", "dev1", "Dev One", "hash", Role.DEV);
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
}
