package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DemoDashboardPresenter {

    private final DashboardController dashboardController;

    public DemoDashboardPresenter(DashboardController dashboardController) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
    }

    public String buildSummary(User user) {
        Objects.requireNonNull(user, "user");
        StringBuilder summary = new StringBuilder();
        summary.append("Logged in as ")
                .append(user.getLoginId())
                .append(" / ")
                .append(user.getRole())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        summary.append("Projects").append(System.lineSeparator());
        summary.append("========").append(System.lineSeparator());
        for (var project : dashboardController.viewProjects()) {
            String statusText = project.statusCounts().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));

            summary.append(project.projectName()).append(System.lineSeparator())
                    .append("  members=").append(project.memberCount())
                    .append(", PL=").append(project.projectLeaderCount())
                    .append(", DEV=").append(project.developerCount())
                    .append(", TESTER=").append(project.testerCount())
                    .append(System.lineSeparator())
                    .append("  visible issues=").append(project.visibleIssueCount())
                    .append(", deleted bin=").append(project.deletedIssueCount())
                    .append(System.lineSeparator())
                    .append("  status=").append(statusText.isBlank() ? IssueStatus.NEW + "=0" : statusText)
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        if (user.getRole() == com.github.marcellokim.issuetracker.domain.Role.ADMIN) {
            summary.append("Users").append(System.lineSeparator());
            summary.append("=====").append(System.lineSeparator());
            for (var account : dashboardController.viewUsers()) {
                summary.append(account.getLoginId())
                        .append(" / ")
                        .append(account.getName())
                        .append(" / ")
                        .append(account.getRole())
                        .append(" / ")
                        .append(account.isActive() ? "ACTIVE" : "INACTIVE")
                        .append(System.lineSeparator());
            }
        }

        return summary.toString();
    }

    public List<Issue> relatedIssues() {
        return dashboardController.viewRelatedIssues();
    }

    public List<DashboardProjectView> projectSummaries() {
        return dashboardController.viewProjects();
    }

    public List<User> users() {
        return dashboardController.viewUsers();
    }
}
