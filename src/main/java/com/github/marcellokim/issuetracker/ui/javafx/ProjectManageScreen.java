package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectManageScreen extends VBox {

    private final DashboardController dashboardController;
    private final ListView<DashboardProjectSummary> projectList = new ListView<>();
    private final Label messageLabel = new Label();
    private Consumer<DashboardProjectSummary> onProjectSelected;
    private Runnable onBack;

    ProjectManageScreen(DashboardController dashboardController){
        this.dashboardController = dashboardController;
        setPadding(new Insets(20));
        setSpacing(12);

        Label titleLabel = new Label("Project Management");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button backButton = new Button("← Back");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        Button createButton = new Button("+ Create Project");
        createButton.setOnAction(event -> messageLabel.setText("Project creation will be implemented in #195"));

        HBox header = new HBox(backButton, titleLabel, createButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        projectList.setCellFactory(list -> new ProjectCell());
        projectList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                DashboardProjectSummary selected = projectList.getSelectionModel().getSelectedItem();
                if (selected != null && onProjectSelected != null) onProjectSelected.accept(selected);
            }
        });
        VBox.setVgrow(projectList, Priority.ALWAYS);

        messageLabel.setStyle("-fx-text-fill: #666;");

        getChildren().addAll(header, projectList, messageLabel);
        loadProjects();
    }

    void setOnProjectSelected(Consumer<DashboardProjectSummary> action){ this.onProjectSelected = action; }
    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadProjects(){
        try{
            List<DashboardProjectSummary> projects = dashboardController.viewProjects();
            projectList.getItems().setAll(projects);
        } catch (Exception exception){
            messageLabel.setText(exception.getMessage());
        }
    }

    private static class ProjectCell extends ListCell<DashboardProjectSummary> {
        @Override
        protected void updateItem(DashboardProjectSummary project, boolean empty){
            super.updateItem(project, empty);
            if (empty || project == null){ setText(null); return; }
            setText(String.format("%s | Members: %d | Issues: %d",
                    project.projectName(), project.memberCount(), project.visibleIssueCount()));
        }
    }
}
