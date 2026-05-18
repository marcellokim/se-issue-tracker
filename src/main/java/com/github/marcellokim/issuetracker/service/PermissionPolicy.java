package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.Locale;
import java.util.Objects;

public final class PermissionPolicy {

    private static final String USER_REQUIRED = "user";
    private static final String ISSUE_REQUIRED = "issue";
    private static final String MANAGE_DELETED_ISSUE = "MANAGE_DELETED_ISSUE";
    private static final String VIEW_STATISTICS = "VIEW_STATISTICS";

    public boolean verifyPermission(User user, String operation, Object resource) {
        if (!isActiveUser(user) || operation == null || operation.isBlank()) {
            return false;
        }

        return switch (operation.trim().toUpperCase(Locale.ROOT)) {
            case MANAGE_DELETED_ISSUE -> isPlOrAdmin(user) && isPersistedProjectResource(resource);
            case "ASSIGN_ISSUE", VIEW_STATISTICS -> isPlOrAdmin(user);
            default -> false;
        };
    }

    public void assertCanRegisterIssue(User user, Project project) {
        Objects.requireNonNull(user, USER_REQUIRED);
        Objects.requireNonNull(project, "project");
        throw pendingOtherTeam();
    }

    public void assertCanAssignIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePlOrAdmin(user, "Only PL or ADMIN can assign issue owners.");
    }

    public void assertCanChangeStatus(User user, Issue issue, IssueStatus targetStatus) {
        Objects.requireNonNull(user, USER_REQUIRED);
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        Objects.requireNonNull(targetStatus, "targetStatus");
        throw pendingOtherTeam();
    }

    public void assertCanManageDeletedIssue(User user, Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        if (!verifyPermission(user, MANAGE_DELETED_ISSUE, targetIssue.projectId())) {
            throw new SecurityException("Only PL or ADMIN can manage deleted issues.");
        }
    }

    public void assertCanManageDependency(User user, Issue issue) {
        Objects.requireNonNull(user, USER_REQUIRED);
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        throw pendingOtherTeam();
    }

    public void assertCanChangePriority(User user, Issue issue) {
        Objects.requireNonNull(user, USER_REQUIRED);
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        throw pendingOtherTeam();
    }

    public void assertCanChangePriority(User user, Issue issue, Priority newPriority) {
        Objects.requireNonNull(newPriority, "newPriority");
        assertCanChangePriority(user, issue);
    }

    public void assertCanManageAccount(User user) {
        Objects.requireNonNull(user, USER_REQUIRED);
        throw pendingOtherTeam();
    }

    public void assertCanManageProject(User user) {
        Objects.requireNonNull(user, USER_REQUIRED);
        throw pendingOtherTeam();
    }

    public void assertCanViewStatistics(User user, Object filters) {
        if (!verifyPermission(user, VIEW_STATISTICS, filters)) {
            throw new SecurityException("Only PL or ADMIN can view statistics.");
        }
    }

    private static void requirePlOrAdmin(User user, String message) {
        if (!isActiveUser(user) || !isPlOrAdmin(user)) {
            throw new SecurityException(message);
        }
    }

    private static boolean isPlOrAdmin(User user) {
        return user.role() == Role.PL || user.role() == Role.ADMIN;
    }

    private static boolean isActiveUser(User user) {
        return user != null && user.active();
    }

    private static boolean isPersistedProjectResource(Object resource) {
        return resource instanceof Long projectId && projectId > 0;
    }

    private static UnsupportedOperationException pendingOtherTeam() {
        return new UnsupportedOperationException("Other team member should implement this part.");
    }
}
