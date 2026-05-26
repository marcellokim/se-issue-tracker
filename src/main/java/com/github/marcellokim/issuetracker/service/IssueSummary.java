package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import java.time.LocalDateTime;
import java.util.Objects;

public record IssueSummary(
        long id,
        String issueId,
        long projectId,
        IssueStatus status,
        Priority priority,
        String title,
        String reporterId,
        String assigneeId,
        String verifierId,
        LocalDateTime reportedDate,
        LocalDateTime updatedAt
) {

    public IssueSummary {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (projectId <= 0L) {
            throw new IllegalArgumentException("projectId must be positive");
        }
        if (issueId == null || issueId.isBlank()) {
            throw new IllegalArgumentException("issueId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        status = Objects.requireNonNull(status, "status");
        priority = Objects.requireNonNull(priority, "priority");
        reporterId = Objects.requireNonNull(reporterId, "reporterId");
        reportedDate = Objects.requireNonNull(reportedDate, "reportedDate");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
