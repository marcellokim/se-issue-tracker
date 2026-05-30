package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.IssueHistory;
import java.util.List;

public interface IssueHistoryRepository {

    List<IssueHistory> findByIssueId(long issueId);
}
