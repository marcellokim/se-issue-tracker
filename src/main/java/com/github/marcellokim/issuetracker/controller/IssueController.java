package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
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

    public List<IssueSummary> searchIssues(
            Long projectId, IssueStatus status, Priority priority,
            String reporterId, String assigneeId, String verifierId,
            String keyword) {
        User user = requireCurrentUser();
        return issueService.searchIssues(
                projectId, status, priority,
                reporterId, assigneeId, verifierId,
                keyword, user.getLoginId());
    }

    public IssueDetailResult viewIssueDetail(long issueId) {
        User user = requireCurrentUser();
        return issueService.viewIssueDetail(issueId, user.getLoginId());
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

    public void removeDependency(String dependencyId) {
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
