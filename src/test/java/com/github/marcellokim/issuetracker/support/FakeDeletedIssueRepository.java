package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.DeletedIssueRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FakeDeletedIssueRepository implements DeletedIssueRepository {

    private final Map<Long, Issue> deletedIssues = new LinkedHashMap<>();
    private final IssueRepository issueRepository;

    public FakeDeletedIssueRepository() {
        this(null);
    }

    public FakeDeletedIssueRepository(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public FakeDeletedIssueRepository addDeletedIssue(Issue issue) {
        if (issue.status() != IssueStatus.DELETED) {
            throw new IllegalArgumentException("issue must be deleted");
        }
        deletedIssues.put(issue.id(), issue);
        save(issue);
        return this;
    }

    @Override
    public List<Issue> findDeletedByProject(long projectId) {
        return deletedIssues.values().stream()
                .filter(issue -> issue.projectId() == projectId)
                .sorted(Comparator.comparingLong(Issue::id))
                .toList();
    }

    @Override
    public Issue softDelete(Issue issue, String changedById, String message, LocalDateTime changedDate) {
        Issue deleted = copyWithStatus(issue, IssueStatus.DELETED, changedDate);
        deletedIssues.put(deleted.id(), deleted);
        save(deleted);
        return deleted;
    }

    @Override
    public Issue restore(Issue issue, String changedById, String message, LocalDateTime changedDate) {
        Issue restored = copyWithStatus(issue, IssueStatus.NEW, changedDate);
        deletedIssues.remove(restored.id());
        save(restored);
        return restored;
    }

    @Override
    public int purgeDeletedById(long issueId) {
        return deletedIssues.remove(issueId) == null ? 0 : 1;
    }

    @Override
    public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
        List<Issue> deleted = findDeletedByProject(projectId);
        int overflow = Math.max(0, deleted.size() - maxDeletedIssues);
        for (int index = 0; index < overflow; index++) {
            deletedIssues.remove(deleted.get(index).id());
        }
        return overflow;
    }

    private void save(Issue issue) {
        if (issueRepository != null) {
            issueRepository.save(issue);
        }
    }

    private static Issue copyWithStatus(Issue issue, IssueStatus status, LocalDateTime updatedAt) {
        return Issue.fromPersistence(Issue.persistedState(
                issue.projectId(),
                issue.title(),
                issue.description(),
                issue.getReporter())
                .id(issue.id())
                .issueId(issue.getIssueId())
                .reportedDate(issue.reportedDate())
                .priority(issue.priority())
                .status(status)
                .assignee(issue.getAssignee())
                .verifier(issue.getVerifier())
                .fixer(issue.getFixer())
                .resolver(issue.getResolver())
                .updatedAt(updatedAt));
    }
}
