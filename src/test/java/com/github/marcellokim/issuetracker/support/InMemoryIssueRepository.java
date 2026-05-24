package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIssueRepository implements IssueRepository {

    private final Map<Long, Issue> issues = new LinkedHashMap<>();

    public InMemoryIssueRepository(Issue... issues) {
        for (Issue issue : issues) {
            save(issue);
        }
    }

    @Override
    public Optional<Issue> findById(long issueId) {
        return Optional.ofNullable(issues.get(issueId));
    }

    @Override
    public List<Issue> findAllById(List<Long> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return List.of();
        }
        return issueIds.stream()
                .map(issues::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public List<Issue> findByProject(long projectId) {
        return issues.values().stream()
                .filter(issue -> issue.projectId() == projectId)
                .toList();
    }

    @Override
    public List<Issue> findDeletedByProject(long projectId) {
        return issues.values().stream()
                .filter(issue -> issue.projectId() == projectId)
                .filter(issue -> issue.status() == IssueStatus.DELETED)
                .toList();
    }

    @Override
    public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
        return new ArrayList<>(issues.values());
    }

    @Override
    public Issue save(Issue issue) {
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
