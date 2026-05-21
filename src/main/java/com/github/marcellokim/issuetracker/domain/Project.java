package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Project {

    private final long id;
    private String name;
    private String description;
    private final String managedById;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedAt;

    // factory
    public static Project create(long id, String name, String description, String managedById,
            LocalDateTime createdDate, LocalDateTime updatedAt) {
        return new Project(id, name, description, managedById, createdDate, updatedAt);
    }

    // private 생성자
    private Project(long id, String name, String description, String managedById,
            LocalDateTime createdDate, LocalDateTime updatedAt) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(managedById, "managedById");
        this.id = id;
        this.name = requireText(name, "name");
        this.description = description;
        this.managedById = managedById; // AdminID
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    // --- domain methods ---

    public Issue registerIssue(
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime now
    ) {
        return Issue.create(issueId, title, description, priority, reporter, now);
    }

    // --- setters ---

    public void rename(String newName, LocalDateTime updatedAt) {
        this.name = requireText(newName, "name");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public void changeDescription(String newDescription, LocalDateTime updatedAt) {
        this.description = newDescription;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    // --- getters ---

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getManagedById() {
        return managedById;
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

}
