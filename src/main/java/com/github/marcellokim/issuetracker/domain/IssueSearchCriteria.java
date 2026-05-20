package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class IssueSearchCriteria {

    private final Long projectId;
    private final IssueStatus status;
    private final Priority priority;
    private final String reporterId;
    private final String assigneeId;
    private final String verifierId;
    private final String keyword;
    private final LocalDateTime reportedFrom;
    private final LocalDateTime reportedTo;
    private final boolean includeDeleted;

    public static IssueSearchCriteria create(
            Long projectId,
            IssueStatus status,
            Priority priority,
            String reporterId,
            String assigneeId,
            String verifierId,
            String keyword,
            LocalDateTime reportedFrom,
            LocalDateTime reportedTo,
            boolean includeDeleted
    ) {
        return new IssueSearchCriteria(
                projectId,
                status,
                priority,
                reporterId,
                assigneeId,
                verifierId,
                keyword,
                reportedFrom,
                reportedTo,
                includeDeleted
        );
    }

    public static IssueSearchCriteria all() {
        return create(null, null, null, null, null, null, null, null, null, false);
    }

    private IssueSearchCriteria(
            Long projectId,
            IssueStatus status,
            Priority priority,
            String reporterId,
            String assigneeId,
            String verifierId,
            String keyword,
            LocalDateTime reportedFrom,
            LocalDateTime reportedTo,
            boolean includeDeleted
    ) {
        this.projectId = projectId;
        this.status = status;
        this.priority = priority;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
        this.verifierId = verifierId;
        this.keyword = keyword;
        this.reportedFrom = reportedFrom;
        this.reportedTo = reportedTo;
        this.includeDeleted = includeDeleted;
    }

    public Long projectId() {
        return projectId;
    }

    public IssueStatus status() {
        return status;
    }

    public Priority priority() {
        return priority;
    }

    public String reporterId() {
        return reporterId;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String verifierId() {
        return verifierId;
    }

    public String keyword() {
        return keyword;
    }

    public LocalDateTime reportedFrom() {
        return reportedFrom;
    }

    public LocalDateTime reportedTo() {
        return reportedTo;
    }

    public boolean includeDeleted() {
        return includeDeleted;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IssueSearchCriteria that)) {
            return false;
        }
        return includeDeleted == that.includeDeleted
                && Objects.equals(projectId, that.projectId)
                && status == that.status
                && priority == that.priority
                && Objects.equals(reporterId, that.reporterId)
                && Objects.equals(assigneeId, that.assigneeId)
                && Objects.equals(verifierId, that.verifierId)
                && Objects.equals(keyword, that.keyword)
                && Objects.equals(reportedFrom, that.reportedFrom)
                && Objects.equals(reportedTo, that.reportedTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                projectId,
                status,
                priority,
                reporterId,
                assigneeId,
                verifierId,
                keyword,
                reportedFrom,
                reportedTo,
                includeDeleted
        );
    }

    @Override
    public String toString() {
        return "IssueSearchCriteria[projectId=" + projectId
                + ", status=" + status
                + ", priority=" + priority
                + ", reporterId=" + reporterId
                + ", assigneeId=" + assigneeId
                + ", verifierId=" + verifierId
                + ", keyword=" + keyword
                + ", reportedFrom=" + reportedFrom
                + ", reportedTo=" + reportedTo
                + ", includeDeleted=" + includeDeleted + "]";
    }
}
