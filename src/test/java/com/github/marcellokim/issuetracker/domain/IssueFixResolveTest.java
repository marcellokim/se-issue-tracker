package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 수정 완료와 검증 완료")
class IssueFixResolveTest {

        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User assignee = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User otherDeveloper = User.create("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User verifier = User.create("tester2", "Tester Two", "hash", Role.TESTER, true, null, null);
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User otherTester = User.create("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
        // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
        private final User pl = User.create("pl1", "PL One", "hash", Role.PL, true, null, null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("ASSIGNED 이슈를 fixed로 변경하면 fixer와 STATUS_CHANGED 이력이 기록된다")
        void markAssignedIssueFixed() {
                var issue = assignedIssue();
                var fixedAt = createdAt.plusMinutes(20);

                issue.markFixed(assignee, "Fix completed", fixedAt);

                assertSame(assignee, issue.getFixer());
                assertEquals(IssueStatus.FIXED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.ASSIGNED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.FIXED.name(), history.getNewValue());
                assertEquals("Fix completed", history.getMessage());
                assertSame(assignee, history.getChangedBy());
        }

        @Test
        @DisplayName("FIXED 이슈를 resolved로 변경하면 resolver와 STATUS_CHANGED 이력이 기록된다")
        void resolveFixedIssue() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                var resolvedAt = createdAt.plusMinutes(30);

                issue.resolve(verifier, "Verified", resolvedAt);

                assertSame(verifier, issue.getResolver());
                assertEquals(IssueStatus.RESOLVED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.RESOLVED.name(), history.getNewValue());
                assertEquals("Verified", history.getMessage());
                assertSame(verifier, history.getChangedBy());
        }

        @Test
        @DisplayName("현재 assignee만 fixed 전이를 수행할 수 있다")
        void onlyCurrentAssigneeCanMarkFixed() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(otherDeveloper, "Fix completed", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("현재 verifier만 resolved 전이를 수행할 수 있다")
        void onlyCurrentVerifierCanResolve() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(otherTester, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("fixer는 DEV, resolver는 TESTER여야 한다")
        void rejectInvalidFixerAndResolverRoles() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(verifier, "Fix completed", createdAt.plusMinutes(20)));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(assignee, "Verified", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("ASSIGNED가 아니면 fixed로, FIXED가 아니면 resolved로 변경할 수 없다")
        void rejectInvalidSourceStatuses() {
                var newIssue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                var assignedIssue = assignedIssue();
                var fixedIssue = assignedIssue();
                fixedIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalStateException.class,
                                () -> newIssue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20)));
                assertThrows(IllegalStateException.class,
                                () -> fixedIssue.markFixed(assignee, "Fix completed again", createdAt.plusMinutes(30)));
                assertThrows(IllegalStateException.class,
                                () -> newIssue.resolve(verifier, "Verified", createdAt.plusMinutes(20)));
                assertThrows(IllegalStateException.class,
                                () -> assignedIssue.resolve(verifier, "Verified", createdAt.plusMinutes(20)));
        }

        @Test
        @DisplayName("상태 변경 comment는 비어 있을 수 없다")
        void rejectBlankStatusComment() {
                var issue = assignedIssue();

                assertThrows(IllegalArgumentException.class,
                                () -> issue.markFixed(assignee, "", createdAt.plusMinutes(20)));
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issue.resolve(verifier, " ", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("비활성 사용자는 fixer 또는 resolver가 될 수 없다")
        void rejectInactiveFixerAndResolver() {
                LocalDateTime now = LocalDateTime.now();
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                var inactiveFixer = User.create("dev2", "Dev Two", "hash", Role.DEV, true, null, null);
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                var inactiveResolver = User.create("tester3", "Tester Three", "hash", Role.TESTER, true, null, null);
                inactiveFixer.deactivate(now);
                inactiveResolver.deactivate(now);

                var issueForFixer = assignedIssue();
                assertThrows(IllegalArgumentException.class,
                                () -> issueForFixer.markFixed(inactiveFixer, "Fix completed",
                                                createdAt.plusMinutes(20)));

                var issueForResolver = assignedIssue();
                issueForResolver.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
                assertThrows(IllegalArgumentException.class,
                                () -> issueForResolver.resolve(inactiveResolver, "Verified",
                                                createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("FIXED 이슈를 rejectFix하면 ASSIGNED로 돌아간다")
        void rejectFixReturnsToAssigned() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                issue.rejectFix(verifier, "Not fixed properly", createdAt.plusMinutes(30));

                assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
                var history = issue.getHistories().getLast();
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.FIXED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.ASSIGNED.name(), history.getNewValue());
        }

        @Test
        @DisplayName("현재 verifier만 rejectFix할 수 있다")
        void onlyCurrentVerifierCanRejectFix() {
                var issue = assignedIssue();
                issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));

                assertThrows(IllegalArgumentException.class,
                                () -> issue.rejectFix(otherTester, "Not fixed", createdAt.plusMinutes(30)));
        }

        @Test
        @DisplayName("FIXED가 아닌 이슈는 rejectFix할 수 없다")
        void rejectFixRequiresFixedStatus() {
                var issue = assignedIssue();

                assertThrows(IllegalStateException.class,
                                () -> issue.rejectFix(verifier, "Not fixed", createdAt.plusMinutes(20)));
        }

        private Issue assignedIssue() {
                var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
                issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
                return issue;
        }
}
