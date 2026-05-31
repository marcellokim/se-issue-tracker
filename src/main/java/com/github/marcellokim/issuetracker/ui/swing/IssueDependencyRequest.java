package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Objects;

record IssueDependencyRequest(IssueDependencyMode mode, long blockingIssueId, long blockedIssueId) {

    IssueDependencyRequest {
        Objects.requireNonNull(mode, "mode");
        if (blockingIssueId <= 0L) {
            throw new IllegalArgumentException("blockingIssueId must be positive");
        }
        if (blockedIssueId <= 0L) {
            throw new IllegalArgumentException("blockedIssueId must be positive");
        }
    }

    static IssueDependencyRequest add(long blockingIssueId, long blockedIssueId) {
        return new IssueDependencyRequest(IssueDependencyMode.ADD, blockingIssueId, blockedIssueId);
    }

    static IssueDependencyRequest remove(long blockingIssueId, long blockedIssueId) {
        return new IssueDependencyRequest(IssueDependencyMode.REMOVE, blockingIssueId, blockedIssueId);
    }
}
