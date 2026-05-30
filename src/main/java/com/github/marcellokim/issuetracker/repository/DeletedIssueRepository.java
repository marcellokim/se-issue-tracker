package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import java.time.LocalDateTime;
import java.util.List;

public interface DeletedIssueRepository {

    List<Issue> findDeletedByProject(long projectId);

    Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate);

    Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate);

    int purgeDeletedById(long issueId);

    int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues);
}
