package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class IssueStateService {

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
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public IssueStateResult changeStatus(String issueId, IssueStatus targetStatus, String comment, String currentUserId) {
        var issue = findIssue(issueId);
        var actor = findUser(currentUserId);
        switch (Objects.requireNonNull(targetStatus, "targetStatus must not be null")) {
            case FIXED -> markFixed(issue, actor, comment);
            case RESOLVED -> resolve(issue, actor, comment);
            case CLOSED -> close(issue, actor, comment);
            default -> throw new UnsupportedOperationException("Target status is outside issue #20 scope: " + targetStatus);
        }
        issueRepository.save(issue);
        return toResult(issue);
    }

    private void markFixed(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanMarkFixed(actor, issue);
        var changedAt = now();
        issue.addComment(nextCommentId(issue), comment, actor, changedAt);
        issue.markFixed(actor, comment, changedAt);
    }

    private void resolve(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanResolve(actor, issue);
        var changedAt = now();
        issue.addComment(nextCommentId(issue), comment, actor, changedAt);
        issue.resolve(actor, comment, changedAt);
    }

    private void close(Issue issue, User actor, String comment) {
        permissionPolicy.assertCanClose(actor, issue);
        var changedAt = now();
        issue.addComment(nextCommentId(issue), comment, actor, changedAt);
        issue.close(actor, comment, changedAt);
    }

    private Issue findIssue(String issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String nextCommentId(Issue issue) {
        return issue.getIssueId() + "-C" + (issue.getComments().size() + 1);
    }

    private static IssueStateResult toResult(Issue issue) {
        return new IssueStateResult(
                issue.getIssueId(),
                issue.getStatus(),
                issue.getAssignee(),
                issue.getVerifier(),
                issue.getFixer(),
                issue.getResolver()
        );
    }
}
