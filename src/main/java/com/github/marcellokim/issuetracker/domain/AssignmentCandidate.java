package com.github.marcellokim.issuetracker.domain;

public record AssignmentCandidate(
        User user,
        int completedIssueCount
) {
}
