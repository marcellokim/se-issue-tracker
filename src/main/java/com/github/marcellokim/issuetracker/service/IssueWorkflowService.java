package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.List;
import java.util.Objects;

public final class IssueWorkflowService {

    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;

    public IssueWorkflowService(
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.commentRepository = Objects.requireNonNull(commentRepository, "commentRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
    }

    public IssueWorkflowActions viewAvailableActions(long issueId, String currentLoginId) {
        WorkflowContext context = workflowContext(issueId, currentLoginId);
        Issue issue = context.issue();
        User actor = context.actor();

        if (issue.status() == IssueStatus.DELETED) {
            return noActions();
        }

        WorkflowAccess access = workflowAccess(actor, issue);
        return new IssueWorkflowActions(
                canUpdateIssue(actor, issue, access),
                canChangePriority(actor, issue, access),
                canStartAssignment(issue, access),
                canAssign(issue, access),
                canReassign(issue, access),
                canChangeVerifier(issue, access),
                canMarkFixed(actor, issue, access),
                canRejectFix(actor, issue, access),
                canResolve(actor, issue, access),
                canClose(actor, issue, access),
                canReopen(actor, issue, access),
                canAddDependency(access),
                canRemoveDependency(access),
                canAddComment(actor, issue, access),
                canSoftDelete(actor, issue, access));
    }

    public boolean canUpdateComment(long issueId, long commentId, String currentLoginId) {
        return canManageComment(issueId, commentId, currentLoginId,
                (actor, comment) -> permissionPolicy.assertCanUpdateComment(actor, comment));
    }

    public boolean canDeleteComment(long issueId, long commentId, String currentLoginId) {
        return canManageComment(issueId, commentId, currentLoginId,
                (actor, comment) -> permissionPolicy.assertCanDeleteComment(actor, comment));
    }

    public List<CommentActionResult> viewCommentActions(long issueId, String currentLoginId) {
        WorkflowContext context = workflowContext(issueId, currentLoginId);
        Issue issue = context.issue();
        User actor = context.actor();
        if (issue.status() == IssueStatus.DELETED || !isActiveProjectMember(actor, issue.projectId())) {
            return List.of();
        }
        return commentRepository.findByIssueId(issue.id()).stream()
                .map(comment -> new CommentActionResult(
                        comment.getCommentId(),
                        allows(() -> permissionPolicy.assertCanUpdateComment(actor, comment)),
                        allows(() -> permissionPolicy.assertCanDeleteComment(actor, comment))))
                .toList();
    }

    private boolean canManageComment(
            long issueId,
            long commentId,
            String currentLoginId,
            CommentPermissionCheck permissionCheck) {
        try {
            long requiredIssueId = requirePositive(issueId, "issueId");
            long requiredCommentId = requirePositive(commentId, "commentId");
            String requiredLoginId = requireText(currentLoginId, "currentLoginId");
            Issue issue = findIssue(requiredIssueId);
            if (issue.status() == IssueStatus.DELETED) {
                return false;
            }
            Comment comment = findComment(requiredCommentId);
            User actor = findUser(requiredLoginId);
            return comment.issueId() == issue.id()
                    && isActiveProjectMember(actor, issue.projectId())
                && allows(() -> permissionCheck.check(actor, comment));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private WorkflowContext workflowContext(long issueId, String currentLoginId) {
        long requiredIssueId = requirePositive(issueId, "issueId");
        String requiredLoginId = requireText(currentLoginId, "currentLoginId");
        return new WorkflowContext(findIssue(requiredIssueId), findUser(requiredLoginId));
    }

    private WorkflowAccess workflowAccess(User actor, Issue issue) {
        boolean projectMember = isActiveProjectMember(actor, issue.projectId());
        boolean projectLead = isProjectLead(actor, issue.projectId());
        return new WorkflowAccess(
                projectMember,
                projectLead,
                projectLead && allows(() -> permissionPolicy.assertCanAssignIssue(actor, issue)),
                projectLead && allows(() -> permissionPolicy.assertCanManageDependency(actor, issue)),
                projectLead && allows(() -> permissionPolicy.assertCanManageDeletedIssue(actor, issue)));
    }

    private boolean canUpdateIssue(User actor, Issue issue, WorkflowAccess access) {
        return access.projectMember()
                && allows(() -> permissionPolicy.assertCanUpdateIssue(actor, issue));
    }

    private boolean canChangePriority(User actor, Issue issue, WorkflowAccess access) {
        return access.projectLead()
                && allows(() -> permissionPolicy.assertCanChangePriority(actor, issue));
    }

    private static boolean canStartAssignment(Issue issue, WorkflowAccess access) {
        return access.canManageAssignment()
                && isOneOf(issue, IssueStatus.NEW, IssueStatus.REOPENED, IssueStatus.ASSIGNED, IssueStatus.FIXED);
    }

    private static boolean canAssign(Issue issue, WorkflowAccess access) {
        return access.canManageAssignment()
                && isOneOf(issue, IssueStatus.NEW, IssueStatus.REOPENED);
    }

    private static boolean canReassign(Issue issue, WorkflowAccess access) {
        return access.canManageAssignment()
                && issue.status() == IssueStatus.ASSIGNED;
    }

    private static boolean canChangeVerifier(Issue issue, WorkflowAccess access) {
        return access.canManageAssignment()
                && issue.status() == IssueStatus.FIXED;
    }

    private boolean canMarkFixed(User actor, Issue issue, WorkflowAccess access) {
        return issue.status() == IssueStatus.ASSIGNED
                && access.projectMember()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.FIXED));
    }

    private boolean canRejectFix(User actor, Issue issue, WorkflowAccess access) {
        return issue.status() == IssueStatus.FIXED
                && access.projectMember()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.ASSIGNED));
    }

    private boolean canResolve(User actor, Issue issue, WorkflowAccess access) {
        return issue.status() == IssueStatus.FIXED
                && access.projectMember()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.RESOLVED))
                && allBlockingIssuesResolvedOrClosed(issue);
    }

    private boolean canClose(User actor, Issue issue, WorkflowAccess access) {
        return issue.status() == IssueStatus.RESOLVED
                && access.projectLead()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.CLOSED));
    }

    private boolean canReopen(User actor, Issue issue, WorkflowAccess access) {
        return isOneOf(issue, IssueStatus.RESOLVED, IssueStatus.CLOSED)
                && access.projectLead()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.REOPENED));
    }

    private static boolean canAddDependency(WorkflowAccess access) {
        return access.canManageDependency();
    }

    private static boolean canRemoveDependency(WorkflowAccess access) {
        return access.canManageDependency();
    }

    private boolean canAddComment(User actor, Issue issue, WorkflowAccess access) {
        return access.projectMember()
                && allows(() -> permissionPolicy.assertCanAddComment(actor, issue));
    }

    private boolean canSoftDelete(User actor, Issue issue, WorkflowAccess access) {
        return access.canManageDeleted()
                && allows(() -> permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.DELETED));
    }

    private boolean allBlockingIssuesResolvedOrClosed(Issue issue) {
        for (IssueDependency dependency : dependencyRepository.findDependenciesBlockingIssue(issue.id())) {
            Issue blockingIssue = findIssue(dependency.blockingIssueId());
            if (blockingIssue.status() != IssueStatus.RESOLVED
                    && blockingIssue.status() != IssueStatus.CLOSED) {
                return false;
            }
        }
        return true;
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private Comment findComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    }

    private User findUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + loginId));
    }

    private boolean isProjectLead(User actor, long projectId) {
        return actor.getRole() == Role.PL
                && userRepository.existsActiveProjectMember(projectId, actor.getLoginId());
    }

    private boolean isActiveProjectMember(User actor, long projectId) {
        return userRepository.existsActiveProjectMember(projectId, actor.getLoginId());
    }

    private static boolean allows(Runnable check) {
        try {
            check.run();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean isOneOf(Issue issue, IssueStatus first, IssueStatus second) {
        return issue.status() == first || issue.status() == second;
    }

    private static boolean isOneOf(Issue issue, IssueStatus first, IssueStatus second, IssueStatus third,
            IssueStatus fourth) {
        return issue.status() == first
                || issue.status() == second
                || issue.status() == third
                || issue.status() == fourth;
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static IssueWorkflowActions noActions() {
        return new IssueWorkflowActions(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    private record WorkflowAccess(
            boolean projectMember,
            boolean projectLead,
            boolean canManageAssignment,
            boolean canManageDependency,
            boolean canManageDeleted) {
    }

    private record WorkflowContext(Issue issue, User actor) {
    }

    @FunctionalInterface
    private interface CommentPermissionCheck {
        void check(User actor, Comment comment);
    }
}
