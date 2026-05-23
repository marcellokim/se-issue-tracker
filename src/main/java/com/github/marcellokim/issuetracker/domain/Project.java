package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Project {
    private final long id;
    private String name;
    private String description;
    private final String managedByLoginId;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedAt;

    public static Project create(
            String name,
            String description,
            String managedByLoginId,
            LocalDateTime now) {
        // New projects are transient; the repository assigns the database id on save.
        LocalDateTime timestamp = Objects.requireNonNull(now, "now");
        return new Project(0L, name, description, managedByLoginId, timestamp, timestamp);
    }

    public static Project fromPersistence(
            long id,
            String name,
            String description,
            String managedByLoginId,
            LocalDateTime createdDate,
            LocalDateTime updatedAt) {
        // Persistence reconstruction keeps stored id and timestamps exactly as read.
        requirePositive(id, "id");
        return new Project(id, name, description, managedByLoginId, createdDate, updatedAt);
    }

    private Project(
            long id,
            String name,
            String description,
            String managedByLoginId,
            LocalDateTime createdDate,
            LocalDateTime updatedAt) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(managedByLoginId, "managedByLoginId");
        this.id = id;
        this.name = requireText(name, "name");
        this.description = description;
        this.managedByLoginId = requireText(managedByLoginId, "managedByLoginId");
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    public Issue registerIssue(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime now) {
        return Issue.create(Issue.persistedState(id, title, description, reporter)
                .issueId(issueId)
                .priority(priority == null ? Priority.MAJOR : priority)
                .reportedDate(now)
                .updatedAt(now));
    }

    public void rename(String newName, LocalDateTime updatedAt) {
        this.name = requireText(newName, "name");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public void changeDescription(String newDescription, LocalDateTime updatedAt) {
        this.description = newDescription;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getManagedByLoginId() {
        return managedByLoginId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
