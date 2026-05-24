package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;

public record AssignmentResult(
        long id,
        String issueId,
        IssueStatus status,
        User assignee,
        User verifier
) {
}
