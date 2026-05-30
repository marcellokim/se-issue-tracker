package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.repository.IssueHistoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FakeIssueHistoryRepository implements IssueHistoryRepository {

    private final Map<Long, IssueHistory> histories = new LinkedHashMap<>();
    private long nextId = 1L;

    @Override
    public Optional<IssueHistory> findById(long historyId) {
        return Optional.ofNullable(histories.get(historyId));
    }

    @Override
    public List<IssueHistory> findByIssueId(long issueId) {
        return histories.values().stream()
                .filter(history -> history.issueId() == issueId)
                .toList();
    }

    public IssueHistory save(IssueHistory history) {
        if (history.id() != 0L) {
            throw new IllegalArgumentException("Issue history must be new before save.");
        }
        IssueHistory saved = IssueHistory.fromPersistence(
                nextId++,
                history.issueId(),
                history.changedById(),
                history.actionType(),
                history.previousValue(),
                history.newValue(),
                history.message(),
                history.changedDate());
        histories.put(saved.id(), saved);
        return saved;
    }

    public void addFixture(IssueHistory history) {
        if (history.id() == 0L) {
            throw new IllegalArgumentException("Fixture history must be persisted.");
        }
        histories.put(history.id(), history);
        nextId = Math.max(nextId, history.id() + 1);
    }

    public List<IssueHistory> findAll() {
        return new ArrayList<>(histories.values());
    }
}
