package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.IssueStateController;
import java.util.Objects;

record IssueStatusChangeSupport(
        IssueStateController issueStateController,
    IssueStatusChangePrompt prompt) {

    IssueStatusChangeSupport {
        Objects.requireNonNull(prompt, "prompt");
    }

    static IssueStatusChangeSupport disabled() {
        return new IssueStatusChangeSupport(null, IssueStatusChangeDialogs::prompt);
    }

    static IssueStatusChangeSupport dialog(IssueStateController issueStateController) {
        return new IssueStatusChangeSupport(issueStateController, IssueStatusChangeDialogs::prompt);
    }
}
