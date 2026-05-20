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
        this.name = name;
        this.description = description;
        this.managedById = managedById;
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    // --- domain methods ---

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

}
