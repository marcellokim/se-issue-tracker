package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.List;

interface IssueListView {

    void showProject(ProjectResult project);

    void showIssues(List<IssueSummary> issues);

    void setRegisterEnabled(boolean enabled);

    void showMessage(String message, boolean error);
}
