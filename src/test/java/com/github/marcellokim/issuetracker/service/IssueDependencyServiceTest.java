package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueDependencyChangeRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue dependency service")
class IssueDependencyServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

    private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, NOW, NOW);
    private final User pl = User.create("pl1", "PL One", "hash", Role.PL, true, NOW, NOW);
    private final User dev = User.create("dev1", "Dev One", "hash", Role.DEV, true, NOW, NOW);

    @Test
    @DisplayName("PL adds dependency with blocking and blocked direction and dependency history")
    void plAddsDependencyWithDirectionAndHistory() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository();
        IssueDependencyService service = service(new InMemoryIssueRepository(blocking, blocked), dependencies);

        IssueDependencyResult result = service.addDependency(blocking.id(), blocked.id(), pl.getLoginId());

        assertEquals(1L, result.id());
        assertEquals(IssueDependency.dependencyIdFor(blocking.id(), blocked.id()), result.dependencyId());
        assertEquals(blocking.id(), result.blockingIssueId());
        assertEquals(blocked.id(), result.blockedIssueId());
        assertEquals(List.of(blocking.id()), dependencies.findByIssueId(blocked.id()).stream()
                .map(IssueDependency::blockingIssueId)
                .toList());

        IssueHistory history = dependencyHistory(blocked);
        assertEquals(ActionType.DEPENDENCY_CHANGED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals(result.dependencyId(), history.getNewValue());
        assertEquals("Dependency added", history.getMessage());
    }

    @Test
    @DisplayName("add delegates relation and blocked issue history write to change repository")
    void addDelegatesWriteThroughChangeRepository() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        TrackingIssueRepository issues = new TrackingIssueRepository(blocking, blocked);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository();
        SpyDependencyChangeRepository changes = new SpyDependencyChangeRepository(dependencies);
        IssueDependencyService service = service(issues, dependencies, changes);

        IssueDependencyResult result = service.addDependency(blocking.id(), blocked.id(), pl.getLoginId());

        assertEquals(result.dependencyId(), changes.savedDependency.getDependencyId());
        assertEquals(blocked, changes.savedBlockedIssue);
        assertEquals(1, changes.saveCalls);
        assertEquals(0, issues.saveCalls);
    }

    @Test
    @DisplayName("list includes incoming and outgoing dependencies for an issue")
    void listIncludesIncomingAndOutgoingDependencies() {
        Issue a = issue(101L);
        Issue b = issue(102L);
        Issue c = issue(103L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository(
                persistedDependency(1L, a.id(), b.id()),
                persistedDependency(2L, b.id(), c.id()));
        IssueDependencyService service = service(new InMemoryIssueRepository(a, b, c), dependencies);

        List<IssueDependencyResult> results = service.listDependencies(b.id(), pl.getLoginId());

        assertEquals(List.of(1L, 2L), results.stream().map(IssueDependencyResult::id).toList());
        assertEquals(List.of(a.id(), b.id()), results.stream().map(IssueDependencyResult::blockingIssueId).toList());
        assertEquals(List.of(b.id(), c.id()), results.stream().map(IssueDependencyResult::blockedIssueId).toList());
    }

    @Test
    @DisplayName("self dependency is rejected before save and has no history side effect")
    void rejectSelfDependencyBeforeSaveAndHistory() {
        Issue issue = issue(101L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository();
        IssueDependencyService service = service(new InMemoryIssueRepository(issue), dependencies);

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(issue.id(), issue.id(), pl.getLoginId()));

        assertEquals(List.of(), dependencies.findAll());
        assertFalse(hasDependencyHistory(issue));
    }

    @Test
    @DisplayName("stored duplicate is rejected even without hydrated aggregate dependency")
    void rejectStoredDuplicateWithoutHydratedAggregateDependency() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository(
                persistedDependency(1L, blocking.id(), blocked.id()));
        IssueDependencyService service = service(new InMemoryIssueRepository(blocking, blocked), dependencies);

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(blocking.id(), blocked.id(), pl.getLoginId()));

        assertEquals(1, dependencies.findAll().size());
        assertFalse(hasDependencyHistory(blocked));
    }

    @Test
    @DisplayName("existing A -> B -> C chain rejects C -> A cycle")
    void rejectCycleAcrossExistingRepositoryEdges() {
        Issue a = issue(101L);
        Issue b = issue(102L);
        Issue c = issue(103L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository(
                persistedDependency(1L, a.id(), b.id()),
                persistedDependency(2L, b.id(), c.id()));
        IssueDependencyService service = service(new InMemoryIssueRepository(a, b, c), dependencies);

        assertThrows(IllegalArgumentException.class,
                () -> service.addDependency(c.id(), a.id(), pl.getLoginId()));

        assertEquals(2, dependencies.findAll().size());
        assertFalse(hasDependencyHistory(a));
    }

    @Test
    @DisplayName("remove deletes dependency and records blocked issue dependency history")
    void removeDeletesAndRecordsBlockedIssueHistory() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        IssueDependency dependency = persistedDependency(1L, blocking.id(), blocked.id());
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository(dependency);
        IssueDependencyService service = service(new InMemoryIssueRepository(blocking, blocked), dependencies);

        service.removeDependency(dependency.id(), pl.getLoginId());

        assertEquals(List.of(), dependencies.findAll());
        IssueHistory history = dependencyHistory(blocked);
        assertEquals(ActionType.DEPENDENCY_CHANGED, history.getAction());
        assertEquals(dependency.getDependencyId(), history.getPreviousValue());
        assertNull(history.getNewValue());
        assertEquals("Dependency removed", history.getMessage());
    }

    @Test
    @DisplayName("remove delegates relation delete and blocked issue history write to change repository")
    void removeDelegatesWriteThroughChangeRepository() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        IssueDependency dependency = persistedDependency(1L, blocking.id(), blocked.id());
        TrackingIssueRepository issues = new TrackingIssueRepository(blocking, blocked);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository(dependency);
        SpyDependencyChangeRepository changes = new SpyDependencyChangeRepository(dependencies);
        IssueDependencyService service = service(issues, dependencies, changes);

        service.removeDependency(dependency.id(), pl.getLoginId());

        assertEquals(dependency.getDependencyId(), changes.deletedDependency.getDependencyId());
        assertEquals(blocked, changes.deletedBlockedIssue);
        assertEquals(1, changes.deleteCalls);
        assertEquals(0, issues.saveCalls);
    }

    @Test
    @DisplayName("non-PL cannot add or list dependencies")
    void rejectNonPlAddAndList() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        IssueDependencyService service = service(new InMemoryIssueRepository(blocking, blocked),
                new InMemoryIssueDependencyRepository());

        assertThrows(SecurityException.class,
                () -> service.addDependency(blocking.id(), blocked.id(), dev.getLoginId()));
        assertThrows(SecurityException.class,
                () -> service.listDependencies(blocked.id(), dev.getLoginId()));
    }

    private IssueDependencyService service(
            InMemoryIssueRepository issues,
            InMemoryIssueDependencyRepository dependencies
    ) {
        return service(issues, dependencies, dependencies);
    }

    private IssueDependencyService service(
            IssueRepository issues,
            InMemoryIssueDependencyRepository dependencies,
            IssueDependencyChangeRepository changes
    ) {
        return new IssueDependencyService(
                issues,
                dependencies,
                changes,
                new InMemoryUserRepository(reporter, pl, dev),
                new PermissionPolicy(),
                new Clock());
    }

    private Issue issue(long id) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Issue " + id, "Dependency test", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(NOW));
    }

    private static IssueDependency persistedDependency(long id, long blockingIssueId, long blockedIssueId) {
        return IssueDependency.fromPersistence(id, blockingIssueId, blockedIssueId, NOW);
    }

    private static IssueHistory dependencyHistory(Issue issue) {
        return issue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.DEPENDENCY_CHANGED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dependency history not found"));
    }

    private static boolean hasDependencyHistory(Issue issue) {
        return issue.getHistories().stream()
                .anyMatch(history -> history.getAction() == ActionType.DEPENDENCY_CHANGED);
    }

    private static final class SpyDependencyChangeRepository implements IssueDependencyChangeRepository {

        private final InMemoryIssueDependencyRepository delegate;
        private IssueDependency savedDependency;
        private Issue savedBlockedIssue;
        private IssueDependency deletedDependency;
        private Issue deletedBlockedIssue;
        private int saveCalls;
        private int deleteCalls;

        private SpyDependencyChangeRepository(InMemoryIssueDependencyRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public IssueDependency saveWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
            saveCalls++;
            savedDependency = dependency;
            savedBlockedIssue = blockedIssue;
            return delegate.save(dependency);
        }

        @Override
        public void deleteWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
            deleteCalls++;
            deletedDependency = dependency;
            deletedBlockedIssue = blockedIssue;
            delegate.deleteById(dependency.id());
        }
    }

    private static final class TrackingIssueRepository implements IssueRepository {

        private final Map<Long, Issue> issues = new LinkedHashMap<>();
        private int saveCalls;

        private TrackingIssueRepository(Issue... issues) {
            for (Issue issue : issues) {
                this.issues.put(issue.id(), issue);
            }
        }

        @Override
        public Optional<Issue> findById(long issueId) {
            return Optional.ofNullable(issues.get(issueId));
        }

        @Override
        public List<Issue> findByProject(long projectId) {
            return issues.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .toList();
        }

        @Override
        public List<Issue> findDeletedByProject(long projectId) {
            return List.of();
        }

        @Override
        public List<Issue> findByCriteria(com.github.marcellokim.issuetracker.domain.IssueSearchCriteria criteria) {
            return new ArrayList<>(issues.values());
        }

        @Override
        public Issue save(Issue issue) {
            saveCalls++;
            issues.put(issue.id(), issue);
            return issue;
        }

        @Override
        public Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("softDelete is not needed by these service tests");
        }

        @Override
        public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("restore is not needed by these service tests");
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            return 0;
        }

        @Override
        public void purge(long issueId) {
            issues.remove(issueId);
        }
    }
}
