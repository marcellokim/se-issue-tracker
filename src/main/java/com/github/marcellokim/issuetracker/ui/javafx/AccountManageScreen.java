package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class AccountManageScreen extends VBox {

    private final DashboardController dashboardController;
    private final AccountController accountController;
    private final ListView<UserResult> userList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private final Button renameButton = new Button("Rename");
    private final Button roleButton = new Button("Change Role");
    private final Button activateButton = new Button("Activate");
    private final Button deactivateButton = new Button("Deactivate");
    private Runnable onBack;

    AccountManageScreen(DashboardController dashboardController, AccountController accountController){
        this.dashboardController = dashboardController;
        this.accountController = accountController;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Account Management");
        ScreenComponents.growInHeader(titleLabel);
        Button createButton = new Button("+ Create Account");
        createButton.setOnAction(event -> showCreateDialog());

        userList.setCellFactory(list -> new UserCell());
        userList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateActionButtons());
        VBox.setVgrow(userList, Priority.ALWAYS);

        renameButton.setDisable(true);
        renameButton.setOnAction(event -> selectedUser().ifPresent(this::handleRename));
        roleButton.setDisable(true);
        roleButton.setOnAction(event -> selectedUser().ifPresent(this::handleChangeRole));
        activateButton.setDisable(true);
        activateButton.setOnAction(event -> selectedUser().ifPresent(this::handleActivate));
        deactivateButton.setDisable(true);
        deactivateButton.setOnAction(event -> selectedUser().ifPresent(this::handleDeactivate));
        HBox actionBar = new HBox(8, renameButton, roleButton, activateButton, deactivateButton);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel, createButton),
                userList, actionBar, messageLabel);
        loadUsers();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private Optional<UserResult> selectedUser(){
        return Optional.ofNullable(userList.getSelectionModel().getSelectedItem());
    }

    private void updateActionButtons(){
        boolean hasSelection = selectedUser().isPresent();
        renameButton.setDisable(!hasSelection);
        roleButton.setDisable(!hasSelection);
        activateButton.setDisable(!hasSelection);
        deactivateButton.setDisable(!hasSelection);
    }

    private void loadUsers(){
        try{
            List<UserResult> users = dashboardController.viewUsers();
            userList.getItems().setAll(users);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void showCreateDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        TextField loginIdField = new TextField();
        loginIdField.setPromptText("Login ID");
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<com.github.marcellokim.issuetracker.domain.Role> roleBox = new ComboBox<>();
        for (com.github.marcellokim.issuetracker.domain.Role r : com.github.marcellokim.issuetracker.domain.Role.values()){
            if (r != com.github.marcellokim.issuetracker.domain.Role.ADMIN) roleBox.getItems().add(r);
        }
        roleBox.setValue(com.github.marcellokim.issuetracker.domain.Role.DEV);
        VBox content = new VBox(8,
                new Label("Login ID:"), loginIdField,
                new Label("Name:"), nameField,
                new Label("Password:"), passwordField,
                new Label("Role:"), roleBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        Runnable validate = () -> okButton.setDisable(
                loginIdField.getText() == null || loginIdField.getText().isBlank()
                || nameField.getText() == null || nameField.getText().isBlank()
                || passwordField.getText() == null || passwordField.getText().isBlank());
        loginIdField.textProperty().addListener((obs, old, val) -> validate.run());
        nameField.textProperty().addListener((obs, old, val) -> validate.run());
        passwordField.textProperty().addListener((obs, old, val) -> validate.run());
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                accountController.createAccount(
                        loginIdField.getText().trim(),
                        nameField.getText().trim(),
                        passwordField.getText(),
                        roleBox.getValue());
                loadUsers();
                ScreenComponents.showInfo(messageLabel, "Account created");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private void handleRename(UserResult user){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rename Account");
        dialog.setHeaderText("Current: " + user.name());
        TextField nameField = new TextField(user.name());
        dialog.getDialogPane().setContent(nameField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        nameField.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? nameField.getText().trim() : null);
        dialog.showAndWait().ifPresent(name -> {
            try{
                accountController.renameAccount(user.loginId(), name);
                loadUsers();
                ScreenComponents.showInfo(messageLabel, "Account renamed");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleChangeRole(UserResult user){
        Dialog<com.github.marcellokim.issuetracker.domain.Role> dialog = new Dialog<>();
        dialog.setTitle("Change Role");
        dialog.setHeaderText("Current: " + user.role());
        ComboBox<com.github.marcellokim.issuetracker.domain.Role> roleBox = new ComboBox<>();
        for (com.github.marcellokim.issuetracker.domain.Role r : com.github.marcellokim.issuetracker.domain.Role.values()){
            if (r != com.github.marcellokim.issuetracker.domain.Role.ADMIN) roleBox.getItems().add(r);
        }
        roleBox.setValue(user.role());
        dialog.getDialogPane().setContent(roleBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("OK");
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? roleBox.getValue() : null);
        dialog.showAndWait().ifPresent(role -> {
            try{
                accountController.changeAccountRole(user.loginId(), role);
                loadUsers();
                ScreenComponents.showInfo(messageLabel, "Role changed");
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleActivate(UserResult user){
        try{
            accountController.activateAccount(user.loginId());
            loadUsers();
            ScreenComponents.showInfo(messageLabel, "Account activated: " + user.loginId());
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void handleDeactivate(UserResult user){
        try{
            accountController.deactivateAccount(user.loginId());
            loadUsers();
            ScreenComponents.showInfo(messageLabel, "Account deactivated: " + user.loginId());
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private static class UserCell extends ListCell<UserResult> {
        @Override
        protected void updateItem(UserResult user, boolean empty){
            super.updateItem(user, empty);
            if (empty || user == null){ setText(null); setGraphic(null); return; }
            setText(String.format("%s (%s) | %s | %s",
                    user.loginId(), user.name(), user.role(), user.active() ? "active" : "inactive"));
        }
    }
}
