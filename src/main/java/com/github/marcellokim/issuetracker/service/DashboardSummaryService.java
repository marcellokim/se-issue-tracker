package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.List;
import java.util.Objects;

public final class DashboardSummaryService {
        private final DashboardSummaryRepository dashboardSummaryRepository;
        private final UserRepository userRepository;
        private final PermissionPolicy permissionPolicy;

        public DashboardSummaryService(
                        DashboardSummaryRepository dashboardSummaryRepository,
                        UserRepository userRepository,
                        PermissionPolicy permissionPolicy) {
                this.dashboardSummaryRepository = Objects.requireNonNull(dashboardSummaryRepository,
                                "dashboardSummaryRepository");
                this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
                this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        }

        public List<DashboardProjectSummary> projectSummariesFor(User user) {
                Objects.requireNonNull(user, "user");
                if (!user.isActive()) {
                        throw new SecurityException("Only active users can view dashboard projects.");
                }
                List<DashboardProjectSnapshot> snapshots = permissionPolicy.canViewAllProjects(user)
                                ? dashboardSummaryRepository.findAllProjectSummaries()
                                : dashboardSummaryRepository.findProjectSummariesByParticipant(user.getLoginId());
                return snapshots.stream()
                                .map(DashboardSummaryService::toSummary)
                                .toList();
        }

        public List<UserResult> usersFor(User user) {
                Objects.requireNonNull(user, "user");
                if (!permissionPolicy.canViewAllUsers(user)) {
                        return List.of();
                }
                return userRepository.findAll().stream()
                                .map(UserResult::from)
                                .toList();
        }

        private static DashboardProjectSummary toSummary(DashboardProjectSnapshot snapshot) {
                return new DashboardProjectSummary(
                                snapshot.projectId(),
                                snapshot.projectName(),
                                snapshot.projectDescription(),
                                snapshot.memberCount(),
                                snapshot.projectLeaderCount(),
                                snapshot.developerCount(),
                                snapshot.testerCount(),
                                snapshot.visibleIssueCount(),
                                snapshot.statusCounts());
        }
}
