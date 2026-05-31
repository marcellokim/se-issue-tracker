package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class IssueListScreen extends VBox {

    private final IssueController issueController;
    private final long projectId;
    private final ListView<IssueSummary> issueList = new ListView<>();
    private final TextField searchField = new TextField();
    private final Label messageLabel = new Label();
    private final Label projectInfoLabel = new Label();
    private Consumer<IssueSummary> onIssueSelected;
    private Runnable onBack;
    private Runnable onDeletedIssueManage;
    private Runnable onStatistics;

    IssueListScreen(IssueController issueController, ProjectController projectController, long projectId){
        this.issueController = issueController;
        this.projectId = projectId;
        setPadding(new Insets(20));
        setSpacing(12);

        Button backButton = new Button("← Projects");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        projectInfoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        loadProjectInfo(projectController);

        HBox header = new HBox(backButton, projectInfoLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        searchField.setPromptText("Search...");
        searchField.setMaxWidth(300);
        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> searchIssues());

        Button registerButton = new Button("+ Register Issue");
        registerButton.setOnAction(event -> messageLabel.setText("Issue registration will be implemented in #141"));
        if (!issueController.canRegisterIssue(projectId)){
            registerButton.setDisable(true);
        }

        Button deletedButton = new Button("Deleted Issues");
        deletedButton.setOnAction(event -> { if (onDeletedIssueManage != null) onDeletedIssueManage.run(); });

        Button statsButton = new Button("Statistics");
        statsButton.setOnAction(event -> { if (onStatistics != null) onStatistics.run(); });

        HBox toolbar = new HBox(10, searchField, searchButton, registerButton, deletedButton, statsButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        issueList.setCellFactory(list -> new IssueCell());
        issueList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                IssueSummary selected = issueList.getSelectionModel().getSelectedItem();
                if (selected != null && onIssueSelected != null) onIssueSelected.accept(selected);
            }
        });
        VBox.setVgrow(issueList, Priority.ALWAYS);

        messageLabel.setStyle("-fx-text-fill: #666;");

        getChildren().addAll(header, toolbar, issueList, messageLabel);
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
            messageLabel.setText(issues.size() + " issues");
        } catch (Exception exception){
            messageLabel.setText(exception.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void searchIssues(){
        try{
            String keyword = searchField.getText().isBlank() ? null : searchField.getText();
            List<IssueSummary> issues = issueController.searchIssues(projectId, keyword, null, null);
            issueList.getItems().setAll(issues);
            messageLabel.setText(issues.size() + " issues");
        } catch (Exception exception){
            messageLabel.setText(exception.getMessage());
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
