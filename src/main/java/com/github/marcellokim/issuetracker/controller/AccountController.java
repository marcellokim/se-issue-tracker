package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import java.util.Objects;

public final class AccountController {

    private final AuthenticationService authenticationService;
    private final AccountService accountService;

    public AccountController(
            AuthenticationService authenticationService,
            AccountService accountService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.accountService = Objects.requireNonNull(accountService, "accountService");
    }

    public UserResponse createAccount(String loginId, String name, String password, Role role) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.createAccount(loginId, name, password, role, user));
    }

    public UserResponse updateAccount(String loginId, String name, Role role) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.updateAccount(loginId, name, role, user));
    }

    public UserResponse renameAccount(String loginId, String name) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.renameAccount(loginId, name, user));
    }

    public UserResponse changeAccountRole(String loginId, Role role) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.changeAccountRole(loginId, role, user));
    }

    public UserResponse activateAccount(String loginId) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.activateAccount(loginId, user));
    }

    public UserResponse deactivateAccount(String loginId) {
        User user = requireCurrentUser();
        return UserResponse.from(accountService.deactivateAccount(loginId, user));
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
