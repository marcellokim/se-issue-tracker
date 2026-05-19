package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DemoDashboardPresenter {

    private final ProjectRepository projects;
    private final IssueRepository issues;
    private final StatisticsRepository statistics;
    private final UserRepository users;

    public DemoDashboardPresenter(
            ProjectRepository projects,
            IssueRepository issues,
            StatisticsRepository statistics,
            UserRepository users
    ) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.issues = Objects.requireNonNull(issues, "issues");
        this.statistics = Objects.requireNonNull(statistics, "statistics");
        this.users = Objects.requireNonNull(users, "users");
    }

    public String buildSummary(User user) {
        Objects.requireNonNull(user, "user");
        StringBuilder summary = new StringBuilder();
        summary.append("Logged in as ")
                .append(user.loginId())
                .append(" / ")
                .append(user.role())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        for (var project : projects.findAll()) {
            var statusCounts = statistics.countByStatus(project.id());
            String statusText = statusCounts.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));

            summary.append(project.name()).append(System.lineSeparator())
                    .append("  members=").append(projects.findParticipants(project.id()).size())
                    .append(", PL=").append(users.findActiveByRole(project.id(), Role.PL).size())
                    .append(", DEV=").append(users.findActiveByRole(project.id(), Role.DEV).size())
                    .append(", TESTER=").append(users.findActiveByRole(project.id(), Role.TESTER).size())
                    .append(System.lineSeparator())
                    .append("  visible issues=").append(issues.findByProject(project.id()).size())
                    .append(", deleted bin=").append(issues.findDeletedByProject(project.id()).size())
                    .append(System.lineSeparator())
                    .append("  status=").append(statusText.isBlank() ? IssueStatus.NEW + "=0" : statusText)
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        return summary.toString();
    }
}
