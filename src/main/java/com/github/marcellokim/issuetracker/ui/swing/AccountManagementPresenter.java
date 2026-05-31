package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Objects;

final class AccountManagementPresenter {

    private final DashboardController dashboardController;
    private final AccountController accountController;
    private final AccountManagementView view;

    AccountManagementPresenter(
            DashboardController dashboardController,
            AccountController accountController,
            AccountManagementView view) {
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadUsers() {
        run(" ", () -> {
        });
    }

    void createAccount(AccountCreateRequest request) {
        Objects.requireNonNull(request, "request");
        run("Account created: " + request.loginId(), () -> accountController.createAccount(
                request.loginId(),
                request.name(),
                request.password(),
                request.role()));
    }

    void renameAccount(String loginId, String name) {
        run("Account renamed: " + loginId, () -> accountController.renameAccount(loginId, name));
    }

    void changeAccountRole(String loginId, Role role) {
        run("Account role changed: " + loginId, () -> accountController.changeAccountRole(loginId, role));
    }

    void activateAccount(String loginId) {
        run("Account activated: " + loginId, () -> accountController.activateAccount(loginId));
    }

    void deactivateAccount(String loginId) {
        run("Account deactivated: " + loginId, () -> accountController.deactivateAccount(loginId));
    }

    private void run(String successMessage, Runnable action) {
        try {
            action.run();
            view.showUsers(dashboardController.viewUsers());
            view.showMessage(successMessage, false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }
}
