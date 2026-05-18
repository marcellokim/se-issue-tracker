package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Issue(
        long id,
        long projectId,
        String title,
        String description,
        LocalDateTime reportedDate,
        Priority priority,
        IssueStatus status,
        String reporterId,
        String assigneeId,
        String verifierId,
        String fixerId,
        String resolverId,
        LocalDateTime updatedAt) {

    public Issue {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reporterId, "reporterId");
    }
}
