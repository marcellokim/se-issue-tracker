package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueHistory {

    private final String historyId;
    private final ActionType action;
    private final String previousValue;
    private final String newValue;
    private final LocalDateTime changedDate;
    private final String message;
    private final User changedBy;

    private IssueHistory(
            String historyId,
            ActionType action,
            String previousValue,
            String newValue,
            String message,
            User changedBy,
            LocalDateTime changedDate
    ) {
        this.historyId = requireText(historyId, "historyId");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.message = message;
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
        this.changedDate = Objects.requireNonNull(changedDate, "changedDate must not be null");
    }

    public static IssueHistory create(
            String historyId,
            ActionType action,
            String previousValue,
            String newValue,
            String message,
            User changedBy,
            LocalDateTime changedDate
    ) {
        return new IssueHistory(historyId, action, previousValue, newValue, message, changedBy, changedDate);
    }

    public String getHistoryId() {
        return historyId;
    }

    public ActionType getAction() {
        return action;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getChangedDate() {
        return changedDate;
    }

    public String getMessage() {
        return message;
    }

    public User getChangedBy() {
        return changedBy;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
