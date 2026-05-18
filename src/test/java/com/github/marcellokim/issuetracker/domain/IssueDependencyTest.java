package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 의존성")
class IssueDependencyTest {

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User pl = new User("U-2", "pl1", "PL One", "hash", Role.PL);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("의존성은 blocking issue와 blocked issue 방향으로 표현된다")
    void createDependencyWithBlockingAndBlockedDirection() {
        var blockingIssue = Issue.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter, createdAt);
        var blockedIssue = Issue.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
        var discoveredAt = createdAt.plusMinutes(20);

        var dependency = blockedIssue.addDependency("ISSUE-1->ISSUE-2", blockingIssue, pl, discoveredAt);

        assertEquals("ISSUE-1->ISSUE-2", dependency.getDependencyId());
        assertSame(blockingIssue, dependency.getBlockingIssue());
        assertSame(blockedIssue, dependency.getBlockedIssue());
        assertEquals(discoveredAt, dependency.getDiscoveredDate());
        assertSame(dependency, blockingIssue.getBlockingDependencies().getFirst());
        assertSame(dependency, blockedIssue.getBlockedByDependencies().getFirst());

        var history = findHistory(blockedIssue, ActionType.DEPENDENCY_CHANGED);
        assertEquals(ActionType.DEPENDENCY_CHANGED, history.getAction());
        assertEquals("ISSUE-1->ISSUE-2", history.getNewValue());
    }

    @Test
    @DisplayName("이슈는 자기 자신에 의존할 수 없다")
    void rejectSelfDependency() {
        var issue = Issue.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> issue.addDependency("SELF", issue, pl, createdAt));
    }

    @Test
    @DisplayName("같은 issueId를 가진 다른 인스턴스도 자기 의존성으로 거부한다")
    void rejectSelfDependencyByIssueId() {
        var blockedIssue = Issue.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter, createdAt);
        var sameIssueId = Issue.create("ISSUE-1", "Same auth", "Same logical issue", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> blockedIssue.addDependency("SELF", sameIssueId, pl, createdAt));
    }

    @Test
    @DisplayName("같은 blocking issue에 대한 의존성은 중복 추가할 수 없다")
    void rejectDuplicateDependency() {
        var blockingIssue = Issue.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter, createdAt);
        var blockedIssue = Issue.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
        blockedIssue.addDependency("ISSUE-1->ISSUE-2", blockingIssue, pl, createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> blockedIssue.addDependency("DUPLICATE", blockingIssue, pl, createdAt.plusMinutes(30)));
    }

    private static IssueHistory findHistory(Issue issue, ActionType action) {
        return issue.getHistories().stream()
                .filter(history -> history.getAction() == action)
                .findFirst()
                .orElseThrow(() -> new AssertionError("History not found for action " + action));
    }
}
