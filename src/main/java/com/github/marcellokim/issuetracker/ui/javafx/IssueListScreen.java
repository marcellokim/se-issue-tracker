package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class IssueListScreen extends VBox {

    private final IssueController issueController;
    private final long projectId;
    private final ListView<IssueSummary> issueList = new ListView<>();
    private final TextField searchField = new TextField();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private final Label projectInfoLabel = new Label();
    private Consumer<IssueSummary> onIssueSelected;
    private Runnable onBack;
    private Runnable onDeletedIssueManage;
    private Runnable onStatistics;

    IssueListScreen(IssueController issueController, ProjectController projectController, long projectId, boolean isPl){
        this.issueController = issueController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Projects", () -> { if (onBack != null) onBack.run(); });
        projectInfoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        ScreenComponents.growInHeader(projectInfoLabel);
        loadProjectInfo(projectController);

        searchField.setPromptText("Search...");
        searchField.setMaxWidth(300);
        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> searchIssues());

        Button registerButton = new Button("+ Register Issue");
        registerButton.setOnAction(event -> showRegisterDialog());
        try{
            if (!issueController.canRegisterIssue(projectId)) registerButton.setDisable(true);
        } catch (Exception exception){
            registerButton.setDisable(true);
            ScreenComponents.showError(messageLabel, exception);
        }

        HBox toolbar = new HBox(10, searchField, searchButton, registerButton);

        if (isPl){
            Button deletedButton = new Button("Deleted Issues");
            deletedButton.setOnAction(event -> { if (onDeletedIssueManage != null) onDeletedIssueManage.run(); });
            toolbar.getChildren().add(deletedButton);
        }

        Button statsButton = new Button("Statistics");
        statsButton.setOnAction(event -> { if (onStatistics != null) onStatistics.run(); });
        toolbar.getChildren().add(statsButton);

        issueList.setCellFactory(list -> new IssueCell());
        ScreenComponents.setupListDoubleClick(issueList, i -> { if (onIssueSelected != null) onIssueSelected.accept(i); });
        VBox.setVgrow(issueList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, projectInfoLabel),
                toolbar, issueList, messageLabel);
        loadIssues();
    }

    void setOnIssueSelected(Consumer<IssueSummary> action){ this.onIssueSelected = action; }
    void setOnBack(Runnable action){ this.onBack = action; }
    void setOnDeletedIssueManage(Runnable action){ this.onDeletedIssueManage = action; }
    void setOnStatistics(Runnable action){ this.onStatistics = action; }

    private void loadProjectInfo(ProjectController projectController){
        try{
            ProjectResult project = projectController.viewProjectNonAdminDetail(projectId);
            projectInfoLabel.setText(project.name());
        } catch (Exception exception){
            projectInfoLabel.setText("Project " + projectId);
        }
    }

    private void loadIssues(){
        try{
            List<IssueSummary> issues = issueController.viewRelatedProjectIssues(projectId);
            issueList.getItems().setAll(issues);
            ScreenComponents.showInfo(messageLabel, issues.size() + " issues");
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void showRegisterDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register Issue");
        TextField titleField = new TextField();
        titleField.setPromptText("Issue title");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(4);
        ComboBox<com.github.marcellokim.issuetracker.domain.Priority> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(com.github.marcellokim.issuetracker.domain.Priority.values());
        priorityBox.setValue(com.github.marcellokim.issuetracker.domain.Priority.MAJOR);
        VBox content = new VBox(8,
                new Label("Title:"), titleField,
                new Label("Description:"), descField,
                new Label("Priority:"), priorityBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        Runnable validateForm = () -> okButton.setDisable(
                titleField.getText() == null || titleField.getText().isBlank()
                || descField.getText() == null || descField.getText().isBlank());
        titleField.textProperty().addListener((obs, old, val) -> validateForm.run());
        descField.textProperty().addListener((obs, old, val) -> validateForm.run());
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                issueController.registerIssue(projectId,
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        priorityBox.getValue());
                loadIssues();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private void searchIssues(){
        try{
            String keyword = searchField.getText().isBlank() ? null : searchField.getText();
            List<IssueSummary> issues = issueController.searchIssues(projectId, keyword, null, null);
            issueList.getItems().setAll(issues);
            ScreenComponents.showInfo(messageLabel, issues.size() + " issues");
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static class IssueCell extends ListCell<IssueSummary> {
        @Override
        protected void updateItem(IssueSummary issue, boolean empty){
            super.updateItem(issue, empty);
            if (empty || issue == null){ setText(null); setGraphic(null); return; }
            VBox box = new VBox(2);
            Label title = new Label(String.format("[%s] %s", issue.issueId(), issue.title()));
            title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            Label info = new Label(String.format("%s | %s | reporter: %s",
                    issue.status(), issue.priority(), issue.reporterId()));
            info.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            box.getChildren().addAll(title, info);
            setGraphic(box);
        }
    }
}
