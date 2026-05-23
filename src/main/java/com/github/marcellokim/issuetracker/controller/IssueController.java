package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class IssueController {

    private final AuthenticationService authenticationService;
    private final IssueService issueService;

    public IssueController(
            AuthenticationService authenticationService,
            IssueService issueService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.issueService = Objects.requireNonNull(issueService, "issueService");
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority) {
        User user = requireCurrentUser();
        return issueService.registerIssue(projectId, title, description, priority, user.getLoginId());
    }

    public IssueResult updateIssue(long issueId, String title, String description) {
        User user = requireCurrentUser();
        return issueService.updateIssue(issueId, title, description, user.getLoginId());
    }

    public IssueResult changePriority(long issueId, Priority priority) {
        User user = requireCurrentUser();
        return issueService.changePriority(issueId, priority, user.getLoginId());
    }

    public CommentView addComment(long issueId, String content) {
        User user = requireCurrentUser();
        return CommentView.from(issueService.addComment(issueId, content, user.getLoginId()));
    }

    public List<CommentView> viewComments(long issueId) {
        User user = requireCurrentUser();
        return issueService.viewComments(issueId, user.getLoginId()).stream()
                .map(CommentView::from)
                .toList();
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

    public CommentView updateComment(long issueId, long commentId, String content) {
        User user = requireCurrentUser();
        return CommentView.from(issueService.updateComment(issueId, commentId, content, user.getLoginId()));
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    public record CommentView(
            String commentId,
            String content,
            CommentPurpose purpose,
            String writerLoginId,
            LocalDateTime createdDate,
            LocalDateTime updatedDate) {

        private static CommentView from(CommentResult result) {
            Objects.requireNonNull(result, "result");
            return new CommentView(
                    result.commentId(),
                    result.content(),
                    result.purpose(),
                    result.writerLoginId(),
                    result.createdDate(),
                    result.updatedDate());
        }
    }
}
