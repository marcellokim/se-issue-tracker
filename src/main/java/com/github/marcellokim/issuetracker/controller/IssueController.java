package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentActionResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class IssueController {

    private final AuthenticationService authenticationService;
    private final IssueService issueService;
    private final IssueWorkflowService issueWorkflowService;

    public IssueController(
            AuthenticationService authenticationService,
            IssueService issueService) {
        this(authenticationService, issueService, null);
    }

    public IssueController(
            AuthenticationService authenticationService,
            IssueService issueService,
            IssueWorkflowService issueWorkflowService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.issueService = Objects.requireNonNull(issueService, "issueService");
        this.issueWorkflowService = issueWorkflowService;
    }

    public IssueResult registerIssue(long projectId, String title, String description, Priority priority) {
        User user = requireCurrentUser();
        return issueService.registerIssue(projectId, title, description, priority, user.getLoginId());
    }

    public boolean canRegisterIssue(long projectId) {
        User user = requireCurrentUser();
        return issueService.canRegisterIssue(projectId, user.getLoginId());
    }

    public IssueDetailResult viewIssueDetail(long issueId) {
        User user = requireCurrentUser();
        IssueDetailResult detail = issueService.viewIssueDetail(issueId, user.getLoginId());
        if (issueWorkflowService == null) {
            return detail;
        }
        IssueWorkflowActions actions = issueWorkflowService.viewAvailableActions(issueId, user.getLoginId());
        return detail.withAvailableActions(actions.availableActionNames());
    }

    public List<IssueSummary> searchIssues(long projectId, String keyword, IssueStatus status,
            Priority priority) {
        return searchIssues(projectId, keyword, status, priority, null, null, null, null, null);
    }

    public List<IssueSummary> searchIssues(
            long projectId,
            String keyword,
            IssueStatus status,
            Priority priority,
            String reporterId,
            String assigneeId,
            String verifierId,
            LocalDateTime reportedFrom,
            LocalDateTime reportedTo) {
        User user = requireCurrentUser();
        return issueService.searchIssues(
                projectId,
                keyword,
                status,
                priority,
                reporterId,
                assigneeId,
                verifierId,
                reportedFrom,
                reportedTo,
                user.getLoginId());
    }

    public List<IssueSummary> viewRelatedProjectIssues(long projectId) {
        User user = requireCurrentUser();
        return issueService.viewRelatedProjectIssues(projectId, user.getLoginId());
    }

    public IssueResult updateIssue(long issueId, String title, String description) {
        User user = requireCurrentUser();
        return issueService.updateIssue(issueId, title, description, user.getLoginId());
    }

    public IssueResult changePriority(long issueId, Priority priority) {
        User user = requireCurrentUser();
        return issueService.changePriority(issueId, priority, user.getLoginId());
    }

    public CommentResult addComment(long issueId, String content) {
        User user = requireCurrentUser();
        return issueService.addComment(issueId, content, user.getLoginId());
    }

    public List<CommentResult> viewComments(long issueId) {
        User user = requireCurrentUser();
        return issueService.viewComments(issueId, user.getLoginId());
    }

    public DependencyResult addDependency(long blockingIssueId, long blockedIssueId) {
        User user = requireCurrentUser();
        return issueService.addDependency(blockingIssueId, blockedIssueId, user.getLoginId());
    }

    public List<DependencyResult> viewProjectDependencies(long projectId) {
        User user = requireCurrentUser();
        return issueService.viewProjectDependencies(projectId, user.getLoginId());
    }

    public void removeDependency(long blockingIssueId, long blockedIssueId) {
        User user = requireCurrentUser();
        issueService.removeDependency(blockingIssueId, blockedIssueId, user.getLoginId());
    }

    public void deleteComment(long issueId, long commentId) {
        User user = requireCurrentUser();
        issueService.deleteComment(issueId, commentId, user.getLoginId());
    }

    public CommentResult updateComment(long issueId, long commentId, String content) {
        User user = requireCurrentUser();
        return issueService.updateComment(issueId, commentId, content, user.getLoginId());
    }

    public IssueWorkflowActions viewAvailableActions(long issueId) {
        User user = requireCurrentUser();
        return requireIssueWorkflowService().viewAvailableActions(issueId, user.getLoginId());
    }

    public List<CommentActionResult> viewCommentActions(long issueId) {
        User user = requireCurrentUser();
        return requireIssueWorkflowService().viewCommentActions(issueId, user.getLoginId());
    }

    public boolean canUpdateComment(long issueId, long commentId) {
        User user = requireCurrentUser();
        return requireIssueWorkflowService().canUpdateComment(issueId, commentId, user.getLoginId());
    }

    public boolean canDeleteComment(long issueId, long commentId) {
        User user = requireCurrentUser();
        return requireIssueWorkflowService().canDeleteComment(issueId, commentId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    private IssueWorkflowService requireIssueWorkflowService() {
        if (issueWorkflowService == null) {
            throw new IllegalStateException("Issue workflow service is not configured.");
        }
        return issueWorkflowService;
    }

}
