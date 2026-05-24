package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
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

    public Issue viewIssue(long issueId) {
        User user = requireCurrentUser();
        return issueService.viewIssue(issueId, user.getLoginId());
    }

    public List<Issue> searchProjectIssues(long projectId, String keyword, IssueStatus status, Priority priority) {
        User user = requireCurrentUser();
        return issueService.searchProjectIssues(projectId, keyword, status, priority, user.getLoginId());
    }

    public List<Issue> viewRelatedProjectIssues(long projectId) {
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

    public IssueWorkflowActionView viewAvailableActions(long issueId) {
        User user = requireCurrentUser();
        return IssueWorkflowActionView.from(
                requireIssueWorkflowService().viewAvailableActions(issueId, user.getLoginId()));
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

    public record IssueWorkflowActionView(
            boolean canUpdateIssue,
            boolean canChangePriority,
            boolean canStartAssignment,
            boolean canAssign,
            boolean canReassign,
            boolean canChangeVerifier,
            boolean canMarkFixed,
            boolean canRejectFix,
            boolean canResolve,
            boolean canClose,
            boolean canReopen,
            boolean canAddDependency,
            boolean canRemoveDependency,
            boolean canAddComment,
            boolean canSoftDelete
    ) {

        private static IssueWorkflowActionView from(IssueWorkflowActions actions) {
            return new IssueWorkflowActionView(
                    actions.canUpdateIssue(),
                    actions.canChangePriority(),
                    actions.canStartAssignment(),
                    actions.canAssign(),
                    actions.canReassign(),
                    actions.canChangeVerifier(),
                    actions.canMarkFixed(),
                    actions.canRejectFix(),
                    actions.canResolve(),
                    actions.canClose(),
                    actions.canReopen(),
                    actions.canAddDependency(),
                    actions.canRemoveDependency(),
                    actions.canAddComment(),
                    actions.canSoftDelete());
        }
    }
}
