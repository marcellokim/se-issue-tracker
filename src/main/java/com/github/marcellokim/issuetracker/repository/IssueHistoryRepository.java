package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.IssueHistory;
import java.util.List;
import java.util.Optional;

public interface IssueHistoryRepository {

    Optional<IssueHistory> findById(long historyId);

    List<IssueHistory> findByIssueId(long issueId);

    Optional<IssueHistory> findLatestStatusChangeToDeleted(long issueId);

    List<IssueHistory> findDeletedTransitionsByProject(long projectId);

    IssueHistory save(IssueHistory history);
}
