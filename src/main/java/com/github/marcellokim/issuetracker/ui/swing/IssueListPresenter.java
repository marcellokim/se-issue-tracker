package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import java.util.Objects;

final class IssueListPresenter {

    private final ProjectController projectController;
    private final IssueController issueController;
    private final IssueListView view;

    IssueListPresenter(
            ProjectController projectController,
            IssueController issueController,
            IssueListView view) {
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadProjectAndIssues(long projectId) {
        try {
            view.showProject(projectController.viewProjectNonAdminDetail(projectId));
            view.setRegisterEnabled(issueController.canRegisterIssue(projectId));
            showIssues(issueController.viewRelatedProjectIssues(projectId));
        } catch (RuntimeException exception) {
            view.setRegisterEnabled(false);
            view.showMessage(exception.getMessage(), true);
        }
    }

    void searchIssues(long projectId, IssueSearchRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            showIssues(issueController.searchRelatedProjectIssues(
                    projectId,
                    request.keyword(),
                    request.status(),
                    request.priority()));
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void registerIssue(long projectId, IssueRegisterRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            IssueResult result = issueController.registerIssue(
                    projectId,
                    request.title(),
                    request.description(),
                    request.priority());
            showIssues(issueController.viewRelatedProjectIssues(projectId));
            view.setRegisterEnabled(issueController.canRegisterIssue(projectId));
            view.showMessage("Issue registered: " + result.title(), false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private void showIssues(List<IssueSummary> issues) {
        view.showIssues(issues);
        view.showMessage(issues.size() + " issues", false);
    }
}
