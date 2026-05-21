package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;

public interface IssueDependencyChangeRepository {

    IssueDependency saveWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue);

    void deleteWithBlockedIssueHistory(IssueDependency dependency, Issue blockedIssue);
}
