package com.github.marcellokim.issuetracker.domain;

public record AssignmentContext(
        long issueId,
        IssueStatus status,
        String assigneeId,
        String verifierId
) {
}
