package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.repository.IssueSearchCriteria;

public final class IssueSearchCriteriaTestFactory {

    private IssueSearchCriteriaTestFactory() {
    }

    public static IssueSearchCriteria all(long projectId) {
        return IssueSearchCriteria.create(projectId, null, null, null, null, null, null, null, null, false);
    }
}
