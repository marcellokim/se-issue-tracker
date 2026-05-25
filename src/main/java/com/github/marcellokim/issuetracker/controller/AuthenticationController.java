package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.service.AuthenticationResult;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import java.util.Objects;

public final class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
    }

    public AuthenticationResult login(String loginId, String password) {
        return authenticationService.login(loginId, password);
    }

    public void logout() {
        authenticationService.logout();
    }
}
