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
    private static final String MANAGE_PROJECT = "MANAGE_PROJECT";
    private static final String VIEW_STATISTICS = "VIEW_STATISTICS";

    public boolean verifyPermission(User user, String operation, Object resource) {
        if (!isActiveUser(user) || operation == null || operation.isBlank()) {
            return false;
        }

        return switch (operation.trim().toUpperCase(Locale.ROOT)) {
            case MANAGE_DELETED_ISSUE -> isPlOrAdmin(user) && isPersistedProjectResource(resource);
            case MANAGE_PROJECT -> user.role() == Role.ADMIN;
            case "ASSIGN_ISSUE", VIEW_STATISTICS -> isPlOrAdmin(user);
            default -> false;
        };
    }

    public void assertCanRegisterIssue(User user, Project project) {
        requireActiveUser(user, "Only active users can register issues.");
        Project targetProject = Objects.requireNonNull(project, "project");
        if (targetProject.id() <= 0) {
            throw new SecurityException("Issue registration requires a persisted project.");
        }
    }

    public void assertCanAssignIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePlOrAdmin(user, "Only PL or ADMIN can assign issue owners.");
    }

    public void assertCanChangeStatus(User user, Issue issue, IssueStatus targetStatus) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        IssueStatus nextStatus = Objects.requireNonNull(targetStatus, "targetStatus");
        switch (nextStatus) {
            case FIXED -> requireAssignedActor(
                    user,
                    targetIssue.assigneeId(),
                    Role.DEV,
                    "Only the active DEV assignee can mark an issue as fixed."
            );
            case RESOLVED -> requireAssignedActor(
                    user,
                    targetIssue.verifierId(),
                    Role.TESTER,
                    "Only the active TESTER verifier can resolve an issue."
            );
            case ASSIGNED -> {
                if (targetIssue.status() == IssueStatus.FIXED) {
                    requireAssignedActor(
                            user,
                            targetIssue.verifierId(),
                            Role.TESTER,
                            "Only the active TESTER verifier can reject a fixed issue."
                    );
                } else {
                    requirePlOrAdmin(user, "Only PL or ADMIN can assign issues.");
                }
            }
            case CLOSED, REOPENED, DELETED -> requirePlOrAdmin(
                    user,
                    "Only PL or ADMIN can close, reopen, or delete issues."
            );
            case NEW -> throw new SecurityException("Issue status cannot be changed back to NEW.");
        }
    }

    public void assertCanManageDeletedIssue(User user, Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        if (!verifyPermission(user, MANAGE_DELETED_ISSUE, targetIssue.projectId())) {
            throw new SecurityException("Only PL or ADMIN can manage deleted issues.");
        }
    }

    public void assertCanManageDependency(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePlOrAdmin(user, "Only PL or ADMIN can manage dependencies.");
    }

    public void assertCanChangePriority(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePlOrAdmin(user, "Only PL or ADMIN can change issue priority.");
    }

    public void assertCanChangePriority(User user, Issue issue, Priority newPriority) {
        Objects.requireNonNull(newPriority, "newPriority");
        assertCanChangePriority(user, issue);
    }

    public void assertCanManageAccount(User user) {
        requireAdmin(user, "Only ADMIN can manage accounts.");
    }

    public void assertCanManageProject(User user) {
        if (!verifyPermission(user, MANAGE_PROJECT, null)) {
            throw new SecurityException("Only ADMIN can manage projects.");
        }
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

    private static void requireAdmin(User user, String message) {
        if (!isActiveUser(user) || user.role() != Role.ADMIN) {
            throw new SecurityException(message);
        }
    }

    private static void requireActiveUser(User user, String message) {
        Objects.requireNonNull(user, USER_REQUIRED);
        if (!user.active()) {
            throw new SecurityException(message);
        }
    }

    private static void requireAssignedActor(User user, String expectedLoginId, Role expectedRole, String message) {
        requireActiveUser(user, message);
        if (user.role() != expectedRole || expectedLoginId == null || !expectedLoginId.equals(user.loginId())) {
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

}
