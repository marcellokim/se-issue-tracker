package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueDependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDependencyService;
import java.util.List;
import java.util.Objects;

public final class IssueDependencyController {

    private final AuthenticationService authenticationService;
    private final IssueDependencyService issueDependencyService;

    public IssueDependencyController(
            AuthenticationService authenticationService,
            IssueDependencyService issueDependencyService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.issueDependencyService = Objects.requireNonNull(issueDependencyService, "issueDependencyService");
    }

    public IssueDependencyResult addDependency(long blockingIssueId, long blockedIssueId) {
        User user = requireCurrentUser();
        return issueDependencyService.addDependency(blockingIssueId, blockedIssueId, user.getLoginId());
    }

    public List<IssueDependencyResult> listDependencies(long issueId) {
        User user = requireCurrentUser();
        return issueDependencyService.listDependencies(issueId, user.getLoginId());
    }

    public void removeDependency(long dependencyId) {
        User user = requireCurrentUser();
        issueDependencyService.removeDependency(dependencyId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
