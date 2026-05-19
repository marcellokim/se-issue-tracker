package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;

public record IssueSearchCriteria(
        Long projectId,
        IssueStatus status,
        Priority priority,
        String reporterId,
        String assigneeId,
        String verifierId,
        String keyword,
        LocalDateTime reportedFrom,
        LocalDateTime reportedTo,
        boolean includeDeleted) {

    public static IssueSearchCriteria all() {
        return new IssueSearchCriteria(null, null, null, null, null, null, null, null, null, false);
    }
}
