package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.Objects;

public final class PermissionPolicy {

    private static final String USER_REQUIRED = "user";
    private static final String ISSUE_REQUIRED = "issue";

    public void assertCanRegisterIssue(User user, Project project) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can register issues.");
        Project targetProject = Objects.requireNonNull(project, "project");
        if (targetProject.getId() <= 0) {
            throw new SecurityException("Issue registration requires a persisted project.");
        }
    }

    public void assertCanViewIssue(User user) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can view issues.");
    }

    public void assertCanAssignIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can assign issue owners.");
    }

    public void assertCanAddComment(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can add issue comments.");
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

    public void assertCanManageDeletedIssue(User user, Issue issue) {
        Issue targetIssue = Objects.requireNonNull(issue, ISSUE_REQUIRED);
        assertCanManageDeletedIssue(user, targetIssue.projectId());
    }

    public void assertCanManageDeletedIssue(User user, long projectId) {
        requirePl(user, "Only PL can manage deleted issues.");
        if (projectId <= 0L) {
            throw new SecurityException("Deleted issue management requires a persisted project.");
        }
    }

    public void assertCanManageDependency(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can manage dependencies.");
    }

    public void assertCanChangePriority(User user, Issue issue) {
        Objects.requireNonNull(issue, ISSUE_REQUIRED);
        requirePl(user, "Only PL can change issue priority.");
    }

    public void assertCanChangePriority(User user, Issue issue, Priority newPriority) {
        Objects.requireNonNull(newPriority, "newPriority");
        assertCanChangePriority(user, issue);
    }

    public void assertCanUpdateComment(User user, Comment comment) {
        Comment targetComment = Objects.requireNonNull(comment, "comment");
        requireCommentWriter(user, targetComment, "Only the comment writer can update the comment.");
        if (targetComment.purpose() != CommentPurpose.GENERAL) {
            throw new SecurityException("Only GENERAL comments can be updated.");
        }
    }

    public void assertCanDeleteComment(User user, Comment comment) {
        Comment targetComment = Objects.requireNonNull(comment, "comment");
        requireCommentWriter(user, targetComment, "Only the comment writer can delete the comment.");
        if (targetComment.purpose() != CommentPurpose.GENERAL) {
            throw new SecurityException("Only GENERAL comments can be deleted.");
        }
    }

    public void assertCanManageAccount(User user) {
        requireAdmin(user, "Only ADMIN can manage accounts.");
    }

    public void assertCanManageProject(User user) {
        requireAdmin(user, "Only ADMIN can manage projects.");
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

    public void assertCanViewStatistics(User user) {
        requireAuthenticatedUserRole(user, "Only active PL, DEV, or TESTER users can view statistics.");
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
}
