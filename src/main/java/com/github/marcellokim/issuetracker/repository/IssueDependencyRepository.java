package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import java.util.List;
import java.util.Optional;

public interface IssueDependencyRepository {

    /*
     * IssueDependency domain behavior is not finalized yet.
     * Keep this repository contract as-is until the domain layer decides the
     * dependency aggregate rules and service flow.
     */

    Optional<IssueDependency> findById(long dependencyId);

    Optional<IssueDependency> findByDependencyId(String dependencyId);

    List<IssueDependency> findByIssueId(long issueId);

    List<IssueDependency> findByBlockingIssueId(long blockingIssueId);

    List<IssueDependency> findByBlockedIssueId(long blockedIssueId);

    List<IssueDependency> findByProjectId(long projectId);

    boolean existsByPair(long blockingIssueId, long blockedIssueId);

    IssueDependency saveAndRecordIssueChange(IssueDependency dependency, Issue issue);

    void deleteByDependencyIdAndRecordIssueChange(String dependencyId, Issue issue);
}
