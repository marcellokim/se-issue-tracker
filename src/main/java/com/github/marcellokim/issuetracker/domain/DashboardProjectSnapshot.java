package com.github.marcellokim.issuetracker.domain;

import java.util.Map;
import java.util.Objects;

public record DashboardProjectSnapshot(
        long projectId,
        String projectName,
        String projectDescription,
        int memberCount,
        int projectLeaderCount,
        int developerCount,
        int testerCount,
        int visibleIssueCount,
        int deletedIssueCount,
        Map<IssueStatus, Integer> statusCounts
) {

    public DashboardProjectSnapshot {
        if (projectId <= 0L) {
            throw new IllegalArgumentException("projectId must be positive");
        }
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("projectName must not be blank");
        }
        projectDescription = projectDescription == null ? "" : projectDescription;
        statusCounts = Map.copyOf(Objects.requireNonNull(statusCounts, "statusCounts"));
    }
}
