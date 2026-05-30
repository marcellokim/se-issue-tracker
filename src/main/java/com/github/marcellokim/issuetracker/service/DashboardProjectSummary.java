package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.util.Map;
import java.util.Objects;

public record DashboardProjectSummary(
        long projectId,
        String projectName,
        String projectDescription,
        int memberCount,
        int projectLeaderCount,
        int developerCount,
        int testerCount,
        int visibleIssueCount,
        Map<IssueStatus, Integer> statusCounts) {

    public DashboardProjectSummary {
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
