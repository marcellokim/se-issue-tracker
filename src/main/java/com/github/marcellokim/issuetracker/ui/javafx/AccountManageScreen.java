package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class AccountManageScreen extends VBox {

    private final DashboardController dashboardController;
    private final ListView<UserResult> userList = new ListView<>();
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Runnable onBack;

    AccountManageScreen(DashboardController dashboardController){
        this.dashboardController = dashboardController;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Back", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Account Management");
        Button createButton = new Button("+ Create Account");
        createButton.setOnAction(event -> ScreenComponents.showInfo(messageLabel, "Account creation will be implemented in #195"));

        userList.setCellFactory(list -> new UserCell());
        VBox.setVgrow(userList, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.headerWithGrow(backButton, titleLabel, createButton),
                userList, messageLabel);
        loadUsers();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadUsers(){
        try{
            List<UserResult> users = dashboardController.viewUsers();
            userList.getItems().setAll(users);
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
