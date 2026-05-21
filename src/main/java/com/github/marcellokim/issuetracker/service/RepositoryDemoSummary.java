package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RepositoryDemoSummary(
        Optional<AdminAccount> admin,
        Optional<ProjectSummary> project
) {

    public RepositoryDemoSummary {
        admin = Objects.requireNonNull(admin, "admin");
        project = Objects.requireNonNull(project, "project");
    }

    public record AdminAccount(String loginId, Role role, boolean active) {

        public AdminAccount {
            if (loginId == null || loginId.isBlank()) {
                throw new IllegalArgumentException("loginId must not be blank");
            }
            role = Objects.requireNonNull(role, "role");
        }
    }

    public record ProjectSummary(
            String projectName,
            int memberCount,
            int activeDevCount,
            int activeTesterCount,
            int issueCount,
            Map<IssueStatus, Integer> statusCounts,
            Map<Priority, Integer> priorityCounts,
            int devRecommendationCandidateCount,
            int testerRecommendationCandidateCount
    ) {

        public ProjectSummary {
            if (projectName == null || projectName.isBlank()) {
                throw new IllegalArgumentException("projectName must not be blank");
            }
            statusCounts = Map.copyOf(Objects.requireNonNull(statusCounts, "statusCounts"));
            priorityCounts = Map.copyOf(Objects.requireNonNull(priorityCounts, "priorityCounts"));
        }
    }
}
