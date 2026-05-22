package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import java.util.List;
import java.util.Objects;

public final class DeletedIssueController {

    private final AuthenticationService authenticationService;
    private final DeletedIssueService deletedIssueService;

    public DeletedIssueController(
            AuthenticationService authenticationService,
            DeletedIssueService deletedIssueService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.deletedIssueService = Objects.requireNonNull(deletedIssueService, "deletedIssueService");
    }

    public List<Issue> viewDeletedIssues(long projectId) {
        User user = requireCurrentUser();
        return deletedIssueService.viewDeletedIssues(projectId, user);
    }

    public Issue deleteIssue(long issueId, String comment) {
        User user = requireCurrentUser();
        return deletedIssueService.deleteIssue(issueId, comment, user);
    }

    public Issue restoreIssue(long issueId, String comment) {
        User user = requireCurrentUser();
        return deletedIssueService.restoreIssue(issueId, comment, user);
    }

    public int purgeOverflow(long projectId) {
        User user = requireCurrentUser();
        return deletedIssueService.purgeOverflow(projectId, user);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
