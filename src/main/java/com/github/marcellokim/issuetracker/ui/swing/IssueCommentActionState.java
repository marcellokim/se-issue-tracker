package com.github.marcellokim.issuetracker.ui.swing;

record IssueCommentActionState(String commentId, boolean canUpdate, boolean canDelete) {

    IssueCommentActionState {
        if (commentId == null || commentId.isBlank()) {
            throw new IllegalArgumentException("commentId must not be blank");
        }
        commentId = commentId.trim();
    }
}
