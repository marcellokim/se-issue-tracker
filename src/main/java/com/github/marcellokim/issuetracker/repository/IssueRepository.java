package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface IssueRepository {

    Optional<Issue> findById(long issueId);

    List<Issue> findByProject(long projectId);

    List<Issue> findDeletedByProject(long projectId);

    List<Issue> findByCriteria(IssueSearchCriteria criteria);

    Issue save(Issue issue);

    void purge(long issueId);
}
