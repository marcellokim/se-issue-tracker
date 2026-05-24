package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueStateResult;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import java.util.Objects;

public final class IssueStateController {

    private final AuthenticationService authenticationService;
    private final IssueStateService issueStateService;

    public IssueStateController(
            AuthenticationService authenticationService,
            IssueStateService issueStateService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.issueStateService = Objects.requireNonNull(issueStateService, "issueStateService");
    }

    public IssueStateResult changeStatus(long issueId, IssueStatus targetStatus, String comment) {
        User user = requireCurrentUser();
        return issueStateService.changeStatus(issueId, targetStatus, comment, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
