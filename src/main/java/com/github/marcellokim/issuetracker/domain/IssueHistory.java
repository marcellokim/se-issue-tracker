package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record IssueHistory(
        long id,
        long issueId,
        String changedById,
        ActionType actionType,
        String previousValue,
        String newValue,
        String message,
        LocalDateTime changedDate) {

    public IssueHistory {
        Objects.requireNonNull(changedById, "changedById");
        Objects.requireNonNull(actionType, "actionType");
    }
}
