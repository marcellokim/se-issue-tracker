package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;

public record IssueStateResult(
        String issueId,
        IssueStatus status,
        User assignee,
        User verifier,
        User fixer,
        User resolver
) {
}
