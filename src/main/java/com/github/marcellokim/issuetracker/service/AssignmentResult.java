package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;

public record AssignmentResult(
        long id,
        String issueId,
        IssueStatus status,
        UserResult assignee,
        UserResult verifier
) {
}
