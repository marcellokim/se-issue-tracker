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

    public User createAccount(String loginId, String name, String password, Role role) {
        User user = requireCurrentUser();
        return accountService.createAccount(loginId, name, password, role, user.getLoginId());
    }

    public User updateAccount(String loginId, String name, Role role) {
        User user = requireCurrentUser();
        return accountService.updateAccount(loginId, name, role, user.getLoginId());
    }

    public User renameAccount(String loginId, String name) {
        User user = requireCurrentUser();
        return accountService.renameAccount(loginId, name, user.getLoginId());
    }

    public User changeAccountRole(String loginId, Role role) {
        User user = requireCurrentUser();
        return accountService.changeAccountRole(loginId, role, user.getLoginId());
    }

    public User activateAccount(String loginId) {
        User user = requireCurrentUser();
        return accountService.activateAccount(loginId, user.getLoginId());
    }

    public User deactivateAccount(String loginId) {
        User user = requireCurrentUser();
        return accountService.deactivateAccount(loginId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
