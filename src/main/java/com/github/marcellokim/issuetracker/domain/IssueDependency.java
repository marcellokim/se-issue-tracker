package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueDependency {

    private final String dependencyId;
    private final LocalDateTime discoveredDate;
    private final Issue blockingIssue;
    private final Issue blockedIssue;

    private IssueDependency(String dependencyId, Issue blockingIssue, Issue blockedIssue, LocalDateTime discoveredDate) {
        this.dependencyId = requireText(dependencyId, "dependencyId");
        this.blockingIssue = Objects.requireNonNull(blockingIssue, "blockingIssue must not be null");
        this.blockedIssue = Objects.requireNonNull(blockedIssue, "blockedIssue must not be null");
        if (Objects.equals(blockingIssue.getIssueId(), blockedIssue.getIssueId())) {
            throw new IllegalArgumentException("An issue cannot depend on itself");
        }
        this.discoveredDate = Objects.requireNonNull(discoveredDate, "discoveredDate must not be null");
    }

    public static IssueDependency create(
            String dependencyId,
            Issue blockingIssue,
            Issue blockedIssue,
            LocalDateTime discoveredDate
    ) {
        return new IssueDependency(dependencyId, blockingIssue, blockedIssue, discoveredDate);
    }

    public String getDependencyId() {
        return dependencyId;
    }

    public LocalDateTime getDiscoveredDate() {
        return discoveredDate;
    }

    public Issue getBlockingIssue() {
        return blockingIssue;
    }

    public Issue getBlockedIssue() {
        return blockedIssue;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
