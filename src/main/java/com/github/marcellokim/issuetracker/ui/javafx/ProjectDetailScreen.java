package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectDetailScreen extends VBox {

    private final ProjectController projectController;
    private final long projectId;
    private final ListView<ProjectMemberResult> memberList = new ListView<>();
    private final Label messageLabel = new Label();
    private Runnable onBack;

    ProjectDetailScreen(ProjectController projectController, long projectId){
        this.projectController = projectController;
        this.projectId = projectId;
        setPadding(new Insets(20));
        setSpacing(12);

        Button backButton = new Button("← Back");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        Label titleLabel = new Label("Project Detail");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        HBox header = new HBox(backButton, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        memberList.setCellFactory(list -> new MemberCell());
        VBox.setVgrow(memberList, Priority.ALWAYS);

        messageLabel.setStyle("-fx-text-fill: #666;");

        getChildren().addAll(header, messageLabel, new Label("Members:"), memberList);
        loadDetail();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadDetail(){
        try{
            ProjectAdminDetail detail = projectController.viewProjectAdminDetail(projectId);
            messageLabel.setText(String.format("Project: %s | %s",
                    detail.project().name(), detail.project().description()));
            memberList.getItems().setAll(detail.participants());
        } catch (Exception exception){
            messageLabel.setText("Load failed: " + exception.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private static class MemberCell extends ListCell<ProjectMemberResult> {
        @Override
        protected void updateItem(ProjectMemberResult member, boolean empty){
            super.updateItem(member, empty);
            if (empty || member == null){ setText(null); return; }
            setText(String.format("%s (%s) | %s | %s",
                    member.userId(), member.userName(), member.role(), member.active() ? "active" : "inactive"));
        }
    }
}
