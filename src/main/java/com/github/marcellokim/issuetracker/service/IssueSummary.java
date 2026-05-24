package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import java.time.LocalDateTime;

public record IssueSummary(
        long id,
        String issueId,
        IssueStatus status,
        Priority priority,
        String title,
        String reporterId,
        String assigneeId,
        String verifierId,
        LocalDateTime reportedDate,
        LocalDateTime updatedAt
) {
}
