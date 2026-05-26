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

        public IssueWorkflowActions viewAvailableActions(long issueId, String currentUserId) {
                Issue issue = findIssue(issueId);
                User actor = findUser(currentUserId);
                boolean projectMember = isActiveProjectMember(actor, issue.projectId());
                boolean projectLead = canProjectLead(actor, issue.projectId());
                boolean canManageAssignment = projectLead && permissionPolicy.canAssignIssue(actor, issue);
                boolean canManageDependency = projectLead && permissionPolicy.canManageDependency(actor, issue);
                boolean canCloseOrReopen = projectLead;
                boolean canManageDeleted = projectLead && permissionPolicy.canManageDeletedIssue(actor, issue);

                return new IssueWorkflowActions(
                                projectMember && permissionPolicy.canUpdateIssue(actor, issue),
                                projectLead && permissionPolicy.canChangePriority(actor, issue),
                                canManageAssignment && isOneOf(issue, IssueStatus.NEW, IssueStatus.REOPENED,
                                                IssueStatus.ASSIGNED, IssueStatus.FIXED),
                                canManageAssignment && isOneOf(issue, IssueStatus.NEW, IssueStatus.REOPENED),
                                canManageAssignment && issue.status() == IssueStatus.ASSIGNED,
                                canManageAssignment && issue.status() == IssueStatus.FIXED,
                                issue.status() == IssueStatus.ASSIGNED
                                                && projectMember
                                                && permissionPolicy.canChangeStatus(actor, issue, IssueStatus.FIXED),
                                issue.status() == IssueStatus.FIXED
                                                && projectMember
                                                && permissionPolicy.canChangeStatus(actor, issue, IssueStatus.ASSIGNED),
                                issue.status() == IssueStatus.FIXED
                                                && projectMember
                                                && permissionPolicy.canChangeStatus(actor, issue, IssueStatus.RESOLVED)
                                                && canResolveBlockingDependencies(issue),
                                canCloseOrReopen && issue.status() == IssueStatus.RESOLVED
                                                && permissionPolicy.canChangeStatus(actor, issue, IssueStatus.CLOSED),
                                canCloseOrReopen && isOneOf(issue, IssueStatus.RESOLVED, IssueStatus.CLOSED)
                                                && permissionPolicy.canChangeStatus(actor, issue, IssueStatus.REOPENED),
                                canManageDependency,
                                canManageDependency,
                                projectMember && permissionPolicy.canAddComment(actor, issue),
                                canManageDeleted && permissionPolicy.canChangeStatus(actor, issue,
                                                IssueStatus.DELETED));
        }

        public boolean canUpdateComment(long issueId, long commentId, String currentUserId) {
                Issue issue = findIssue(issueId);
                Comment comment = findComment(commentId);
                User actor = findUser(currentUserId);
                return comment.issueId() == issue.id()
                                && isActiveProjectMember(actor, issue.projectId())
                                && permissionPolicy.canUpdateComment(actor, comment);
        }

        public boolean canDeleteComment(long issueId, long commentId, String currentUserId) {
                Issue issue = findIssue(issueId);
                Comment comment = findComment(commentId);
                User actor = findUser(currentUserId);
                return comment.issueId() == issue.id()
                                && isActiveProjectMember(actor, issue.projectId())
                                && permissionPolicy.canDeleteComment(actor, comment);
        }

        private Issue findIssue(long issueId) {
                return issueRepository.findById(issueId)
                                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        }

        private Comment findComment(long commentId) {
                return commentRepository.findById(commentId)
                                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        }

        private User findUser(String userId) {
                return userRepository.findByLoginId(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        }

        private boolean canProjectLead(User actor, long projectId) {
                return userRepository.findActiveByRole(projectId, Role.PL).stream()
                                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        }

        private boolean isActiveProjectMember(User actor, long projectId) {
                return userRepository.findActiveByRole(projectId, actor.getRole()).stream()
                                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        }

        private boolean canResolveBlockingDependencies(Issue issue) {
                for (IssueDependency dependency : dependencyRepository.findByBlockedIssueId(issue.id())) {
                        Issue blockingIssue = findIssue(dependency.blockingIssueId());
                        if (blockingIssue.status() != IssueStatus.RESOLVED
                                        && blockingIssue.status() != IssueStatus.CLOSED) {
                                return false;
                        }
                }
                return true;
        }

        private static boolean isOneOf(Issue issue, IssueStatus first, IssueStatus second) {
                return issue.status() == first || issue.status() == second;
        }

        private static boolean isOneOf(
                        Issue issue,
                        IssueStatus first,
                        IssueStatus second,
                        IssueStatus third,
                        IssueStatus fourth) {
                return issue.status() == first
                                || issue.status() == second
                                || issue.status() == third
                                || issue.status() == fourth;
        }
}
