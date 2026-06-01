package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;

record IssueSearchRequest(String keyword, IssueStatus status, Priority priority) {

    IssueSearchRequest {
        keyword = normalize(keyword);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
