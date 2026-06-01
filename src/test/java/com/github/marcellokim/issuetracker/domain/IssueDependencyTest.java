package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue dependency")
class IssueDependencyTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("dependency points from blocker to blocked issue")
    void keepsDependencyDirection() {
        var blockingIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter,
                createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
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
    @DisplayName("an issue cannot depend on the same issue id")
    void doesNotDependOnSameIssueId() {
        var blockedIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter, createdAt);
        var sameIssueId = IssueFixtures.create("ISSUE-1", "Same auth", "Same logical issue", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> blockedIssue.addDependency("SELF", sameIssueId, pl, createdAt));
    }

    @Test
    @DisplayName("same blocking issue cannot be added twice")
    void rejectsDuplicateDependency() {
        var blockingIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter,
                createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
        blockedIssue.addDependency("ISSUE-1->ISSUE-2", blockingIssue, pl, createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> blockedIssue.addDependency("DUPLICATE", blockingIssue, pl, createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("multiple blocking issues can be added")
    void addsMultipleDependencies() {
        var blockingIssue1 = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth fix", null, reporter, createdAt);
        var blockingIssue2 = IssueFixtures.create("ISSUE-3", "Fix DB", "DB fix", null, reporter, createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends", null, reporter, createdAt);

        blockedIssue.addDependency("ISSUE-1->ISSUE-2", blockingIssue1, pl, createdAt.plusMinutes(20));
        blockedIssue.addDependency("ISSUE-3->ISSUE-2", blockingIssue2, pl, createdAt.plusMinutes(30));

        assertEquals(2, blockedIssue.getBlockedByDependencies().size());
        assertEquals(1, blockingIssue1.getBlockingDependencies().size());
        assertEquals(1, blockingIssue2.getBlockingDependencies().size());
    }

    @Test
    @DisplayName("removing dependency updates both sides")
    void removesDependencyFromBothSides() {
        var blockingIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter,
                createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
        var dependency = blockedIssue.addDependency("ISSUE-1->ISSUE-2", blockingIssue, pl, createdAt.plusMinutes(20));

        blockedIssue.removeDependency(dependency, pl, createdAt.plusMinutes(30));

        assertEquals(0, blockedIssue.getBlockedByDependencies().size());
        assertEquals(0, blockingIssue.getBlockingDependencies().size());

        var history = blockedIssue.getHistories().getLast();
        assertEquals(ActionType.DEPENDENCY_CHANGED, history.getAction());
        assertEquals("ISSUE-1->ISSUE-2", history.getPreviousValue());
        assertNull(history.getNewValue());
    }

    @Test
    @DisplayName("other issue's dependency is not removed")
    void doesNotRemoveOtherIssueDependency() {
        var blockingIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter,
                createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);
        var otherIssue = IssueFixtures.create("ISSUE-3", "Other", "Other issue", null, reporter, createdAt);
        var dependency = otherIssue.addDependency("ISSUE-1->ISSUE-3", blockingIssue, pl, createdAt.plusMinutes(20));

        assertThrows(IllegalArgumentException.class,
                () -> blockedIssue.removeDependency(dependency, pl, createdAt.plusMinutes(30)));
    }

    @Test
    @DisplayName("loaded dependency gets the generated id")
    void loadedDependencyGetsGeneratedId() {
        var dependency = IssueDependency.fromPersistence(7L, 10L, 20L, createdAt);

        assertEquals(7L, dependency.id());
        assertEquals(10L, dependency.blockingIssueId());
        assertEquals(20L, dependency.blockedIssueId());
        assertEquals(createdAt, dependency.discoveredDate());
        assertEquals(IssueDependency.dependencyIdFor(10L, 20L), dependency.getDependencyId());
    }

    @Test
    @DisplayName("dependency needs an id and a blocking issue")
    void requiresIdAndBlockingIssue() {
        var blockingIssue = IssueFixtures.create("ISSUE-1", "Fix auth", "Auth must be fixed", null, reporter,
                createdAt);
        var blockedIssue = IssueFixtures.create("ISSUE-2", "Login UI", "UI depends on auth", null, reporter, createdAt);

        assertThrows(IllegalArgumentException.class,
                () -> IssueDependency.create("", blockingIssue, blockedIssue, createdAt));
        assertThrows(NullPointerException.class,
                () -> IssueDependency.create("DEP", null, blockedIssue, createdAt));
    }

    private static IssueHistory findHistory(Issue issue, ActionType action) {
        return issue.getHistories().stream()
                .filter(history -> history.getAction() == action)
                .findFirst()
                .orElseThrow(() -> new AssertionError("History not found for action " + action));
    }
}
