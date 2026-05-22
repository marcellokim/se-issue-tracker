package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueHistory {

    private final long id;
    private final long issueId;
    private final String historyId;
    private final String changedById;
    private final ActionType actionType;
    private final String previousValue;
    private final String newValue;
    private final String message;
    private final LocalDateTime changedDate;
    private final User changedBy;

    private IssueHistory(
            long id,
            long issueId,
            String historyId,
            String changedById,
            User changedBy,
            ActionType actionType,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate
    ) {
        this.id = id;
        this.issueId = issueId;
        this.historyId = requireText(historyId, "historyId");
        this.changedById = requireText(changedById, "changedById");
        this.changedBy = changedBy;
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.message = message;
        this.changedDate = Objects.requireNonNull(changedDate, "changedDate must not be null");
    }

    public static IssueHistory fromPersistence(
            long id,
            long issueId,
            String changedById,
            ActionType actionType,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate
    ) {
        return new IssueHistory(requirePositive(id, "id"), requirePositive(issueId, "issueId"),
                Long.toString(id), changedById,
                null, actionType, previousValue, newValue, message, changedDate);
    }

    public static IssueHistory newForPersistence(
            long issueId,
            String changedById,
            ActionType actionType,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate
    ) {
        return new IssueHistory(
                0L,
                requirePositive(issueId, "issueId"),
                "NEW-HISTORY",
                changedById,
                null,
                actionType,
                previousValue,
                newValue,
                message,
                changedDate);
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
        Objects.requireNonNull(changedBy, "changedBy must not be null");
        return new IssueHistory(0L, 0L, historyId, changedBy.getLoginId(),
                changedBy, action, previousValue, newValue, message, changedDate);
    }

    public long id() {
        return id;
    }

    public long issueId() {
        return issueId;
    }

    public String changedById() {
        return changedById;
    }

    public ActionType actionType() {
        return actionType;
    }

    public String previousValue() {
        return previousValue;
    }

    public String newValue() {
        return newValue;
    }

    public String message() {
        return message;
    }

    public LocalDateTime changedDate() {
        return changedDate;
    }

    public String getHistoryId() {
        return historyId;
    }

    public ActionType getAction() {
        return actionType;
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

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
