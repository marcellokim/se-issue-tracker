package com.github.marcellokim.issuetracker.repository;

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

    List<IssueDependency> findByIssueId(long issueId);

    List<IssueDependency> findByBlockingIssueId(long blockingIssueId);

    List<IssueDependency> findByBlockedIssueId(long blockedIssueId);

    boolean existsByPair(long blockingIssueId, long blockedIssueId);

    IssueDependency save(IssueDependency dependency);

    void deleteById(long dependencyId);

    void deleteByIssueId(long issueId);
}
