package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.util.Objects;

record IssueStatusChangeRequest(IssueStatus targetStatus, String comment) {

    IssueStatusChangeRequest {
        Objects.requireNonNull(targetStatus, "targetStatus");
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
        comment = comment.trim();
    }
}
