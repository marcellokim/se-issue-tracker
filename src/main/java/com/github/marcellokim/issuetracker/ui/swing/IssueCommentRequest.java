package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;

    record IssueCommentRequest(IssueCommentMode mode, Long commentId, String content) {

    IssueCommentRequest {
        Objects.requireNonNull(mode, "mode");
        if ((mode == IssueCommentMode.UPDATE || mode == IssueCommentMode.DELETE)
                && (commentId == null || commentId <= 0L)) {
            throw new IllegalArgumentException("commentId must be positive");
        }
        if (mode == IssueCommentMode.ADD) {
            commentId = null;
        }
        if (mode == IssueCommentMode.ADD || mode == IssueCommentMode.UPDATE) {
            content = requireText(content);
        } else {
            content = null;
        }
    }

    static IssueCommentRequest add(String content) {
        return new IssueCommentRequest(IssueCommentMode.ADD, null, content);
    }

    static IssueCommentRequest update(long commentId, String content) {
        return new IssueCommentRequest(IssueCommentMode.UPDATE, commentId, content);
    }

    static IssueCommentRequest delete(long commentId) {
        return new IssueCommentRequest(IssueCommentMode.DELETE, commentId, null);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return value.trim();
    }
}
