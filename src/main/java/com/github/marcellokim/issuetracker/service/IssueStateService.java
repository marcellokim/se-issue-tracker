package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueStateService {

    private final IssueRepository issueRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public IssueStateService(
            IssueRepository issueRepository,
            IssueDependencyRepository dependencyRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.dependencyRepository = Objects.requireNonNull(dependencyRepository, "dependencyRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public IssueStateResult changeStatus(long issueId, IssueStatus targetStatus, String comment, String currentUserId) {
        IssueStatus requiredTargetStatus = Objects.requireNonNull(targetStatus, "targetStatus");
        String requiredComment = requireComment(comment);
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
        switch (requiredTargetStatus) {
            case FIXED -> markFixed(issue, actor, requiredComment);
            case ASSIGNED -> rejectFix(issue, actor, requiredComment);
            case RESOLVED -> resolve(issue, actor, requiredComment);
            case CLOSED -> close(issue, actor, requiredComment);
            case REOPENED -> reopen(issue, actor, requiredComment);
            default -> throw new UnsupportedOperationException("Unsupported target status: " + requiredTargetStatus);
        }
        issueRepository.save(issue);
        return toResult(issue);
    }

    private void markFixed(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.FIXED);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can change issue status.");
        LocalDateTime changedAt = now();
        issue.markFixed(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }

    private void rejectFix(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.ASSIGNED);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can change issue status.");
        LocalDateTime changedAt = now();
        issue.rejectFix(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }

    private void resolve(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.RESOLVED);
        requireActiveProjectMember(actor, issue.projectId(), "Only project members can change issue status.");
        rejectUnresolvedBlockingIssues(issue);
        LocalDateTime changedAt = now();
        issue.resolve(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }

    private void close(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.CLOSED);
        requireProjectLead(actor, issue.projectId(), "Only the project PL can close or reopen issues.");
        LocalDateTime changedAt = now();
        issue.close(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }

    private void reopen(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.REOPENED);
        requireProjectLead(actor, issue.projectId(), "Only the project PL can close or reopen issues.");
        LocalDateTime changedAt = now();
        issue.reopen(actor, comment, changedAt);
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE);
    }

    private void rejectUnresolvedBlockingIssues(Issue issue) {
        for (IssueDependency dep : dependencyRepository.findByBlockedIssueId(issue.id())) {
            Issue blockingIssue = findIssue(dep.blockingIssueId());
            IssueStatus status = blockingIssue.getStatus();
            if (status != IssueStatus.RESOLVED && status != IssueStatus.CLOSED) {
                throw new IllegalStateException(
                        "Cannot resolve: blocking issue " + blockingIssue.getIssueId()
                                + " is still " + status);
            }
        }
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireProjectLead(User actor, long projectId, String message) {
        boolean projectLead = userRepository.findActiveByRole(projectId, Role.PL).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        if (!projectLead) {
            throw new SecurityException(message);
        }
    }

    private void requireActiveProjectMember(User actor, long projectId, String message) {
        boolean projectMember = userRepository.findActiveByRole(projectId, actor.getRole()).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        if (!projectMember) {
            throw new SecurityException(message);
        }
    }

    private LocalDateTime now() {
        return clock.now();
    }

    private static String requireComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
        return comment;
    }

    private static IssueStateResult toResult(Issue issue) {
        return new IssueStateResult(
                issue.id(),
                issue.getIssueId(),
                issue.status(),
                toUserResult(issue.getAssignee()),
                toUserResult(issue.getVerifier()),
                toUserResult(issue.getFixer()),
                toUserResult(issue.getResolver())
        );
    }

    private static UserResult toUserResult(User user) {
        return user == null ? null : UserResult.from(user);
    }
}
