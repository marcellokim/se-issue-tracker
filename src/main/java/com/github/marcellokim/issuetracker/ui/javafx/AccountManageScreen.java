package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class AccountManageScreen extends VBox {

    private final DashboardController dashboardController;
    private final ListView<UserResult> userList = new ListView<>();
    private final Label messageLabel = new Label();
    private Runnable onBack;

    AccountManageScreen(DashboardController dashboardController){
        this.dashboardController = dashboardController;
        setPadding(new Insets(20));
        setSpacing(12);

        Label titleLabel = new Label("Account Management");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button backButton = new Button("← Back");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        Button createButton = new Button("+ Create Account");
        createButton.setOnAction(event -> messageLabel.setText("Account creation will be implemented in #195"));

        HBox header = new HBox(backButton, titleLabel, createButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        userList.setCellFactory(list -> new UserCell());
        VBox.setVgrow(userList, Priority.ALWAYS);

        messageLabel.setStyle("-fx-text-fill: #666;");

        getChildren().addAll(header, userList, messageLabel);
        loadUsers();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadUsers(){
        try{
            List<UserResult> users = dashboardController.viewUsers();
            userList.getItems().setAll(users);
        } catch (Exception exception){
            messageLabel.setText(exception.getMessage());
        }
    }

    private static class UserCell extends ListCell<UserResult> {
        @Override
        protected void updateItem(UserResult user, boolean empty){
            super.updateItem(user, empty);
            if (empty || user == null){
                setText(null);
                return;
            }
            setText(String.format("%s (%s) | %s | %s",
                    user.loginId(), user.name(), user.role(), user.active() ? "active" : "inactive"));
        }
    }
}
