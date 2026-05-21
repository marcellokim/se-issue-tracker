package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.repository.IssueDependencyChangeRepository;
import com.github.marcellokim.issuetracker.repository.IssueDependencyRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIssueDependencyRepository
        implements IssueDependencyRepository, IssueDependencyChangeRepository {

    private final Map<Long, IssueDependency> dependencies = new LinkedHashMap<>();
    private long nextId = 1L;

    public InMemoryIssueDependencyRepository(IssueDependency... dependencies) {
        for (IssueDependency dependency : dependencies) {
            save(dependency);
        }
    }

    @Override
    public Optional<IssueDependency> findById(long dependencyId) {
        return Optional.ofNullable(dependencies.get(dependencyId));
    }

    @Override
    public List<IssueDependency> findByIssueId(long issueId) {
        return dependencies.values().stream()
                .filter(dependency -> dependency.blockingIssueId() == issueId || dependency.blockedIssueId() == issueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findByBlockingIssueId(long blockingIssueId) {
        return dependencies.values().stream()
                .filter(dependency -> dependency.blockingIssueId() == blockingIssueId)
                .toList();
    }

    @Override
    public List<IssueDependency> findByBlockedIssueId(long blockedIssueId) {
        return dependencies.values().stream()
                .filter(dependency -> dependency.blockedIssueId() == blockedIssueId)
                .toList();
    }

    @Override
    public boolean existsByPair(long blockingIssueId, long blockedIssueId) {
        return dependencies.values().stream()
                .anyMatch(dependency -> dependency.blockingIssueId() == blockingIssueId
                        && dependency.blockedIssueId() == blockedIssueId);
    }

    @Override
    public IssueDependency save(IssueDependency dependency) {
        IssueDependency saved = dependency.id() == 0L
                ? IssueDependency.fromPersistence(
                        nextId++,
                        dependency.getDependencyId(),
                        dependency.blockingIssueId(),
                        dependency.blockedIssueId(),
                        dependency.discoveredDate())
                : dependency;
        dependencies.put(saved.id(), saved);
        if (saved.id() >= nextId) {
            nextId = saved.id() + 1L;
        }
        return saved;
    }

    @Override
    public IssueDependency saveWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
        return save(dependency);
    }

    @Override
    public void deleteById(long dependencyId) {
        dependencies.remove(dependencyId);
    }

    @Override
    public void deleteWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue) {
        deleteById(dependency.id());
    }

    @Override
    public void deleteByIssueId(long issueId) {
        new ArrayList<>(dependencies.values()).stream()
                .filter(dependency -> dependency.blockingIssueId() == issueId || dependency.blockedIssueId() == issueId)
                .map(IssueDependency::id)
                .forEach(dependencies::remove);
    }

    public List<IssueDependency> findAll() {
        return List.copyOf(dependencies.values());
    }
}
