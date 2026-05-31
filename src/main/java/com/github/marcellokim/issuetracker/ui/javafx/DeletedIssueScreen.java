package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class DeletedIssueScreen extends VBox {

    private final DeletedIssueController deletedIssueController;
    private final long projectId;
    private final ListView<IssueSummary> issueList = new ListView<>();
    private final Label countLabel = new Label();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Runnable onBack;

    DeletedIssueScreen(DeletedIssueController deletedIssueController, long projectId){
        this.deletedIssueController = deletedIssueController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Deleted Issue Management");
        countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        issueList.setCellFactory(list -> new DeletedIssueCell());
        VBox.setVgrow(issueList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.headerWithGrow(backButton, titleLabel, countLabel),
                issueList, messageLabel);
        loadDeletedIssues();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadDeletedIssues(){
        try{
            int limit = deletedIssueController.getMaxRetentionLimit();
            List<IssueSummary> issues = deletedIssueController.viewDeletedIssues(projectId);
            issueList.getItems().setAll(issues);
            countLabel.setText(String.format("(%d/%d)", issues.size(), limit));
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static class DeletedIssueCell extends ListCell<IssueSummary> {
        @Override
        protected void updateItem(IssueSummary issue, boolean empty){
            super.updateItem(issue, empty);
            if (empty || issue == null){ setText(null); setGraphic(null); return; }
            setText(String.format("[%s] %s | %s", issue.issueId(), issue.title(), issue.status()));
        }
    }
}
