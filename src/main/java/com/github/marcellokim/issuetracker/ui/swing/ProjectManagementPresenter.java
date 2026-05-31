package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.Objects;

final class ProjectManagementPresenter {

    private final DashboardController dashboardController;
    private final ProjectController projectController;
    private final ProjectManagementView view;

    ProjectManagementPresenter(
            DashboardController dashboardController,
            ProjectController projectController,
            ProjectManagementView view) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadProjects() {
        run(" ", () -> {
        });
    }

    void createProject(ProjectCreateRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            ProjectResult created = projectController.createProject(request.name(), request.description());
            refresh("Project created: " + created.name());
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void renameProject(long projectId, String name) {
        try {
            ProjectResult renamed = projectController.renameProject(projectId, name);
            refresh("Project renamed: " + renamed.name());
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void changeProjectDescription(long projectId, String description) {
        try {
            ProjectResult changed = projectController.changeProjectDescription(projectId, description);
            refresh("Project description changed: " + changed.name());
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void deleteProject(long projectId) {
        run("Project deleted: " + projectId, () -> projectController.deleteProject(projectId));
    }

    private void run(String successMessage, Runnable action) {
        try {
            action.run();
            refresh(successMessage);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private void refresh(String successMessage) {
        view.showProjects(dashboardController.viewProjects());
        view.showMessage(successMessage, false);
    }
}
