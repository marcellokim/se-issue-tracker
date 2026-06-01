package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import java.util.Objects;

final class DeletedIssuePresenter {

    private final DeletedIssueController deletedIssueController;
    private final DeletedIssueView view;

    DeletedIssuePresenter(DeletedIssueController deletedIssueController, DeletedIssueView view) {
        this.deletedIssueController = Objects.requireNonNull(deletedIssueController, "deletedIssueController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadDeletedIssues(long projectId) {
        try {
            DeletedIssueSnapshot snapshot = fetchDeletedIssues(projectId);
            view.showDeletedIssues(snapshot.maxRetentionLimit(), snapshot.issues());
            view.showMessage(summaryMessage(snapshot), false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void restoreIssue(long projectId, long issueId, String comment) {
        try {
            deletedIssueController.restoreIssue(issueId, comment);
            refreshDeletedIssues(projectId);
            view.showMessage("Issue restored.", false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void purgeDeletedIssue(long projectId, long issueId) {
        try {
            deletedIssueController.purgeDeletedIssue(issueId);
            refreshDeletedIssues(projectId);
            view.showMessage("Deleted issue purged.", false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private void refreshDeletedIssues(long projectId) {
        DeletedIssueSnapshot snapshot = fetchDeletedIssues(projectId);
        view.showDeletedIssues(snapshot.maxRetentionLimit(), snapshot.issues());
    }

    private DeletedIssueSnapshot fetchDeletedIssues(long projectId) {
        List<IssueSummary> issues = deletedIssueController.viewDeletedIssues(projectId);
        int maxRetentionLimit = deletedIssueController.getMaxRetentionLimit();
        return new DeletedIssueSnapshot(maxRetentionLimit, issues);
    }

    private static String summaryMessage(DeletedIssueSnapshot snapshot) {
        return "Deleted issues " + snapshot.issues().size() + "/" + snapshot.maxRetentionLimit();
    }

    private record DeletedIssueSnapshot(int maxRetentionLimit, List<IssueSummary> issues) {

        private DeletedIssueSnapshot {
            issues = List.copyOf(issues);
        }
    }
}
