package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import java.util.Objects;

record SwingControllers(
        AuthenticationController authenticationController,
        DashboardController dashboardController,
        AccountController accountController,
        ProjectController projectController,
        IssueController issueController,
        StatisticsController statisticsController) {

    SwingControllers {
        Objects.requireNonNull(authenticationController, "authenticationController");
        Objects.requireNonNull(dashboardController, "dashboardController");
        Objects.requireNonNull(accountController, "accountController");
        Objects.requireNonNull(projectController, "projectController");
        Objects.requireNonNull(issueController, "issueController");
    }

    static SwingControllers withoutStatistics(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController) {
        return new SwingControllers(
                authenticationController,
                dashboardController,
                accountController,
                projectController,
                issueController,
                null);
    }
}
