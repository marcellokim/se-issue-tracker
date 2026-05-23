package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import java.util.List;
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

    public List<Issue> viewRelatedIssues() {
        return dashboardSummaryService.relatedIssuesFor(requireCurrentUser());
    }

    public List<DashboardProjectSummary> viewProjects() {
        return dashboardSummaryService.projectSummariesFor(requireCurrentUser());
    }

    public List<User> viewUsers() {
        return dashboardSummaryService.usersFor(requireCurrentUser());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
