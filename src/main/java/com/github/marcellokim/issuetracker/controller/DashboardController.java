package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashboardController {

    private final AuthenticationService authenticationService;
    private final DashboardSummaryService dashboardSummaryService;

    public DashboardController(
            AuthenticationService authenticationService,
            DashboardSummaryService dashboardSummaryService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.dashboardSummaryService = Objects.requireNonNull(dashboardSummaryService, "dashboardSummaryService");
    }

    public List<IssueSummary> viewRelatedIssues() {
        return dashboardSummaryService.relatedIssuesFor(requireCurrentUser());
    }

    public List<DashboardProjectView> viewProjects() {
        return dashboardSummaryService.projectSummariesFor(requireCurrentUser()).stream()
                .map(DashboardProjectView::from)
                .toList();
    }

    public List<User> viewUsers() {
        return dashboardSummaryService.usersFor(requireCurrentUser());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    public record DashboardProjectView(
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

        private static DashboardProjectView from(DashboardProjectSummary summary) {
            Objects.requireNonNull(summary, "summary");
            return new DashboardProjectView(
                    summary.projectId(),
                    summary.projectName(),
                    summary.projectDescription(),
                    summary.memberCount(),
                    summary.projectLeaderCount(),
                    summary.developerCount(),
                    summary.testerCount(),
                    summary.visibleIssueCount(),
                    summary.deletedIssueCount(),
                    summary.statusCounts());
        }
    }
}
