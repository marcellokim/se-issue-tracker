package com.github.marcellokim.issuetracker.domain;

import java.util.Objects;

public final class AssignmentCandidate {

    private final User user;
    private final int completedIssueCount;
    private final String reason;

    public static AssignmentCandidate create(User user, int completedIssueCount) {
        return create(
                user,
                completedIssueCount,
                completedIssueCount > 0
                        ? "Resolved/closed issue history count: " + completedIssueCount
                        : "Fallback active project member with no resolved/closed history yet"
        );
    }

    public static AssignmentCandidate create(User user, int completedIssueCount, String reason) {
        return new AssignmentCandidate(user, completedIssueCount, reason);
    }

    private AssignmentCandidate(User user, int completedIssueCount, String reason) {
        this.user = Objects.requireNonNull(user, "user");
        this.completedIssueCount = completedIssueCount;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public User user() {
        return user;
    }

    public int completedIssueCount() {
        return completedIssueCount;
    }

    public String reason() {
        return reason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentCandidate that)) {
            return false;
        }
        return completedIssueCount == that.completedIssueCount
                && Objects.equals(user, that.user)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, completedIssueCount, reason);
    }

    @Override
    public String toString() {
        return "AssignmentCandidate[user=" + user
                + ", completedIssueCount=" + completedIssueCount
                + ", reason=" + reason + "]";
    }
}
