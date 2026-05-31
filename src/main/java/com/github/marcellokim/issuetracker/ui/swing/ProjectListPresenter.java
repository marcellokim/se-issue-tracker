package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import java.util.Objects;

final class ProjectListPresenter {

    private final DashboardController dashboardController;
    private final ProjectListView view;

    ProjectListPresenter(DashboardController dashboardController, ProjectListView view) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadProjects() {
        try {
            view.showProjects(dashboardController.viewProjects());
            view.showMessage(" ", false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }
}
