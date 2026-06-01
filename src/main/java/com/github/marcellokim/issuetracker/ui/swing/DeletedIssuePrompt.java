package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.awt.Component;
import java.util.Optional;

interface DeletedIssuePrompt {

    Optional<String> requestRestoreComment(Component parent, IssueSummary issue);

    boolean confirmPurge(Component parent, IssueSummary issue);
}
