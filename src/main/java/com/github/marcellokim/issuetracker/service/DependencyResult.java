package com.github.marcellokim.issuetracker.service;

import java.time.LocalDateTime;

public record DependencyResult(
        long id,
        String dependencyId,
        String blockingIssueId,
        String blockedIssueId,
        LocalDateTime discoveredDate
) {
}
