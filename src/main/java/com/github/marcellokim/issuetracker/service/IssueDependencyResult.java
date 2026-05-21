package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueDependency;
import java.time.LocalDateTime;
import java.util.Objects;

public record IssueDependencyResult(
        long id,
        String dependencyId,
        long blockingIssueId,
        long blockedIssueId,
        LocalDateTime discoveredDate
) {

    public static IssueDependencyResult from(IssueDependency dependency) {
        IssueDependency target = Objects.requireNonNull(dependency, "dependency");
        return new IssueDependencyResult(
                target.id(),
                target.getDependencyId(),
                target.blockingIssueId(),
                target.blockedIssueId(),
                target.discoveredDate());
    }
}
