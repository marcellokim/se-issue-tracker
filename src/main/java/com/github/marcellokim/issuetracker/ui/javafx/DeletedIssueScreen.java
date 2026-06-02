package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
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

        Button restoreButton = new Button("Restore Selected");
        restoreButton.setDisable(true);
        Button purgeButton = new Button("Permanently Delete");
        purgeButton.setDisable(true);
        issueList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            restoreButton.setDisable(val == null);
            purgeButton.setDisable(val == null);
        });
        restoreButton.setOnAction(event -> {
            IssueSummary selected = issueList.getSelectionModel().getSelectedItem();
            if (selected != null) handleRestore(selected);
        });
        purgeButton.setOnAction(event -> {
            IssueSummary selected = issueList.getSelectionModel().getSelectedItem();
            if (selected != null) handlePurge(selected);
        });
        HBox toolbar = new HBox(8, restoreButton, purgeButton);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel, countLabel),
                toolbar, issueList, messageLabel);
        loadDeletedIssues();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void handlePurge(IssueSummary issue){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Permanently Delete");
        dialog.setHeaderText("Permanently delete [" + ScreenComponents.shortIssueId(issue.issueId()) + "] " + issue.title() + "?");
        dialog.getDialogPane().setContent(new Label("This action cannot be undone."));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Delete");
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK){
                try{
                    deletedIssueController.purgeDeletedIssue(issue.id());
                    loadDeletedIssues();
                    ScreenComponents.showInfo(messageLabel, "Issue permanently deleted: " + ScreenComponents.shortIssueId(issue.issueId()));
                } catch (Exception exception){
                    ScreenComponents.showError(messageLabel, exception);
                }
            }
        });
    }

    private void handleRestore(IssueSummary issue){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Restore Issue");
        dialog.setHeaderText("Restore: [" + ScreenComponents.shortIssueId(issue.issueId()) + "] " + issue.title());
        TextArea textArea = new TextArea();
        textArea.setPromptText("Reason for restore...");
        textArea.setPrefRowCount(3);
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        textArea.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? textArea.getText().trim() : null);
        dialog.showAndWait().ifPresent(comment -> {
            try{
                deletedIssueController.restoreIssue(issue.id(), comment);
                loadDeletedIssues();
                ScreenComponents.showInfo(messageLabel, "Issue restored: " + ScreenComponents.shortIssueId(issue.issueId()));
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

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
            setText(String.format("[%s] %s | %s", ScreenComponents.shortIssueId(issue.issueId()), issue.title(), issue.status()));
        }
    }
}
