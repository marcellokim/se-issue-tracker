package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import java.util.Objects;

public final class IssueController {

    private final AuthenticationService authenticationService;
    private final IssueService issueService;

    public IssueController(
            AuthenticationService authenticationService,
            IssueService issueService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.issueService = Objects.requireNonNull(issueService, "issueService");
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority) {
        User user = requireCurrentUser();
        return issueService.registerIssue(projectId, title, description, priority, user.getLoginId());
    }

    public CommentResult addComment(long issueId, String content) {
        User user = requireCurrentUser();
        return issueService.addComment(issueId, content, user.getLoginId());
    }

    public DependencyResult addDependency(long blockingIssueId, long blockedIssueId) {
        User user = requireCurrentUser();
        return issueService.addDependency(blockingIssueId, blockedIssueId, user.getLoginId());
    }

    public void removeDependency(long dependencyId) {
        User user = requireCurrentUser();
        issueService.removeDependency(dependencyId, user.getLoginId());
    }

    public void deleteComment(long issueId, long commentId) {
        User user = requireCurrentUser();
        issueService.deleteComment(issueId, commentId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
