package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import java.util.Optional;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ProjectDetailScreen extends VBox {

    private final ProjectController projectController;
    private final long projectId;
    private final Label projectInfoLabel = new Label();
    private final ListView<ProjectMemberResult> memberList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private final Button removeParticipantButton = new Button("Remove Participant");
    private Runnable onBack;

    ProjectDetailScreen(ProjectController projectController, long projectId){
        this.projectController = projectController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Project Detail");

        projectInfoLabel.setWrapText(true);

        Button renameButton = new Button("Rename");
        renameButton.setOnAction(event -> handleRename());
        Button descButton = new Button("Change Description");
        descButton.setOnAction(event -> handleChangeDescription());
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> handleDelete());
        HBox projectActions = new HBox(8, renameButton, descButton, deleteButton);

        memberList.setCellFactory(list -> new MemberCell());
        memberList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) ->
                removeParticipantButton.setDisable(val == null));
        VBox.setVgrow(memberList, Priority.ALWAYS);

        Button addParticipantButton = new Button("Add Participant");
        addParticipantButton.setOnAction(event -> handleAddParticipant());
        removeParticipantButton.setDisable(true);
        removeParticipantButton.setOnAction(event -> selectedMember().ifPresent(this::handleRemoveParticipant));
        HBox memberActions = new HBox(8, addParticipantButton, removeParticipantButton);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                projectInfoLabel, projectActions,
                new Label("Members:"), memberList, memberActions,
                messageLabel);
        loadDetail();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private Optional<ProjectMemberResult> selectedMember(){
        return Optional.ofNullable(memberList.getSelectionModel().getSelectedItem());
    }

    private void loadDetail(){
        try{
            ProjectAdminDetail detail = projectController.viewProjectAdminDetail(projectId);
            projectInfoLabel.setText(String.format("Project: %s | %s",
                    detail.project().name(), detail.project().description()));
            memberList.getItems().setAll(detail.participants());
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void handleRename(){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rename Project");
        TextField nameField = new TextField();
        nameField.setPromptText("New project name");
        dialog.getDialogPane().setContent(nameField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        nameField.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? nameField.getText().trim() : null);
        dialog.showAndWait().ifPresent(name -> {
            try{
                projectController.renameProject(projectId, name);
                loadDetail();
                ScreenComponents.showInfo(messageLabel, "Project renamed");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleChangeDescription(){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Description");
        TextField descField = new TextField();
        descField.setPromptText("New description");
        dialog.getDialogPane().setContent(descField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        descField.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? descField.getText().trim() : null);
        dialog.showAndWait().ifPresent(description -> {
            try{
                projectController.changeProjectDescription(projectId, description);
                loadDetail();
                ScreenComponents.showInfo(messageLabel, "Description changed");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleDelete(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete Project");
        dialog.setHeaderText("Are you sure you want to delete this project?");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Delete");
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                projectController.deleteProject(projectId);
                if (onBack != null) onBack.run();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private void handleAddParticipant(){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Participant");
        TextField loginIdField = new TextField();
        loginIdField.setPromptText("User login ID");
        dialog.getDialogPane().setContent(loginIdField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        loginIdField.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? loginIdField.getText().trim() : null);
        dialog.showAndWait().ifPresent(loginId -> {
            try{
                projectController.addProjectParticipant(projectId, loginId);
                loadDetail();
                ScreenComponents.showInfo(messageLabel, "Participant added: " + loginId);
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleRemoveParticipant(ProjectMemberResult member){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Remove Participant");
        dialog.setHeaderText("Remove " + member.userId() + " (" + member.userName() + ")?");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Remove");
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                projectController.removeProjectParticipant(projectId, member.userId());
                loadDetail();
                ScreenComponents.showInfo(messageLabel, "Participant removed: " + member.userId());
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
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
