package com.github.marcellokim.issuetracker.domain;

public record AssignmentCandidate(
        User user,
        int completedIssueCount,
        String reason
) {

    public AssignmentCandidate(User user, int completedIssueCount) {
        this(
                user,
                completedIssueCount,
                completedIssueCount > 0
                        ? "Resolved/closed issue history count: " + completedIssueCount
                        : "Fallback active project member with no resolved/closed history yet"
        );
    }
}
