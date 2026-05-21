package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dependency resolution guard")
class DependencyResolutionGuardTest {

    private static final long PROJECT_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);
    private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, NOW, NOW);
    private final User assignee = User.create("dev1", "Dev One", "hash", Role.DEV, true, NOW, NOW);
    private final User verifier = User.create("tester2", "Tester Two", "hash", Role.TESTER, true, NOW, NOW);
    private final User pl = User.create("pl1", "PL One", "hash", Role.PL, true, NOW, NOW);

    @Test
    @DisplayName("unresolved blocking issue rejects resolve")
    void unresolvedBlockingIssueRejectsResolve() {
        Issue blocked = issue(1L, IssueStatus.FIXED);
        Issue blocking = issue(2L, IssueStatus.FIXED);
        var guard = guard(new InMemoryIssueRepository(blocked, blocking),
                new InMemoryIssueDependencyRepository(dependency(1L, blocking.id(), blocked.id())));

        assertThrows(IllegalStateException.class, () -> guard.assertCanResolve(blocked));
    }

    @Test
    @DisplayName("resolved and closed blockers allow resolve")
    void resolvedAndClosedBlockersAllowResolve() {
        Issue blocked = issue(1L, IssueStatus.FIXED);
        Issue resolvedBlocking = issue(2L, IssueStatus.RESOLVED);
        Issue closedBlocking = issue(3L, IssueStatus.CLOSED);
        var guard = guard(new InMemoryIssueRepository(blocked, resolvedBlocking, closedBlocking),
                new InMemoryIssueDependencyRepository(
                        dependency(1L, resolvedBlocking.id(), blocked.id()),
                        dependency(2L, closedBlocking.id(), blocked.id())));

        assertDoesNotThrow(() -> guard.assertCanResolve(blocked));
    }

    @Test
    @DisplayName("missing blocking issue rejects resolve")
    void missingBlockingIssueRejectsResolve() {
        Issue blocked = issue(1L, IssueStatus.FIXED);
        var guard = guard(new InMemoryIssueRepository(blocked),
                new InMemoryIssueDependencyRepository(dependency(1L, 999L, blocked.id())));

        assertThrows(IllegalStateException.class, () -> guard.assertCanResolve(blocked));
    }

    private DependencyResolutionGuard guard(
            InMemoryIssueRepository issues,
            InMemoryIssueDependencyRepository dependencies
    ) {
        return new DependencyResolutionGuard(issues, dependencies);
    }

    private IssueDependency dependency(long id, long blockingIssueId, long blockedIssueId) {
        return IssueDependency.fromPersistence(id, blockingIssueId, blockedIssueId, NOW);
    }

    private Issue issue(long id, IssueStatus status) {
        var builder = Issue.persistedState(PROJECT_ID, "Issue " + id, "Dependency guard test", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .updatedAt(NOW);
        if (status == IssueStatus.FIXED) {
            builder.assignee(assignee).verifier(verifier).fixer(assignee);
        } else if (status == IssueStatus.RESOLVED) {
            builder.assignee(assignee).verifier(verifier).fixer(assignee).resolver(verifier);
        } else if (status == IssueStatus.CLOSED) {
            builder.fixer(assignee).resolver(verifier);
        }
        return Issue.fromPersistence(builder);
    }
}
