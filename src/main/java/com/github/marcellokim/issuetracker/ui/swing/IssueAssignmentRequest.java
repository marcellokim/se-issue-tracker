package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;

record IssueAssignmentRequest(IssueAssignmentMode mode, String assigneeId, String verifierId) {

    IssueAssignmentRequest(IssueAssignmentMode mode, String assigneeId, String verifierId) {
        this.mode = Objects.requireNonNull(mode, "mode");
        String normalizedAssigneeId = normalize(assigneeId);
        String normalizedVerifierId = normalize(verifierId);
        this.assigneeId = requiredAssigneeId(this.mode, normalizedAssigneeId);
        this.verifierId = requiredVerifierId(this.mode, normalizedVerifierId);
    }

    private static String requiredAssigneeId(IssueAssignmentMode mode, String value) {
        return switch (mode) {
            case ASSIGN, REASSIGN_DEV -> requireText(value, "assigneeId");
            case CHANGE_TESTER -> value;
        };
    }

    private static String requiredVerifierId(IssueAssignmentMode mode, String value) {
        return switch (mode) {
            case ASSIGN, CHANGE_TESTER -> requireText(value, "verifierId");
            case REASSIGN_DEV -> value;
        };
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
