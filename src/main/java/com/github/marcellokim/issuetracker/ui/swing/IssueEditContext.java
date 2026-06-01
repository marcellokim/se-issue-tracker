package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Priority;
import java.util.Objects;

record IssueEditContext(String title, String description, Priority priority) {

    IssueEditContext {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(priority, "priority");
    }
}
