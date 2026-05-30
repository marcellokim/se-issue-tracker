package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.AuthenticationResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.Objects;

public final class LoginPresenter {

    private final AuthenticationController authenticationController;
    private final LoginView view;
    private final SwingNavigator navigator;

    public LoginPresenter(
            AuthenticationController authenticationController,
            LoginView view,
            SwingNavigator navigator
    ) {
        this.authenticationController = Objects.requireNonNull(authenticationController, "authenticationController");
        this.view = Objects.requireNonNull(view, "view");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
    }

    public void loginRequested() {
        view.setLoginEnabled(false);
        try {
            AuthenticationResult result = authenticationController.login(view.loginId(), view.password());
            if (!result.success()) {
                view.showMessage(result.message(), true);
                return;
            }

            UserResult user = result.user();
            view.showMessage("", false);
            view.clearPassword();
            if (user.role() == Role.ADMIN) {
                navigator.showAdminDashboard(user);
            } else {
                navigator.showProjectList(user);
            }
        } finally {
            view.setLoginEnabled(true);
        }
    }
}
