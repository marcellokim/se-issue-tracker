package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class AdminDashboardScreen extends VBox {

    private final DashboardController dashboardController;
    private final Label statusLabel = ScreenComponents.messageLabel();
    private Runnable onAccountManage;
    private Runnable onProjectManage;
    private Runnable onLogout;

    AdminDashboardScreen(DashboardController dashboardController){
        this.dashboardController = dashboardController;
        ScreenComponents.applyScreenDefaults(this);

        Label titleLabel = new Label("Admin Dashboard");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> { if (onLogout != null) onLogout.run(); });

        HBox header = new HBox(titleLabel, logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button accountButton = new Button("Account Management");
        accountButton.setPrefWidth(200);
        accountButton.setPrefHeight(60);
        accountButton.setStyle("-fx-font-size: 16px;");
        accountButton.setOnAction(event -> { if (onAccountManage != null) onAccountManage.run(); });

        Button projectButton = new Button("Project Management");
        projectButton.setPrefWidth(200);
        projectButton.setPrefHeight(60);
        projectButton.setStyle("-fx-font-size: 16px;");
        projectButton.setOnAction(event -> { if (onProjectManage != null) onProjectManage.run(); });

        HBox buttons = new HBox(20, accountButton, projectButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(40, 0, 0, 0));

        getChildren().addAll(header, statusLabel, buttons);
        loadSummary();
    }

    void setOnAccountManage(Runnable action){ this.onAccountManage = action; }
    void setOnProjectManage(Runnable action){ this.onProjectManage = action; }
    void setOnLogout(Runnable action){ this.onLogout = action; }

    private void loadSummary(){
        try{
            List<DashboardProjectSummary> projects = dashboardController.viewProjects();
            List<UserResult> users = dashboardController.viewUsers();
            ScreenComponents.showInfo(statusLabel, String.format("Projects: %d | Users: %d", projects.size(), users.size()));
        } catch (Exception exception){
            ScreenComponents.showError(statusLabel, exception);
        }
    }
}
