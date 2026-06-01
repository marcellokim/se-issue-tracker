package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import java.util.Objects;

record IssueActionSupport(
        IssueStatusChangeSupport statusChange,
        AssignmentController assignmentController,
        IssueAssignmentPrompt assignmentPrompt,
        IssueCommentPrompt commentPrompt,
        IssueDependencyPrompt dependencyPrompt,
        DeletedIssueController deletedIssueController,
        IssueEditPrompt editPrompt) {

    IssueActionSupport {
        Objects.requireNonNull(statusChange, "statusChange");
        Objects.requireNonNull(assignmentPrompt, "assignmentPrompt");
        Objects.requireNonNull(commentPrompt, "commentPrompt");
        Objects.requireNonNull(dependencyPrompt, "dependencyPrompt");
        Objects.requireNonNull(editPrompt, "editPrompt");
    }

    static IssueActionSupport disabled() {
        return new IssueActionSupport(
                IssueStatusChangeSupport.disabled(),
                null,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                IssueDependencyDialogs::prompt,
                null,
                IssueEditDialogs::prompt);
    }

    static IssueActionSupport dialogs(
            IssueStateController issueStateController,
            AssignmentController assignmentController,
            DeletedIssueController deletedIssueController) {
        return new IssueActionSupport(
                IssueStatusChangeSupport.dialog(issueStateController),
                assignmentController,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                IssueDependencyDialogs::prompt,
                deletedIssueController,
                IssueEditDialogs::prompt);
    }
}
