package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import java.util.Objects;

record IssueActionSupport(
        IssueStatusChangeSupport statusChange,
        AssignmentController assignmentController,
        IssueAssignmentPrompt assignmentPrompt,
        IssueCommentPrompt commentPrompt) {

    IssueActionSupport {
        Objects.requireNonNull(statusChange, "statusChange");
        Objects.requireNonNull(assignmentPrompt, "assignmentPrompt");
        Objects.requireNonNull(commentPrompt, "commentPrompt");
    }

    static IssueActionSupport disabled() {
        return new IssueActionSupport(
                IssueStatusChangeSupport.disabled(),
                null,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt);
    }

    static IssueActionSupport dialogs(
            IssueStateController issueStateController,
            AssignmentController assignmentController) {
        return new IssueActionSupport(
                IssueStatusChangeSupport.dialog(issueStateController),
                assignmentController,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt);
    }
}
