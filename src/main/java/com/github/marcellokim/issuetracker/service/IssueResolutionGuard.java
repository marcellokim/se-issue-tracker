package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;

@FunctionalInterface
public interface IssueResolutionGuard {

    void assertCanResolve(Issue issue);

    static IssueResolutionGuard none() {
        return issue -> {
        };
    }
}
