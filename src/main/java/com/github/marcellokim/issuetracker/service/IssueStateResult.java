package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;

public record IssueStateResult(
        long id,
        String issueId,
        IssueStatus status,
        UserResult assignee,
        UserResult verifier,
        UserResult fixer,
        UserResult resolver
) {
}
