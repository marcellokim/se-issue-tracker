package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectDetailScreen extends VBox {

    private final ProjectController projectController;
    private final long projectId;
    private final ListView<ProjectMemberResult> memberList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Runnable onBack;

    ProjectDetailScreen(ProjectController projectController, long projectId){
        this.projectController = projectController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Project Detail");

        memberList.setCellFactory(list -> new MemberCell());
        VBox.setVgrow(memberList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                messageLabel, new Label("Members:"), memberList);
        loadDetail();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadDetail(){
        try{
            ProjectAdminDetail detail = projectController.viewProjectAdminDetail(projectId);
            ScreenComponents.showInfo(messageLabel, String.format("Project: %s | %s",
                    detail.project().name(), detail.project().description()));
            memberList.getItems().setAll(detail.participants());
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static class MemberCell extends ListCell<ProjectMemberResult> {
        @Override
        protected void updateItem(ProjectMemberResult member, boolean empty){
            super.updateItem(member, empty);
            if (empty || member == null){ setText(null); setGraphic(null); return; }
            setText(String.format("%s (%s) | %s | %s",
                    member.userId(), member.userName(), member.role(), member.active() ? "active" : "inactive"));
        }
    }
}
