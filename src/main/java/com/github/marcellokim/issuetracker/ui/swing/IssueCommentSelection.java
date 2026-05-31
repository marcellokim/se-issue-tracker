package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;

record IssueCommentSelection(long commentId, String content) {

    IssueCommentSelection {
        if (commentId <= 0L) {
            throw new IllegalArgumentException("commentId must be positive");
        }
        Objects.requireNonNull(content, "content");
    }
}
