package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Project(
        long id,
        String name,
        String description,
        String managedById,
        LocalDateTime createdDate,
        LocalDateTime updatedAt) {

    public Project {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(managedById, "managedById");
    }
}
