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

    public boolean verifyPermission(User user, String operation, Object resource) {
        if (!isActiveUser(user) || operation == null || operation.isBlank()) {
            return false;
        }

        return switch (operation.trim().toUpperCase(Locale.ROOT)) {
            case "ASSIGN_ISSUE", "MANAGE_DELETED_ISSUE", "VIEW_STATISTICS" -> isPlOrAdmin(user);
            default -> false;
        };
    }

    public void assertCanRegisterIssue(User user, Project project) {
        // 다른 팀원이 구현해야하는 부분: 이슈 등록 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanAssignIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, "issue");
        requirePlOrAdmin(user, "Only PL or ADMIN can assign issue owners.");
    }

    public void assertCanChangeStatus(User user, Issue issue, IssueStatus targetStatus) {
        // 다른 팀원이 구현해야하는 부분: role별 이슈 상태 전이 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanManageDeletedIssue(User user, Issue issue) {
        Objects.requireNonNull(issue, "issue");
        requirePlOrAdmin(user, "Only PL or ADMIN can manage deleted issues.");
    }

    public void assertCanManageDependency(User user, Issue issue) {
        // 다른 팀원이 구현해야하는 부분: dependency 추가/삭제 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanChangePriority(User user, Issue issue) {
        // 다른 팀원이 구현해야하는 부분: priority 변경 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanChangePriority(User user, Issue issue, Priority newPriority) {
        assertCanChangePriority(user, issue);
    }

    public void assertCanManageAccount(User user) {
        // 다른 팀원이 구현해야하는 부분: Admin 계정 관리 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanManageProject(User user) {
        // 다른 팀원이 구현해야하는 부분: Admin/PL 프로젝트 관리 권한 정책.
        throw pendingOtherTeam();
    }

    public void assertCanViewStatistics(User user, Object filters) {
        requirePlOrAdmin(user, "Only PL or ADMIN can view statistics.");
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

    private static UnsupportedOperationException pendingOtherTeam() {
        return new UnsupportedOperationException("다른 팀원이 구현해야하는 부분");
    }
}
