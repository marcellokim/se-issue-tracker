package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("권한 정책")
class PermissionPolicyTest {

    private final PermissionPolicy policy = new PermissionPolicy();
    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User assignee = new User("U-2", "dev1", "Dev One", "hash", Role.DEV);
    private final User verifier = new User("U-3", "tester2", "Tester Two", "hash", Role.TESTER);
    private final User pl = new User("U-4", "pl1", "PL One", "hash", Role.PL);
    private final User otherDev = new User("U-5", "dev2", "Dev Two", "hash", Role.DEV);
    private final User otherTester = new User("U-6", "tester3", "Tester Three", "hash", Role.TESTER);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("PL은 배정과 종료 권한을 가진다")
    void allowPlAssignmentAndClose() {
        assertDoesNotThrow(() -> policy.assertCanAssignIssue(pl, newIssue()));
        assertDoesNotThrow(() -> policy.assertCanReassignIssue(pl, assignedIssue()));
        assertDoesNotThrow(() -> policy.assertCanChangeVerifier(pl, fixedIssue()));
        assertDoesNotThrow(() -> policy.assertCanClose(pl, resolvedIssue()));
    }

    @Test
    @DisplayName("assignee DEV만 fixed 전이 권한을 가진다")
    void allowOnlyAssigneeToMarkFixed() {
        var issue = assignedIssue();

        assertDoesNotThrow(() -> policy.assertCanMarkFixed(assignee, issue));
        assertThrows(SecurityException.class, () -> policy.assertCanMarkFixed(otherDev, issue));
        assertThrows(SecurityException.class, () -> policy.assertCanMarkFixed(verifier, issue));
    }

    @Test
    @DisplayName("verifier TESTER만 resolved 전이 권한을 가진다")
    void allowOnlyVerifierToResolve() {
        var issue = fixedIssue();

        assertDoesNotThrow(() -> policy.assertCanResolve(verifier, issue));
        assertThrows(SecurityException.class, () -> policy.assertCanResolve(otherTester, issue));
        assertThrows(SecurityException.class, () -> policy.assertCanResolve(assignee, issue));
    }

    @Test
    @DisplayName("잘못된 role이나 상태에서는 권한 검사를 통과할 수 없다")
    void rejectInvalidPermissionContext() {
        assertThrows(SecurityException.class, () -> policy.assertCanAssignIssue(assignee, newIssue()));
        assertThrows(IllegalStateException.class, () -> policy.assertCanAssignIssue(pl, assignedIssue()));
        assertThrows(IllegalStateException.class, () -> policy.assertCanClose(pl, fixedIssue()));
    }

    private Issue newIssue() {
        return Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
    }

    private Issue assignedIssue() {
        var issue = newIssue();
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
        return issue;
    }

    private Issue fixedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        return issue;
    }

    private Issue resolvedIssue() {
        var issue = fixedIssue();
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        return issue;
    }
}
