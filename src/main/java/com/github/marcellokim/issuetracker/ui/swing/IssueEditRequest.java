package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Priority;
import java.util.Objects;

record IssueEditRequest(
        IssueEditMode mode,
        String title,
        String description,
        Priority priority,
        String comment) {

    IssueEditRequest {
        Objects.requireNonNull(mode, "mode");
        switch (mode) {
            case UPDATE -> {
                title = requireText(title, "title");
                description = requireText(description, "description");
                priority = null;
                comment = null;
            }
            case CHANGE_PRIORITY -> {
                title = null;
                description = null;
                Objects.requireNonNull(priority, "priority");
                comment = null;
            }
            case SOFT_DELETE -> {
                title = null;
                description = null;
                priority = null;
                comment = requireText(comment, "comment");
            }
        }
    }

    static IssueEditRequest update(String title, String description) {
        return new IssueEditRequest(IssueEditMode.UPDATE, title, description, null, null);
    }

    static IssueEditRequest changePriority(Priority priority) {
        return new IssueEditRequest(IssueEditMode.CHANGE_PRIORITY, null, null, priority, null);
    }

    static IssueEditRequest softDelete(String comment) {
        return new IssueEditRequest(IssueEditMode.SOFT_DELETE, null, null, null, comment);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
