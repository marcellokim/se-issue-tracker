package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.List;
import java.util.Objects;

public final class DeletedIssueService {

    private static final int MAX_DELETED_ISSUES_PER_PROJECT = 30;

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    public DeletedIssueService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<IssueSummary> viewDeletedIssues(long projectId, User actor) {
        requireDeletedIssuePermission(actor, projectId);
        return issueRepository.findDeletedByProject(projectId).stream()
                .map(DeletedIssueService::toIssueSummary)
                .toList();
    }

    public IssueSummary deleteIssue(long issueId, String comment, User actor) {
        comment = requireText(comment, "comment");
        Issue issue = findIssue(issueId);
        requireDeletedIssuePermission(actor, issue.projectId());
        permissionPolicy.assertCanChangeStatus(actor, issue, IssueStatus.DELETED);

        Issue deletedIssue = issueRepository.softDelete(issueId, actor.getLoginId(), comment, clock.now());
        issueRepository.purgeDeletedBeyondLimit(deletedIssue.projectId(), MAX_DELETED_ISSUES_PER_PROJECT);
        return toIssueSummary(deletedIssue);
    }

    public IssueSummary restoreIssue(long issueId, String comment, User actor) {
        comment = requireText(comment, "comment");
        Issue issue = findIssue(issueId);
        requireDeletedIssuePermission(actor, issue.projectId());
        return toIssueSummary(issueRepository.restore(issueId, actor.getLoginId(), comment, clock.now()));
    }

    public int purgeOverflow(long projectId, User actor) {
        requireDeletedIssuePermission(actor, projectId);
        return issueRepository.purgeDeletedBeyondLimit(projectId, MAX_DELETED_ISSUES_PER_PROJECT);
    }

    public void purgeDeletedIssue(long issueId, User actor) {
        requireIssueId(issueId);
        Issue issue = findIssue(issueId);
        requireDeletedIssuePermission(actor, issue.projectId());

        if (issue.status() != IssueStatus.DELETED) {
            throw new IllegalArgumentException("Only deleted issues can be purged.");
        }

        int deletedRows = issueRepository.purgeDeletedById(issueId);
        if (deletedRows == 0) {
            throw new IllegalStateException("Deleted issue was not purged: " + issueId);
        }
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue was not found: " + issueId));
    }

    private void requireDeletedIssuePermission(User actor, long projectId) {
        permissionPolicy.assertCanManageDeletedIssue(actor, projectId);
        requireProjectLead(actor, projectId);
    }

    private void requireProjectLead(User actor, long projectId) {
        boolean projectLead = userRepository.findActiveByRole(projectId, Role.PL).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        if (!projectLead) {
            throw new SecurityException("Only the project PL can manage deleted issues.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static void requireIssueId(long issueId) {
        if (issueId <= 0L) {
            throw new IllegalArgumentException("issueId must be positive");
        }
    }

    private static IssueSummary toIssueSummary(Issue issue) {
        return new IssueSummary(
                issue.id(),
                issue.getIssueId(),
                issue.projectId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.reporterId(),
                issue.assigneeId(),
                issue.verifierId(),
                issue.reportedDate(),
                issue.updatedAt());
    }
}
