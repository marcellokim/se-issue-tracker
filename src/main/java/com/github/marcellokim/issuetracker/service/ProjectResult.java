package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Project;
import java.time.LocalDateTime;
import java.util.Objects;

public record ProjectResult(
        long id,
        String name,
        String description,
        String managedByLoginId,
        LocalDateTime createdDate,
        LocalDateTime updatedAt
) {

    public ProjectResult {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        description = description == null ? "" : description;
        if (managedByLoginId == null || managedByLoginId.isBlank()) {
            throw new IllegalArgumentException("managedByLoginId must not be blank");
        }
    }

    public static ProjectResult from(Project project) {
        Objects.requireNonNull(project, "project");
        return new ProjectResult(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getManagedByLoginId(),
                project.getCreatedDate(),
                project.getUpdatedAt());
    }
}
