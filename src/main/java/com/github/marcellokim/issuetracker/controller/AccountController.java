package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.UserResult;
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

    public UserResult createAccount(String loginId, String name, String password, Role role) {
        User user = requireCurrentUser();
        return accountService.createAccount(loginId, name, password, role, user);
    }

    public UserResult renameAccount(String loginId, String name) {
        User user = requireCurrentUser();
        return accountService.renameAccount(loginId, name, user);
    }

    public UserResult changeAccountRole(String loginId, Role role) {
        User user = requireCurrentUser();
        return accountService.changeAccountRole(loginId, role, user);
    }

    public UserResult activateAccount(String loginId) {
        User user = requireCurrentUser();
        return accountService.activateAccount(loginId, user);
    }

    public UserResult deactivateAccount(String loginId) {
        User user = requireCurrentUser();
        return accountService.deactivateAccount(loginId, user);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
