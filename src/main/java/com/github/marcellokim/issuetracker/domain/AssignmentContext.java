package com.github.marcellokim.issuetracker.domain;

import java.util.Objects;

public final class AssignmentContext {

    private final long issueId;
    private final IssueStatus status;
    private final String assigneeId;
    private final String verifierId;

    public static AssignmentContext create(
            long issueId,
            IssueStatus status,
            String assigneeId,
            String verifierId
    ) {
        return new AssignmentContext(issueId, status, assigneeId, verifierId);
    }

    private AssignmentContext(long issueId, IssueStatus status, String assigneeId, String verifierId) {
        this.issueId = issueId;
        this.status = status;
        this.assigneeId = assigneeId;
        this.verifierId = verifierId;
    }

    public long issueId() {
        return issueId;
    }

    public IssueStatus status() {
        return status;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String verifierId() {
        return verifierId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AssignmentContext that)) {
            return false;
        }
        return issueId == that.issueId
                && status == that.status
                && Objects.equals(assigneeId, that.assigneeId)
                && Objects.equals(verifierId, that.verifierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issueId, status, assigneeId, verifierId);
    }

    @Override
    public String toString() {
        return "AssignmentContext[issueId=" + issueId
                + ", status=" + status
                + ", assigneeId=" + assigneeId
                + ", verifierId=" + verifierId + "]";
    }
}
