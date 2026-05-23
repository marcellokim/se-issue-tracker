package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.User;

public record IssueResult(
        long id,
        String issueId,
        IssueStatus status,
        Priority priority,
        String title,
        String description,
        User reporter
) {
}
