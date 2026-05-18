package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;

public record IssueDependency(
        long id,
        long blockingIssueId,
        long blockedIssueId,
        LocalDateTime discoveredDate
) {
}
