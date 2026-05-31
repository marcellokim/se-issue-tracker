package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.service.AuthenticationResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

final class LoginScreen extends VBox {

    private final TextField loginIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Sign in");
    private final Label messageLabel = new Label();
    private final AuthenticationController authController;
    private Consumer<UserResult> onLoginSuccess;

    LoginScreen(AuthenticationController authController){
        this.authController = authController;
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        setSpacing(12);
        setMaxWidth(360);

        Label titleLabel = new Label("Issue Tracker");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        loginIdField.setPromptText("ID");
        loginIdField.setMaxWidth(280);

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(280);
        passwordField.setOnAction(event -> handleLogin());

        loginButton.setMaxWidth(280);
        loginButton.setStyle("-fx-font-size: 14px;");
        loginButton.setOnAction(event -> handleLogin());

        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(280);

        getChildren().addAll(titleLabel, loginIdField, passwordField, loginButton, messageLabel);
    }

    void setOnLoginSuccess(Consumer<UserResult> action){
        this.onLoginSuccess = action;
    }

    private void handleLogin(){
        loginButton.setDisable(true);
        messageLabel.setText("");
        try{
            AuthenticationResult result = authController.login(loginIdField.getText(), passwordField.getText());
            if (!result.success()){
                messageLabel.setText(result.message());
                return;
            }
            passwordField.clear();
            if (onLoginSuccess != null){
                onLoginSuccess.accept(result.user());
            }
        } catch (Exception exception){
            messageLabel.setText("Login failed: " + exception.getMessage());
        } finally{
            loginButton.setDisable(false);
        }
    }
}
