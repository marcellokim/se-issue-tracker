package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;

record IssueCommentActionState(
        String displayCommentId,
        Long numericCommentId,
        String content,
        boolean canUpdate,
        boolean canDelete) {

    IssueCommentActionState {
        if (displayCommentId == null || displayCommentId.isBlank()) {
            throw new IllegalArgumentException("commentId must not be blank");
        }
        displayCommentId = displayCommentId.trim();
        Objects.requireNonNull(content, "content");
        if (numericCommentId != null && numericCommentId <= 0L) {
            throw new IllegalArgumentException("numericCommentId must be positive");
        }
        if (numericCommentId == null) {
            canUpdate = false;
            canDelete = false;
        }
    }
}
