package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
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
            case MANAGE_DELETED_ISSUE -> isPl(user) && isPersistedProjectResource(resource);
            case MANAGE_PROJECT -> isAdmin(user);
            case "ASSIGN_ISSUE" -> isPl(user);
            case VIEW_STATISTICS -> isAuthUserRole(user);
            default -> false;
        };
    }

    public void assertCanRegisterIssue(User user, Project project) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can register issues.");
        Project targetProject = Objects.requireNonNull(project, "project");
        if (targetProject.getId() <= 0) {
            throw new SecurityException("Issue registration requires a persisted project.");
        }
    }

    // UI 버튼 관련
    public boolean canRegisterIssue(User user, Project project) {
        return allows(() -> assertCanRegisterIssue(user, project));
    }

    public void assertCanViewIssue(User user) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can view issues.");
    }

    // UI 버튼 관련
    public boolean canViewIssue(User user) {
        return allows(() -> assertCanViewIssue(user));
    }

    public void assertCanAssignIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can assign issue owners.");
    }

    // UI 버튼 관련
    public boolean canAssignIssue(User user, Issue issue) {
        return allows(() -> assertCanAssignIssue(user, issue));
    }

    public void assertCanAddComment(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can add issue comments.");
    }

    // UI 버튼 관련
    public boolean canAddComment(User user, Issue issue) {
        return allows(() -> assertCanAddComment(user, issue));
    }

    public void assertCanUpdateIssue(User user, Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can update issues.");
        if (!Objects.equals(user.getLoginId(), targetIssue.reporterId())) {
            throw new SecurityException("Only the issue reporter can update title and description.");
        }
        if (targetIssue.status() != IssueStatus.NEW && targetIssue.status() != IssueStatus.REOPENED) {
            throw new SecurityException("Only NEW or REOPENED issues can update title and description.");
        }
    }

    // UI 관련
    public boolean canUpdateIssue(User user, Issue issue) {
        return allows(() -> assertCanUpdateIssue(user, issue));
    }

    public void assertCanChangeStatus(User user, Issue issue, IssueStatus targetStatus) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        IssueStatus nextStatus = Objects.requireNonNull(targetStatus, "targetStatus");
        switch (nextStatus) {
            case FIXED -> requireAssignedActor(
                    user,
                    targetIssue.assigneeId(),
                    Role.DEV,
                    "Only the active DEV assignee can mark an issue as fixed.");
            case RESOLVED -> requireAssignedActor(
                    user,
                    targetIssue.verifierId(),
                    Role.TESTER,
                    "Only the active TESTER verifier can resolve an issue.");
            case ASSIGNED -> {
                if (targetIssue.status() == IssueStatus.FIXED) {
                    requireAssignedActor(
                            user,
                            targetIssue.verifierId(),
                            Role.TESTER,
                            "Only the active TESTER verifier can reject a fixed issue.");
                } else {
                    requirePl(user, "Only PL can assign issues.");
                }
            }
            case CLOSED, REOPENED -> requirePl(user, "Only PL can close or reopen issues.");
            case DELETED -> {
                requirePl(user, "Only PL can delete issues.");
                requireDeletableStatus(targetIssue);
            }
            case NEW -> throw new SecurityException("Issue status cannot be changed back to NEW.");
        }
    }

    // UI 버튼 관련
    public boolean canChangeStatus(User user, Issue issue, IssueStatus targetStatus) {
        return allows(() -> assertCanChangeStatus(user, issue, targetStatus));
    }

    public void assertCanManageDeletedIssue(User user, Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        if (!verifyPermission(user, MANAGE_DELETED_ISSUE, targetIssue.projectId())) {
            throw new SecurityException("Only PL can manage deleted issues.");
        }
    }

    // UI 버튼 관련
    public boolean canManageDeletedIssue(User user, Issue issue) {
        return allows(() -> assertCanManageDeletedIssue(user, issue));
    }

    public void assertCanManageDependency(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can manage dependencies.");
    }

    // UI 버튼 관련
    public boolean canManageDependency(User user, Issue issue) {
        return allows(() -> assertCanManageDependency(user, issue));
    }

    public void assertCanChangePriority(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can change issue priority.");
    }

    public void assertCanChangePriority(User user, Issue issue, Priority newPriority) {
        Objects.requireNonNull(newPriority, "newPriority");
        assertCanChangePriority(user, issue);
    }

    public boolean canChangePriority(User user, Issue issue) {
        return allows(() -> assertCanChangePriority(user, issue));
    }

    public boolean canChangePriority(User user, Issue issue, Priority newPriority) {
        return allows(() -> assertCanChangePriority(user, issue, newPriority));
    }

    public void assertCanUpdateComment(User user, Comment comment) {
        Comment targetComment = Objects.requireNonNull(comment, "comment");
        requireCommentWriter(user, targetComment, "Only the comment writer can update the comment.");
        if (targetComment.purpose() != CommentPurpose.GENERAL) {
            throw new SecurityException("Only GENERAL comments can be updated.");
        }
    }

    public boolean canUpdateComment(User user, Comment comment) {
        return allows(() -> assertCanUpdateComment(user, comment));
    }

    public void assertCanDeleteComment(User user, Comment comment) {
        Comment targetComment = Objects.requireNonNull(comment, "comment");
        requireCommentWriter(user, targetComment, "Only the comment writer can delete the comment.");
        if (targetComment.purpose() != CommentPurpose.GENERAL) {
            throw new SecurityException("Only GENERAL comments can be deleted.");
        }
    }

    public boolean canDeleteComment(User user, Comment comment) {
        return allows(() -> assertCanDeleteComment(user, comment));
    }

    public void assertCanManageAccount(User user) {
        requireAdmin(user, "Only ADMIN can manage accounts.");
    }

    public boolean canManageAccount(User user) {
        return allows(() -> assertCanManageAccount(user));
    }

    public void assertCanManageProject(User user) {
        if (!verifyPermission(user, MANAGE_PROJECT, null)) {
            throw new SecurityException("Only ADMIN can manage projects.");
        }
    }

    public boolean canManageProject(User user) {
        return allows(() -> assertCanManageProject(user));
    }

    public boolean canViewAllProjects(User user) {
        return isAdmin(user);
    }

    public boolean canViewAllUsers(User user) {
        return isAdmin(user);
    }

    public boolean canViewAllProjectIssues(User user) {
        return isActiveUser(user) && (user.getRole() == Role.ADMIN || user.getRole() == Role.PL);
    }

    public void assertCanViewStatistics(User user, Object filters) {
        if (!verifyPermission(user, VIEW_STATISTICS, filters)) {
            throw new SecurityException("Only active PL, DEV, or TESTER users can view statistics.");
        }
    }

    public boolean canViewStatistics(User user, Object filters) {
        return allows(() -> assertCanViewStatistics(user, filters));
    }

    private static boolean allows(Runnable check) {
        try {
            check.run();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void requirePl(User user, String message) {
        if (!isPl(user)) {
            throw new SecurityException(message);
        }
    }

    private static void requireAdmin(User user, String message) {
        if (!isAdmin(user)) {
            throw new SecurityException(message);
        }
    }

    private static void requireAuthenticatedUserRole(User user, String message) {
        if (!isActiveUser(user) || !isAuthUserRole(user)) {
            throw new SecurityException(message);
        }
    }

    private static void requireCommentWriter(User user, Comment comment, String message) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can manage comments.");
        if (!Objects.equals(user.getLoginId(), comment.writerId())) {
            throw new SecurityException(message);
        }
    }

    private static void requireActiveUser(User user, String message) {
        Objects.requireNonNull(user, USER_REQUIRED);
        if (!user.isActive()) {
            throw new SecurityException(message);
        }
    }

    private static void requireAssignedActor(User user, String expectedLoginId, Role expectedRole, String message) {
        requireActiveUser(user, message);
        if (user.getRole() != expectedRole || expectedLoginId == null || !expectedLoginId.equals(user.getLoginId())) {
            throw new SecurityException(message);
        }
    }

    private static void requireDeletableStatus(Issue issue) {
        if (issue.status() != IssueStatus.NEW && issue.status() != IssueStatus.CLOSED) {
            throw new SecurityException("Only NEW or CLOSED issues can be deleted.");
        }
    }

    private static boolean isAdmin(User user) {
        return isActiveUser(user) && user.getRole() == Role.ADMIN;
    }

    private static boolean isPl(User user) {
        return isActiveUser(user) && user.getRole() == Role.PL;
    }

    private static boolean isAuthUserRole(User user) {
        return user.getRole() == Role.PL || user.getRole() == Role.DEV || user.getRole() == Role.TESTER;
    }

    private static boolean isActiveUser(User user) {
        return user != null && user.isActive();
    }

    private static boolean isPersistedProjectResource(Object resource) {
        return resource instanceof Long projectId && projectId > 0;
    }

}
