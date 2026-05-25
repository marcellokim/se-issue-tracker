package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIssueRepository implements IssueRepository {

    private final Map<Long, Issue> issues = new LinkedHashMap<>();
    private long nextId = 1L;

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
        return issues.values().stream()
                .filter(issue -> issue.projectId() == criteria.projectId())
                .filter(issue -> criteria.status() == null || issue.status() == criteria.status())
                .filter(issue -> criteria.includeDeleted() || issue.status() != IssueStatus.DELETED)
                .filter(issue -> criteria.priority() == null || issue.priority() == criteria.priority())
                .filter(issue -> criteria.reporterId() == null || criteria.reporterId().equals(issue.reporterId()))
                .filter(issue -> criteria.assigneeId() == null || criteria.assigneeId().equals(issue.assigneeId()))
                .filter(issue -> criteria.verifierId() == null || criteria.verifierId().equals(issue.verifierId()))
                .filter(issue -> matchesKeyword(issue, criteria.keyword()))
                .filter(issue -> criteria.reportedFrom() == null || !issue.reportedDate().isBefore(criteria.reportedFrom()))
                .filter(issue -> criteria.reportedTo() == null || issue.reportedDate().isBefore(criteria.reportedTo()))
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndTitle(long projectId, String title) {
        return issues.values().stream()
                .anyMatch(issue -> issue.projectId() == projectId && issue.title().equals(title));
    }

    @Override
    public boolean existsByResponsibleUser(String userLoginId) {
        return issues.values().stream()
                .filter(issue -> issue.status() != IssueStatus.DELETED)
                .anyMatch(issue -> userLoginId.equals(issue.assigneeId())
                        || userLoginId.equals(issue.verifierId())
                        || userLoginId.equals(issue.fixerId())
                        || userLoginId.equals(issue.resolverId()));
    }

    @Override
    public Issue save(Issue issue) {
        Issue saved = issue.id() == 0L ? persistNew(issue) : issue;
        issues.put(saved.id(), saved);
        nextId = Math.max(nextId, saved.id() + 1);
        return saved;
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

    private Issue persistNew(Issue issue) {
        return Issue.fromPersistence(Issue.persistedState(
                        issue.projectId(),
                        issue.title(),
                        issue.description(),
                        issue.getReporter())
                .id(nextId++)
                .issueId(issue.getIssueId())
                .reportedDate(issue.reportedDate())
                .priority(issue.priority())
                .status(issue.status())
                .assignee(issue.getAssignee())
                .verifier(issue.getVerifier())
                .fixer(issue.getFixer())
                .resolver(issue.getResolver())
                .updatedAt(issue.updatedAt()));
    }

    private static boolean matchesKeyword(Issue issue, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.toLowerCase();
        return issue.title().toLowerCase().contains(normalizedKeyword)
                || issue.description().toLowerCase().contains(normalizedKeyword);
    }
}
