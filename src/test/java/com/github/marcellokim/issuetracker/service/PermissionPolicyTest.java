package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Permission policy")
class PermissionPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 12, 0);

    private final PermissionPolicy policy = new PermissionPolicy();
    private final User admin = user("admin", Role.ADMIN);
    private final User pl = user("pl1", Role.PL);
    private final User dev = user("dev1", Role.DEV);
    private final User tester = user("tester1", Role.TESTER);
    private final Project project = new Project(1L, "project1", "Demo project", "admin", NOW, NOW);

    @Test
    @DisplayName("allows ADMIN account and project management only")
    void adminCanManageAccountsAndProjectsOnly() {
        assertDoesNotThrow(() -> policy.assertCanManageAccount(admin));
        assertDoesNotThrow(() -> policy.assertCanManageProject(admin));
        assertTrue(policy.verifyPermission(admin, "MANAGE_PROJECT", null));

        assertThrows(SecurityException.class, () -> policy.assertCanRegisterIssue(admin, project));
        assertThrows(SecurityException.class, () -> policy.assertCanAssignIssue(admin, issue(IssueStatus.NEW)));
        assertThrows(SecurityException.class, () -> policy.assertCanManageDeletedIssue(admin, issue(IssueStatus.DELETED)));
        assertThrows(SecurityException.class, () -> policy.assertCanManageDependency(admin, issue(IssueStatus.ASSIGNED)));
        assertThrows(SecurityException.class, () -> policy.assertCanChangePriority(admin, issue(IssueStatus.ASSIGNED)));
        assertThrows(SecurityException.class, () -> policy.assertCanViewStatistics(admin, project.id()));
    }

    @Test
    @DisplayName("does not grant issue workflow permissions to ADMIN through generic permission checks")
    void adminDoesNotReceiveIssueWorkflowPermissions() {
        assertFalse(policy.verifyPermission(admin, "ASSIGN_ISSUE", project.id()));
        assertFalse(policy.verifyPermission(admin, "MANAGE_DELETED_ISSUE", project.id()));
        assertFalse(policy.verifyPermission(admin, "VIEW_STATISTICS", project.id()));
    }

    @Test
    @DisplayName("allows PL issue management permissions")
    void plCanManageIssueWorkflow() {
        assertDoesNotThrow(() -> policy.assertCanAssignIssue(pl, issue(IssueStatus.NEW)));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.RESOLVED), IssueStatus.CLOSED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.CLOSED), IssueStatus.REOPENED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.ASSIGNED), IssueStatus.DELETED));
        assertDoesNotThrow(() -> policy.assertCanManageDeletedIssue(pl, issue(IssueStatus.DELETED)));
        assertDoesNotThrow(() -> policy.assertCanManageDependency(pl, issue(IssueStatus.ASSIGNED)));
        assertDoesNotThrow(() -> policy.assertCanChangePriority(pl, issue(IssueStatus.ASSIGNED)));
        assertDoesNotThrow(() -> policy.assertCanViewStatistics(pl, project.id()));

        assertTrue(policy.verifyPermission(pl, "ASSIGN_ISSUE", project.id()));
        assertTrue(policy.verifyPermission(pl, "MANAGE_DELETED_ISSUE", project.id()));
        assertTrue(policy.verifyPermission(pl, "VIEW_STATISTICS", project.id()));
    }

    @Test
    @DisplayName("allows PL, DEV, and TESTER to register issues")
    void authenticatedIssueActorsCanRegisterIssues() {
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(pl, project));
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(dev, project));
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(tester, project));
    }

    private Issue issue(IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(project.id(), "Issue", "Issue description", tester)
                .id(1L)
                .issueId("ISSUE-1")
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .assignee(dev)
                .verifier(tester)
                .updatedAt(NOW));
    }

    private static User user(String loginId, Role role) {
        return new User(loginId, loginId, loginId, "hash", role);
    }
}
