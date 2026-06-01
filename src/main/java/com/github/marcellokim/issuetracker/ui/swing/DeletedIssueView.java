package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;

interface DeletedIssueView {

    void showDeletedIssues(int maxRetentionLimit, List<IssueSummary> issues);

    void showMessage(String message, boolean error);
}
