package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectManageScreen extends VBox {

    private final ListView<DashboardProjectSummary> projectList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Consumer<DashboardProjectSummary> onProjectSelected;
    private Runnable onBack;

    ProjectManageScreen(DashboardController dashboardController){
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Project Management");
        ScreenComponents.growInHeader(titleLabel);
        Button createButton = new Button("+ Create Project");
        createButton.setOnAction(event -> ScreenComponents.showInfo(messageLabel, "Project creation will be implemented in #195"));

        projectList.setCellFactory(list -> new CompactProjectCell());
        ScreenComponents.setupListDoubleClick(projectList, p -> { if (onProjectSelected != null) onProjectSelected.accept(p); });
        VBox.setVgrow(projectList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel, createButton),
                projectList, messageLabel);
        ScreenComponents.loadList(projectList, messageLabel, dashboardController::viewProjects);
    }

    void setOnProjectSelected(Consumer<DashboardProjectSummary> action){ this.onProjectSelected = action; }
    void setOnBack(Runnable action){ this.onBack = action; }

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
