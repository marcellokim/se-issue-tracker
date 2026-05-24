package com.github.marcellokim.issuetracker.service;

import java.time.LocalDateTime;

public record DependencyResult(
        long id,
        String dependencyId,
        long blockingIssueId,
        String blockingIssueKey,
        long blockedIssueId,
        String blockedIssueKey,
        LocalDateTime discoveredDate
) {
}
