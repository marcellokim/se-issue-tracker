package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Priority;

record IssueRegisterRequest(String title, String description, Priority priority) {

    IssueRegisterRequest {
        title = normalize(title);
        description = normalize(description);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
