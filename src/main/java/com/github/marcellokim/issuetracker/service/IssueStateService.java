package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueStateService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public IssueStateService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
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
            case RESOLVED -> resolve(issue, actor, requiredComment);
            case ASSIGNED -> rejectFix(issue, actor, requiredComment);
            case CLOSED -> close(issue, actor, requiredComment);
            case REOPENED -> reopen(issue, actor, requiredComment);
            default -> throw new UnsupportedOperationException("Unsupported target status: " + requiredTargetStatus);
        }
        issueRepository.save(issue);
        return toResult(issue);
    }

    private void markFixed(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.FIXED);
        LocalDateTime changedAt = now();
        issue.markFixed(actor, comment, changedAt);
        recordStatusChangeReason(issue, actor, comment, changedAt);
    }

    private void resolve(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.RESOLVED);
        LocalDateTime changedAt = now();
        issue.resolve(actor, comment, changedAt);
        recordStatusChangeReason(issue, actor, comment, changedAt);
    }

    private void rejectFix(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.ASSIGNED);
        LocalDateTime changedAt = now();
        issue.rejectFix(actor, comment, changedAt);
        recordStatusChangeReason(issue, actor, comment, changedAt);
    }

    private void close(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.CLOSED);
        LocalDateTime changedAt = now();
        issue.close(actor, comment, changedAt);
        recordStatusChangeReason(issue, actor, comment, changedAt);
    }

    private void reopen(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.REOPENED);
        LocalDateTime changedAt = now();
        issue.reopen(actor, comment, changedAt);
        recordStatusChangeReason(issue, actor, comment, changedAt);
    }

    private void recordStatusChangeReason(Issue issue, User actor, String comment, LocalDateTime changedAt) {
        // 도메인 전이가 성공한 뒤에만 사유 댓글을 남겨 실패 전이의 부수효과를 막는다.
        issue.addComment(
                CommentIdGenerator.nextCommentId(),
                comment,
                actor,
                changedAt,
                CommentPurpose.STATUS_CHANGE_REASON);
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
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
                issue.getIssueId(),
                issue.status(),
                issue.getAssignee(),
                issue.getVerifier(),
                issue.getFixer(),
                issue.getResolver()
        );
    }
}
