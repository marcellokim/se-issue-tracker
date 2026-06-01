package com.github.marcellokim.issuetracker.ui.swing;

record IssueDependencySelection(long blockingIssueId, long blockedIssueId) {

    IssueDependencySelection {
        if (blockingIssueId <= 0L) {
            throw new IllegalArgumentException("blockingIssueId must be positive");
        }
        if (blockedIssueId <= 0L) {
            throw new IllegalArgumentException("blockedIssueId must be positive");
        }
    }
}
