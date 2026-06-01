package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue comment request")
class IssueCommentRequestTest {

    @Test
    @DisplayName("validates add and update comment content")
    void validatesContentRequests() {
        IssueCommentRequest add = IssueCommentRequest.add(" comment body ");
        IssueCommentRequest update = IssueCommentRequest.update(100L, " edited body ");

        assertEquals(IssueCommentMode.ADD, add.mode());
        assertNull(add.commentId());
        assertEquals("comment body", add.content());
        assertEquals(IssueCommentMode.UPDATE, update.mode());
        assertEquals(100L, update.commentId());
        assertEquals("edited body", update.content());

        assertThrows(IllegalArgumentException.class, () -> IssueCommentRequest.add(" "));
        assertThrows(IllegalArgumentException.class, () -> IssueCommentRequest.update(100L, ""));
    }

    @Test
    @DisplayName("validates delete comment id")
    void validatesDeleteRequest() {
        IssueCommentRequest request = IssueCommentRequest.delete(100L);

        assertEquals(IssueCommentMode.DELETE, request.mode());
        assertEquals(100L, request.commentId());
        assertNull(request.content());

        assertThrows(IllegalArgumentException.class, () -> IssueCommentRequest.delete(0L));
    }
}
