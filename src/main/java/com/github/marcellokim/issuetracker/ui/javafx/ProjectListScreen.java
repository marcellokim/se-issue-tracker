package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectListScreen extends VBox {

    private final DashboardController dashboardController;
    private final ListView<DashboardProjectSummary> projectList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Consumer<DashboardProjectSummary> onProjectSelected;
    private Runnable onLogout;

    ProjectListScreen(DashboardController dashboardController){
        this.dashboardController = dashboardController;
        ScreenComponents.applyScreenDefaults(this);

        Label titleLabel = ScreenComponents.titleLabel("Projects");
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> { if (onLogout != null) onLogout.run(); });

        projectList.setCellFactory(list -> new ProjectCell());
        projectList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                DashboardProjectSummary selected = projectList.getSelectionModel().getSelectedItem();
                if (selected != null && onProjectSelected != null) onProjectSelected.accept(selected);
            }
        });
        VBox.setVgrow(projectList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.headerWithGrow(titleLabel, logoutButton),
                projectList, messageLabel);
        loadProjects();
    }

    void setOnProjectSelected(Consumer<DashboardProjectSummary> action){ this.onProjectSelected = action; }
    void setOnLogout(Runnable action){ this.onLogout = action; }

    private void loadProjects(){
        try{
            List<DashboardProjectSummary> projects = dashboardController.viewProjects();
            projectList.getItems().setAll(projects);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static class ProjectCell extends ListCell<DashboardProjectSummary> {
        @Override
        protected void updateItem(DashboardProjectSummary project, boolean empty){
            super.updateItem(project, empty);
            if (empty || project == null){ setText(null); setGraphic(null); return; }
            VBox box = new VBox(4);
            Label name = new Label(project.projectName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            Label info = new Label(String.format("Members: %d | Issues: %d | PL %d / DEV %d / TESTER %d",
                    project.memberCount(), project.visibleIssueCount(),
                    project.projectLeaderCount(), project.developerCount(), project.testerCount()));
            info.setStyle("-fx-text-fill: #666;");
            box.getChildren().addAll(name, info);
            setGraphic(box);
        }
    }
}
