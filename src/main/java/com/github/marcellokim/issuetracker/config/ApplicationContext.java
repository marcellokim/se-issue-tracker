package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import java.io.IOException;
import java.sql.SQLException;

public record ApplicationContext(
        AuthenticationController authenticationController,
        DashboardController dashboardController,
        ProjectController projectController,
        IssueController issueController,
        AssignmentController assignmentController,
        IssueStateController issueStateController,
        DeletedIssueController deletedIssueController,
        StatisticsController statisticsController
) {

    public static ApplicationContext fromEnvironment() throws IOException, SQLException {
        return new ApplicationBootstrap().startUiContext();
    }

}
