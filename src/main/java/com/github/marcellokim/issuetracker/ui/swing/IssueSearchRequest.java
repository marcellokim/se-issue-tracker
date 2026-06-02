package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import java.time.LocalDateTime;

record IssueSearchRequest(
        String keyword,
        IssueStatus status,
        Priority priority,
        String reporterId,
        String assigneeId,
        String verifierId,
        LocalDateTime reportedFrom,
        LocalDateTime reportedTo) {

    IssueSearchRequest {
        keyword = normalize(keyword);
        reporterId = normalize(reporterId);
        assigneeId = normalize(assigneeId);
        verifierId = normalize(verifierId);
    }

    IssueSearchRequest(String keyword, IssueStatus status, Priority priority) {
        this(keyword, status, priority, null, null, null, null, null);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
