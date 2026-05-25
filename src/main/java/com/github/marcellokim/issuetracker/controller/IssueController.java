package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
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
        return detail.withAvailableActions(availableActionNames(
                issueWorkflowService.viewAvailableActions(issueId, user.getLoginId())));
    }

    public List<IssueSummary> searchIssues(long projectId, String keyword, IssueStatus status,
            Priority priority) {
        return searchIssues(projectId, keyword, status, priority, null, null, null);
    }

    public List<IssueSummary> searchIssues(
            long projectId,
            String keyword,
            IssueStatus status,
            Priority priority,
            String reporterId,
            String assigneeId,
            String verifierId) {
        User user = requireCurrentUser();
        return issueService.searchIssues(
                projectId,
                keyword,
                status,
                priority,
                reporterId,
                assigneeId,
                verifierId,
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

    public void removeDependency(String dependencyId) {
        User user = requireCurrentUser();
        issueService.removeDependency(dependencyId, user.getLoginId());
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

    private static List<String> availableActionNames(IssueWorkflowActions actions) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        if (actions.canUpdateIssue()) {
            names.add("UPDATE_ISSUE");
        }
        if (actions.canChangePriority()) {
            names.add("CHANGE_PRIORITY");
        }
        if (actions.canStartAssignment()) {
            names.add("START_ASSIGNMENT");
        }
        if (actions.canAssign()) {
            names.add("ASSIGN");
        }
        if (actions.canReassign()) {
            names.add("REASSIGN_DEV");
        }
        if (actions.canChangeVerifier()) {
            names.add("CHANGE_TESTER");
        }
        if (actions.canMarkFixed()) {
            names.add("MARK_FIXED");
        }
        if (actions.canRejectFix()) {
            names.add("REJECT_FIX");
        }
        if (actions.canResolve()) {
            names.add("RESOLVE");
        }
        if (actions.canClose()) {
            names.add("CLOSE");
        }
        if (actions.canReopen()) {
            names.add("REOPEN");
        }
        if (actions.canAddDependency()) {
            names.add("ADD_DEPENDENCY");
        }
        if (actions.canRemoveDependency()) {
            names.add("REMOVE_DEPENDENCY");
        }
        if (actions.canAddComment()) {
            names.add("ADD_COMMENT");
        }
        if (actions.canSoftDelete()) {
            names.add("SOFT_DELETE");
        }
        return List.copyOf(names);
    }

}
