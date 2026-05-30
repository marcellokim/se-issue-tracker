package com.github.marcellokim.issuetracker.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;

public interface IssueRepository {

    Optional<Issue> findById(long issueId);

    List<Issue> findAllById(List<Long> issueIds);

    List<Issue> findByProject(long projectId);

    List<Issue> findDeletedByProject(long projectId);

    List<Issue> findByCriteria(IssueSearchCriteria criteria);

    boolean existsByProjectIdAndTitle(long projectId, String title);

    boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId);

    boolean existsByResponsibleUser(String userLoginId);

    boolean existsActiveAssignmentByProjectAndUser(long projectId, String loginId);

    Issue save(Issue issue);

    Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate);

    Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate);

    int purgeDeletedById(long issueId);

    int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues);
}
