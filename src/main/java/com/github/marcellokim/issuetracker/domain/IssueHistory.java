// package com.github.marcellokim.issuetracker.domain;

// import java.time.LocalDateTime;
// import java.util.Objects;

// public final class IssueHistory {

//     private final long id; // IssueHistory 고유 ID
//     private final long issueId;
//     private final String changedByLoginId;
//     private final ActionType actionType;
//     private final String previousValue;
//     private final String newValue;
//     private final String message;
//     private final LocalDateTime changedDate;

//     public static IssueHistory create(
//             long issueId,
//             String changedByLoginId,
//             ActionType actionType,
//             String previousValue,
//             String newValue,
//             String message,
//             LocalDateTime changedDate) {
//         return new IssueHistory(
//                 0L,
//                 issueId,
//                 changedByLoginId,
//                 actionType,
//                 previousValue,
//                 newValue,
//                 message,
//                 changedDate);
//     }

//     public static IssueHistory fromPersistence(
//             long id,
//             long issueId,
//             String changedByLoginId,
//             ActionType actionType,
//             String previousValue,
//             String newValue,
//             String message,
//             LocalDateTime changedDate) {
//         return new IssueHistory(
//                 id,
//                 issueId,
//                 changedByLoginId,
//                 actionType,
//                 previousValue,
//                 newValue,
//                 message,
//                 changedDate);
//     }

//     private IssueHistory(
//             long id,
//             long issueId,
//             String changedByLoginId,
//             ActionType actionType,
//             String previousValue,
//             String newValue,
//             String message,
//             LocalDateTime changedDate) {
//         this.id = id;
//         this.issueId = issueId;
//         this.changedByLoginId = requireText(changedByLoginId, "changedById");
//         this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
//         this.previousValue = previousValue;
//         this.newValue = newValue;
//         this.message = message;
//         this.changedDate = Objects.requireNonNull(changedDate, "changedDate must not be null");
//     }

//     // -- getter --
//     public long getId() {
//         return id;
//     }

//     public long getIssueId() {
//         return issueId;
//     }

//     public String getChangedByLoginId() {
//         return changedByLoginId;
//     }

//     public ActionType getActionType() {
//         return actionType;
//     }

//     public String getPreviousValue() {
//         return previousValue;
//     }

//     public String getNewValue() {
//         return newValue;
//     }

//     public String getMessage() {
//         return message;
//     }

//     public LocalDateTime getChangedDate() {
//         return changedDate;
//     }

//     private static String requireText(String value, String fieldName) {
//         if (value == null || value.isBlank()) {
//             throw new IllegalArgumentException(fieldName + " must not be blank");
//         }
//         return value;
//     }
// }

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

    public static IssueHistory fromPersistence(
            long id,
            long issueId,
            String changedById,
            ActionType actionType,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate) {
        return new IssueHistory(
                id,
                issueId,
                changedById,
                actionType,
                previousValue,
                newValue,
                message,
                changedDate);
    }

    private IssueHistory(
            long id,
            long issueId,
            String changedById,
            ActionType actionType,
            String previousValue,
            String newValue,
            String message,
            LocalDateTime changedDate) {
        this.id = id;
        this.issueId = issueId;
        this.historyId = Long.toString(id);
        this.changedById = requireText(changedById, "changedById");
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.message = message;
        this.changedDate = Objects.requireNonNull(changedDate, "changedDate must not be null");
        this.changedBy = null;
    }

    private IssueHistory(
            String historyId,
            ActionType action,
            String previousValue,
            String newValue,
            String message,
            User changedBy,
            LocalDateTime changedDate) {
        this.id = 0L;
        this.issueId = 0L;
        this.historyId = requireText(historyId, "historyId");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
        this.changedById = changedBy.getLoginId();
        this.actionType = Objects.requireNonNull(action, "action must not be null");
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.message = message;
        this.changedDate = Objects.requireNonNull(changedDate, "changedDate must not be null");
    }

    public static IssueHistory create(
            String historyId,
            ActionType action,
            String previousValue,
            String newValue,
            String message,
            User changedBy,
            LocalDateTime changedDate) {
        return new IssueHistory(historyId, action, previousValue, newValue, message, changedBy, changedDate);
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
}