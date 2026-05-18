package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueDependency {

    private final long id;
    private final long blockingIssueId;
    private final long blockedIssueId;
    private final String dependencyId;
    private final Issue blockingIssue;
    private final Issue blockedIssue;
    private final LocalDateTime discoveredDate;

    public IssueDependency(long id, long blockingIssueId, long blockedIssueId, LocalDateTime discoveredDate) {
        this.id = id;
        this.blockingIssueId = blockingIssueId;
        this.blockedIssueId = blockedIssueId;
        this.dependencyId = Long.toString(id);
        this.blockingIssue = null;
        this.blockedIssue = null;
        this.discoveredDate = discoveredDate;
    }

    private IssueDependency(String dependencyId, Issue blockingIssue, Issue blockedIssue, LocalDateTime discoveredDate) {
        this.id = 0L;
        this.blockingIssue = Objects.requireNonNull(blockingIssue, "blockingIssue must not be null");
        this.blockedIssue = Objects.requireNonNull(blockedIssue, "blockedIssue must not be null");
        this.blockingIssueId = blockingIssue.id();
        this.blockedIssueId = blockedIssue.id();
        this.dependencyId = requireText(dependencyId, "dependencyId");
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

    public long id() {
        return id;
    }

    public long blockingIssueId() {
        return blockingIssueId;
    }

    public long blockedIssueId() {
        return blockedIssueId;
    }

    public LocalDateTime discoveredDate() {
        return discoveredDate;
    }

    public String getDependencyId() {
        return dependencyId;
    }

    public Issue getBlockingIssue() {
        return blockingIssue;
    }

    public Issue getBlockedIssue() {
        return blockedIssue;
    }

    public LocalDateTime getDiscoveredDate() {
        return discoveredDate;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
