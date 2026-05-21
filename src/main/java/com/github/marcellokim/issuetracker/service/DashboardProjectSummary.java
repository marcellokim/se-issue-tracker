package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.util.Map;
import java.util.Objects;

public record DashboardProjectSummary(
        String projectName,
        int memberCount,
        int projectLeaderCount,
        int developerCount,
        int testerCount,
        int visibleIssueCount,
        int deletedIssueCount,
        Map<IssueStatus, Integer> statusCounts
) {

    public DashboardProjectSummary {
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("projectName must not be blank");
        }
        statusCounts = Map.copyOf(Objects.requireNonNull(statusCounts, "statusCounts"));
    }
}
