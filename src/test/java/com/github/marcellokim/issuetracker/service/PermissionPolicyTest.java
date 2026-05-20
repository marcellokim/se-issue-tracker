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
    private final Project project = Project.create(1L, "project1", "Demo project", "admin", NOW, NOW);

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
    @DisplayName("rejects empty, unknown, inactive, and malformed generic permission checks")
    void genericPermissionChecksRejectInvalidRequests() {
        assertFalse(policy.verifyPermission(null, "MANAGE_PROJECT", null));
        assertFalse(policy.verifyPermission(pl, null, project.id()));
        assertFalse(policy.verifyPermission(pl, " ", project.id()));
        assertFalse(policy.verifyPermission(pl, "UNKNOWN_OPERATION", project.id()));
        assertFalse(policy.verifyPermission(inactive("pl2", Role.PL), "ASSIGN_ISSUE", project.id()));
        assertFalse(policy.verifyPermission(inactive("dev2", Role.DEV), "VIEW_STATISTICS", project.id()));

        assertTrue(policy.verifyPermission(pl, " view_statistics ", project.id()));
        assertTrue(policy.verifyPermission(dev, "VIEW_STATISTICS", project.id()));
        assertTrue(policy.verifyPermission(tester, "VIEW_STATISTICS", project.id()));
        assertFalse(policy.verifyPermission(pl, "MANAGE_DELETED_ISSUE", null));
        assertFalse(policy.verifyPermission(pl, "MANAGE_DELETED_ISSUE", 0L));
        assertTrue(policy.verifyPermission(pl, "MANAGE_DELETED_ISSUE", project.id()));
    }

    @Test
    @DisplayName("allows PL issue management permissions")
    void plCanManageIssueWorkflow() {
        assertDoesNotThrow(() -> policy.assertCanAssignIssue(pl, issue(IssueStatus.NEW)));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.RESOLVED), IssueStatus.CLOSED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.CLOSED), IssueStatus.REOPENED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.NEW), IssueStatus.DELETED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(pl, issue(IssueStatus.CLOSED), IssueStatus.DELETED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(pl, issue(IssueStatus.ASSIGNED), IssueStatus.DELETED));
        assertDoesNotThrow(() -> policy.assertCanManageDeletedIssue(pl, issue(IssueStatus.DELETED)));
        assertDoesNotThrow(() -> policy.assertCanManageDependency(pl, issue(IssueStatus.ASSIGNED)));
        assertDoesNotThrow(() -> policy.assertCanChangePriority(pl, issue(IssueStatus.ASSIGNED)));
        assertDoesNotThrow(() -> policy.assertCanViewStatistics(pl, project.id()));

        assertTrue(policy.verifyPermission(pl, "ASSIGN_ISSUE", project.id()));
        assertTrue(policy.verifyPermission(pl, "MANAGE_DELETED_ISSUE", project.id()));
        assertTrue(policy.verifyPermission(pl, "VIEW_STATISTICS", project.id()));
    }

    @Test
    @DisplayName("allows active auth users except ADMIN to view statistics")
    void activeIssueActorsCanViewStatistics() {
        assertDoesNotThrow(() -> policy.assertCanViewStatistics(pl, project.id()));
        assertDoesNotThrow(() -> policy.assertCanViewStatistics(dev, project.id()));
        assertDoesNotThrow(() -> policy.assertCanViewStatistics(tester, project.id()));

        assertThrows(SecurityException.class, () -> policy.assertCanViewStatistics(admin, project.id()));
        assertThrows(SecurityException.class,
                () -> policy.assertCanViewStatistics(inactive("tester2", Role.TESTER), project.id()));
    }

    @Test
    @DisplayName("enforces assigned actor for fixed and resolved transitions")
    void statusChangesRequireCurrentAssignedActor() {
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(dev, issue(IssueStatus.ASSIGNED), IssueStatus.FIXED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(tester, issue(IssueStatus.ASSIGNED), IssueStatus.FIXED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(inactive("dev1", Role.DEV), issue(IssueStatus.ASSIGNED),
                        IssueStatus.FIXED));

        assertDoesNotThrow(() -> policy.assertCanChangeStatus(tester, issue(IssueStatus.FIXED), IssueStatus.RESOLVED));
        assertDoesNotThrow(() -> policy.assertCanChangeStatus(tester, issue(IssueStatus.FIXED), IssueStatus.ASSIGNED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(dev, issue(IssueStatus.FIXED), IssueStatus.RESOLVED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(user("tester2", Role.TESTER), issue(IssueStatus.FIXED),
                        IssueStatus.RESOLVED));
    }

    @Test
    @DisplayName("rejects disallowed status transitions and missing status arguments")
    void rejectsDisallowedStatusChanges() {
        assertThrows(NullPointerException.class,
                () -> policy.assertCanChangeStatus(pl, issue(IssueStatus.NEW), null));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(pl, issue(IssueStatus.ASSIGNED), IssueStatus.NEW));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(dev, issue(IssueStatus.RESOLVED), IssueStatus.CLOSED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(dev, issue(IssueStatus.CLOSED), IssueStatus.REOPENED));
        assertThrows(SecurityException.class,
                () -> policy.assertCanChangeStatus(pl, issue(IssueStatus.REOPENED), IssueStatus.DELETED));
    }

    @Test
    @DisplayName("allows PL, DEV, and TESTER to register issues")
    void authenticatedIssueActorsCanRegisterIssues() {
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(pl, project));
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(dev, project));
        assertDoesNotThrow(() -> policy.assertCanRegisterIssue(tester, project));
    }

    @Test
    @DisplayName("rejects issue registration without an active auth actor or persisted project")
    void rejectsInvalidIssueRegistrationRequests() {
        Project transientProject = Project.create(0L, "draft", "Draft project", "admin", NOW, NOW);

        assertThrows(SecurityException.class, () -> policy.assertCanRegisterIssue(inactive("dev2", Role.DEV), project));
        assertThrows(SecurityException.class, () -> policy.assertCanRegisterIssue(admin, project));
        assertThrows(SecurityException.class, () -> policy.assertCanRegisterIssue(dev, transientProject));
        assertThrows(NullPointerException.class, () -> policy.assertCanRegisterIssue(dev, null));
    }

    @Test
    @DisplayName("rejects non-PL issue management and non-ADMIN account management")
    void rejectsWrongRoleForManagementOperations() {
        assertThrows(SecurityException.class, () -> policy.assertCanAssignIssue(dev, issue(IssueStatus.NEW)));
        assertThrows(SecurityException.class, () -> policy.assertCanManageDependency(dev, issue(IssueStatus.ASSIGNED)));
        assertThrows(SecurityException.class, () -> policy.assertCanChangePriority(tester, issue(IssueStatus.ASSIGNED)));
        assertThrows(NullPointerException.class,
                () -> policy.assertCanChangePriority(pl, issue(IssueStatus.ASSIGNED), null));
        assertThrows(SecurityException.class, () -> policy.assertCanManageDeletedIssue(dev, issue(IssueStatus.DELETED)));
        assertThrows(SecurityException.class, () -> policy.assertCanManageAccount(pl));
        assertThrows(SecurityException.class, () -> policy.assertCanManageProject(pl));
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
<<<<<<< HEAD
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        return User.create(loginId, loginId, "hash", role, true, null, null);
=======
        return new User(loginId, loginId, "hash", role, true, null, null);
>>>>>>> 88350b0 (ProjectController 테스트 보강)
    }

    private static User inactive(String loginId, Role role) {
        User user = user(loginId, role);
        user.deactivate();
        return user;
    }
}
