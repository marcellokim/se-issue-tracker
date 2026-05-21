package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Collections;
import java.util.EnumMap;
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
            statusCounts = orderedEnumMap(statusCounts, IssueStatus.class, "statusCounts");
            priorityCounts = orderedEnumMap(priorityCounts, Priority.class, "priorityCounts");
        }

        private static <E extends Enum<E>> Map<E, Integer> orderedEnumMap(
                Map<E, Integer> source,
                Class<E> enumType,
                String fieldName
        ) {
            // CLI smoke 출력은 diff 비교 대상이므로 enum 선언 순서를 보존함.
            EnumMap<E, Integer> ordered = new EnumMap<>(enumType);
            ordered.putAll(Objects.requireNonNull(source, fieldName));
            return Collections.unmodifiableMap(ordered);
        }
    }
}
