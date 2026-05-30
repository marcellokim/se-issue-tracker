package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FakeIssueDependencyRepository implements IssueDependencyRepository {

    private final Map<Long, IssueDependency> dependencies = new LinkedHashMap<>();
    private long nextId = 1L;

    @Override
    public Optional<IssueDependency> findByDependencyId(String dependencyId) {
        return dependencies.values().stream()
                .filter(dependency -> dependency.getDependencyId().equals(dependencyId))
                .findFirst();
    }

    @Override
    public List<IssueDependency> findDependenciesBlockedByIssue(long blockingIssueId) {
        return dependencies.values().stream()
                .filter(d -> d.blockingIssueId() == blockingIssueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findDependenciesBlockingIssue(long blockedIssueId) {
        return dependencies.values().stream()
                .filter(d -> d.blockedIssueId() == blockedIssueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findByProjectId(long projectId) {
        return List.copyOf(dependencies.values());
    }

    @Override
    public boolean existsByPair(long blockingIssueId, long blockedIssueId) {
        return dependencies.values().stream()
                .anyMatch(d -> d.blockingIssueId() == blockingIssueId && d.blockedIssueId() == blockedIssueId);
    }

    public IssueDependency addFixture(IssueDependency dependency) {
        long id = dependency.id() == 0L ? nextId++ : dependency.id();
        var persisted = IssueDependency.fromPersistence(
                id, dependency.getDependencyId(),
                dependency.blockingIssueId(), dependency.blockedIssueId(),
                dependency.discoveredDate());
        dependencies.put(id, persisted);
        return persisted;
    }

    @Override
    public IssueDependency recordDependencyAdded(IssueDependency dependency, Issue issue) {
        return addFixture(dependency);
    }

    @Override
    public void recordDependencyRemoved(String dependencyId, Issue issue) {
        IssueDependency dependency = findByDependencyId(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found: " + dependencyId));
        if (!dependencies.containsKey(dependency.id())) {
            throw new IllegalArgumentException("Dependency not found: " + dependencyId);
        }
        dependencies.remove(dependency.id());
    }
}
