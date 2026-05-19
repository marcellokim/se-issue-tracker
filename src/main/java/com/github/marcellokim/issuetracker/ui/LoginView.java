package com.github.marcellokim.issuetracker.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class LoginView {

    private final Label statusLabel = new Label();
    private final Label resultLabel = new Label();
    private final TextField loginIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextArea summaryArea = new TextArea();
    private final Button loginButton = new Button("Login");
    private final BorderPane root = new BorderPane();

    public LoginView() {
        configureFields();
        configureLayout();
    }

    public Parent root() {
        return root;
    }

    public Button loginButton() {
        return loginButton;
    }

    public String loginId() {
        return loginIdField.getText();
    }

    public String password() {
        return passwordField.getText();
    }

    public void fillCredentials(String loginId, String password) {
        loginIdField.setText(loginId);
        passwordField.setText(password);
    }

    public void setLoginEnabled(boolean enabled) {
        loginButton.setDisable(!enabled);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void showSuccess(String message, String summary) {
        resultLabel.setText(message);
        resultLabel.setStyle("-fx-text-fill: #047857;");
        summaryArea.setText(summary);
    }

    public void showFailure(String message) {
        resultLabel.setText(message);
        resultLabel.setStyle("-fx-text-fill: #b91c1c;");
        summaryArea.clear();
    }

    public void showWarning(String message) {
        resultLabel.setText(message);
        resultLabel.setStyle("-fx-text-fill: #b45309;");
    }

    private void configureFields() {
        loginIdField.setPromptText("admin, pl1, dev1, tester1");
        passwordField.setPromptText("password");
        loginButton.setDefaultButton(true);
        summaryArea.setEditable(false);
        summaryArea.setWrapText(false);
        summaryArea.setPrefRowCount(12);
        VBox.setVgrow(summaryArea, Priority.ALWAYS);
    }

    private void configureLayout() {
        root.setPadding(new Insets(24));
        root.setTop(header());
        root.setCenter(form());
        root.setBottom(summaryArea);
        BorderPane.setMargin(summaryArea, new Insets(18, 0, 0, 0));
    }

    private VBox header() {
        Label title = new Label("Issue Tracker Login");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        statusLabel.setStyle("-fx-text-fill: #4b5563;");

        VBox box = new VBox(6, title, statusLabel);
        box.setPadding(new Insets(0, 0, 18, 0));
        return box;
    }

    private GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        grid.add(new Label("ID"), 0, 0);
        grid.add(loginIdField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new HBox(10, loginButton, resultLabel), 1, 3);

        return grid;
    }
}
