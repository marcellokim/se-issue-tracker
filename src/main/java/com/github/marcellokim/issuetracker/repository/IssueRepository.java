package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IssueRepository {

    Optional<Issue> findById(long issueId);

    List<Issue> findAllById(List<Long> issueIds);

    List<Issue> findByProject(long projectId);

    List<Issue> findDeletedByProject(long projectId);

    List<Issue> findByCriteria(IssueSearchCriteria criteria);

    boolean existsByProjectIdAndTitle(long projectId, String title);

    boolean existsByResponsibleUser(String userLoginId);

    boolean existsActiveAssignmentByProjectAndUser(long projectId, String loginId);

    Issue save(Issue issue);

    Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate);

    Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate);

    int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues);

    void purge(long issueId);
}
