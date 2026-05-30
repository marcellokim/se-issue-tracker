package com.github.marcellokim.issuetracker.repository;

import java.util.List;
import java.util.Optional;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;

public interface IssueRepository {

    Optional<Issue> findById(long issueId);

    List<Issue> findAllById(List<Long> issueIds);

    List<Issue> findByCriteria(IssueSearchCriteria criteria);

    boolean existsByProjectIdAndTitle(long projectId, String title);

    boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId);

    boolean hasCurrentIssueResponsibility(String userLoginId);

    boolean hasCurrentIssueResponsibility(long projectId, String loginId);

    Issue save(Issue issue);
}
