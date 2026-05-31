package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class DeletedIssueScreen extends VBox {

    private final DeletedIssueController deletedIssueController;
    private final long projectId;
    private final ListView<IssueSummary> issueList = new ListView<>();
    private final Label countLabel = new Label();
    private final Label messageLabel = new Label();
    private Runnable onBack;

    DeletedIssueScreen(DeletedIssueController deletedIssueController, long projectId){
        this.deletedIssueController = deletedIssueController;
        this.projectId = projectId;
        setPadding(new Insets(20));
        setSpacing(12);

        Button backButton = new Button("← Issues");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        Label titleLabel = new Label("Deleted Issue Management");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        countLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        HBox header = new HBox(backButton, titleLabel, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        issueList.setCellFactory(list -> new DeletedIssueCell());
        VBox.setVgrow(issueList, Priority.ALWAYS);

        messageLabel.setStyle("-fx-text-fill: #666;");

        getChildren().addAll(header, issueList, messageLabel);
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
            messageLabel.setText(exception.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private static class DeletedIssueCell extends ListCell<IssueSummary> {
        @Override
        protected void updateItem(IssueSummary issue, boolean empty){
            super.updateItem(issue, empty);
            if (empty || issue == null){ setText(null); return; }
            setText(String.format("[%s] %s | %s", issue.issueId(), issue.title(), issue.status()));
        }
    }
}
