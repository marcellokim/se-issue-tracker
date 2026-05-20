package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Project {

    private final long id;
    private final String name;
    private final String description;
    private final String managedById;
    private final LocalDateTime createdDate;
    private final LocalDateTime updatedAt;
    public static Project create(long id, String name, String description, String managedById,
                                  LocalDateTime createdDate, LocalDateTime updatedAt) {
        return new Project(id, name, description, managedById, createdDate, updatedAt);
    }

    private Project(long id, String name, String description, String managedById,
                    LocalDateTime createdDate, LocalDateTime updatedAt) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(managedById, "managedById");
        this.id = id;
        this.name = name;
        this.description = description;
        this.managedById = managedById;
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

    // --- getters ---

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String managedById() {
        return managedById;
    }

    public LocalDateTime createdDate() {
        return createdDate;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

}
