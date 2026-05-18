package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DemoDashboardService;
import com.github.marcellokim.issuetracker.ui.LoginView;
import java.util.Objects;

public final class LoginController {

    private final LoginView view;
    private final AuthenticationService authenticationService;
    private final DemoDashboardService dashboardService;

    public LoginController(
            LoginView view,
            AuthenticationService authenticationService,
            DemoDashboardService dashboardService) {
        this.view = Objects.requireNonNull(view, "view");
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
    }

    public void bind() {
        view.loginButton().setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        var result = authenticationService.login(view.loginId(), view.password());

        if (result.success()) {
            view.showSuccess(result.message(), dashboardService.buildSummary(result.user()));
        } else {
            view.showFailure(result.message());
        }
    }
}
