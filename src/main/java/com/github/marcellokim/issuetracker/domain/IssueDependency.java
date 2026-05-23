package com.github.marcellokim.issuetracker.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

public final class IssueDependency {

    private final long id;
    private final long blockingIssueId;
    private final long blockedIssueId;
    private final String dependencyId;
    private final Issue blockingIssue;
    private final Issue blockedIssue;
    private final LocalDateTime discoveredDate;

    public static IssueDependency fromPersistence(
            long id,
            long blockingIssueId,
            long blockedIssueId,
            LocalDateTime discoveredDate) {
        return fromPersistence(
                id,
                dependencyIdFor(blockingIssueId, blockedIssueId),
                blockingIssueId,
                blockedIssueId,
                discoveredDate);
    }

    public static IssueDependency fromPersistence(
            long id,
            String dependencyId,
            long blockingIssueId,
            long blockedIssueId,
            LocalDateTime discoveredDate) {
        return new IssueDependency(id, dependencyId, blockingIssueId, blockedIssueId, discoveredDate);
    }

    private IssueDependency(
            long id,
            String dependencyId,
            long blockingIssueId,
            long blockedIssueId,
            LocalDateTime discoveredDate) {
        this.id = id;
        this.blockingIssueId = blockingIssueId;
        this.blockedIssueId = blockedIssueId;
        this.dependencyId = requireText(dependencyId, "dependencyId");
        this.blockingIssue = null;
        this.blockedIssue = null;
        this.discoveredDate = discoveredDate;
    }

    private IssueDependency(String dependencyId, Issue blockingIssue, Issue blockedIssue,
            LocalDateTime discoveredDate) {
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
            LocalDateTime discoveredDate) {
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

    public static String dependencyIdFor(long blockingIssueId, long blockedIssueId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = blockingIssueId + ":" + blockedIssueId;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable.", exception);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueDependency other)) {
            return false;
        }
        return Objects.equals(dependencyId, other.dependencyId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dependencyId);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
