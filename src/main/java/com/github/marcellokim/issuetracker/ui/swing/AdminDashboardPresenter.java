package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import java.util.Objects;

final class AdminDashboardPresenter {

    private final DashboardController dashboardController;
    private final AdminDashboardView view;

    AdminDashboardPresenter(DashboardController dashboardController, AdminDashboardView view) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void load() {
        try {
            view.showDashboard(dashboardController.viewProjects(), dashboardController.viewUsers());
        } catch (RuntimeException exception) {
            view.showError(messageOf(exception));
        }
    }

    private static String messageOf(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Dashboard data could not be loaded.";
        }
        return message;
    }
}
