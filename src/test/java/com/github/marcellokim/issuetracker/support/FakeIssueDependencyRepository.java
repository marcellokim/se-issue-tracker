package com.github.marcellokim.issuetracker.support;

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
    public Optional<IssueDependency> findById(long dependencyId) {
        return Optional.ofNullable(dependencies.get(dependencyId));
    }

    @Override
    public List<IssueDependency> findByIssueId(long issueId) {
        return dependencies.values().stream()
                .filter(d -> d.blockingIssueId() == issueId || d.blockedIssueId() == issueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findByBlockingIssueId(long blockingIssueId) {
        return dependencies.values().stream()
                .filter(d -> d.blockingIssueId() == blockingIssueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findByBlockedIssueId(long blockedIssueId) {
        return dependencies.values().stream()
                .filter(d -> d.blockedIssueId() == blockedIssueId)
                .toList();
    }

    @Override
    public boolean existsByPair(long blockingIssueId, long blockedIssueId) {
        return dependencies.values().stream()
                .anyMatch(d -> d.blockingIssueId() == blockingIssueId && d.blockedIssueId() == blockedIssueId);
    }

    @Override
    public IssueDependency save(IssueDependency dependency) {
        long id = dependency.id() == 0L ? nextId++ : dependency.id();
        var persisted = IssueDependency.fromPersistence(
                id, dependency.getDependencyId(),
                dependency.blockingIssueId(), dependency.blockedIssueId(),
                dependency.discoveredDate());
        dependencies.put(id, persisted);
        return persisted;
    }

    @Override
    public void deleteById(long dependencyId) {
        dependencies.remove(dependencyId);
    }

    @Override
    public void deleteByIssueId(long issueId) {
        dependencies.entrySet().removeIf(
                e -> e.getValue().blockingIssueId() == issueId || e.getValue().blockedIssueId() == issueId);
    }
}
