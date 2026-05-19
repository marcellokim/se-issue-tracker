package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.util.List;
import java.util.Objects;

public final class DeletedIssueController {

    private static final int MAX_DELETED_ISSUES_PER_PROJECT = 30;

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final IssueRepository issueRepository;
    private final Clock clock;

    public DeletedIssueController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            IssueRepository issueRepository,
            Clock clock
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<Issue> viewDeletedIssues(long projectId) {
        User user = requireCurrentUser();
        requireDeletedIssuePermission(user, projectId);
        return issueRepository.findDeletedByProject(projectId);
    }

    public Issue deleteIssue(long issueId, String comment) {
        User user = requireCurrentUser();
        Issue issue = findIssue(issueId);
        permissionPolicy.assertCanManageDeletedIssue(user, issue);

        Issue deletedIssue = issueRepository.softDelete(issueId, user.loginId(), comment, clock.now());
        issueRepository.purgeDeletedBeyondLimit(deletedIssue.projectId(), MAX_DELETED_ISSUES_PER_PROJECT);
        return deletedIssue;
    }

    public Issue restoreIssue(long issueId, String comment) {
        User user = requireCurrentUser();
        Issue issue = findIssue(issueId);
        permissionPolicy.assertCanManageDeletedIssue(user, issue);
        return issueRepository.restore(issueId, user.loginId(), comment, clock.now());
    }

    public int purgeOverflow(long projectId) {
        User user = requireCurrentUser();
        requireDeletedIssuePermission(user, projectId);
        return issueRepository.purgeDeletedBeyondLimit(projectId, MAX_DELETED_ISSUES_PER_PROJECT);
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue was not found: " + issueId));
    }

    private void requireDeletedIssuePermission(User user, long projectId) {
        if (!permissionPolicy.verifyPermission(user, "MANAGE_DELETED_ISSUE", projectId)) {
            throw new SecurityException("Only PL or ADMIN can manage deleted issues.");
        }
    }
}
