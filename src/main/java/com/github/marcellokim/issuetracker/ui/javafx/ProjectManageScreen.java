package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectManageScreen extends VBox {

    private final DashboardController dashboardController;
    private final ProjectController projectController;
    private final ListView<DashboardProjectSummary> projectList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Consumer<DashboardProjectSummary> onProjectSelected;
    private Runnable onBack;

    ProjectManageScreen(DashboardController dashboardController, ProjectController projectController){
        this.dashboardController = dashboardController;
        this.projectController = projectController;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Project Management");
        ScreenComponents.growInHeader(titleLabel);
        Button createButton = new Button("+ Create Project");
        createButton.setOnAction(event -> showCreateDialog());

        projectList.setCellFactory(list -> new CompactProjectCell());
        ScreenComponents.setupListDoubleClick(projectList, p -> { if (onProjectSelected != null) onProjectSelected.accept(p); });
        VBox.setVgrow(projectList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel, createButton),
                projectList, messageLabel);
        loadProjects();
    }

    void setOnProjectSelected(Consumer<DashboardProjectSummary> action){ this.onProjectSelected = action; }
    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadProjects(){
        ScreenComponents.loadList(projectList, messageLabel, dashboardController::viewProjects);
    }

    private void showCreateDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Project");
        TextField nameField = new TextField();
        nameField.setPromptText("Project name");
        TextField descField = new TextField();
        descField.setPromptText("Description");
        VBox content = new VBox(8,
                new Label("Name:"), nameField,
                new Label("Description:"), descField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        Runnable validate = () -> okButton.setDisable(
                nameField.getText() == null || nameField.getText().isBlank()
                || descField.getText() == null || descField.getText().isBlank());
        nameField.textProperty().addListener((obs, old, val) -> validate.run());
        descField.textProperty().addListener((obs, old, val) -> validate.run());
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                projectController.createProject(
                        nameField.getText().trim(),
                        descField.getText().trim());
                loadProjects();
                ScreenComponents.showInfo(messageLabel, "Project created");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private static class CompactProjectCell extends ListCell<DashboardProjectSummary> {
        @Override
        protected void updateItem(DashboardProjectSummary project, boolean empty){
            super.updateItem(project, empty);
            if (empty || project == null){ setText(null); setGraphic(null); return; }
            setText(String.format("%s | Members: %d | Issues: %d",
                    project.projectName(), project.memberCount(), project.visibleIssueCount()));
        }
    }
}
