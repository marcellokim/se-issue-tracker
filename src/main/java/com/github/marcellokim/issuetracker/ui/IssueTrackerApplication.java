package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.controller.LoginController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class IssueTrackerApplication extends Application {

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView();
        initializeLoginFlow(loginView);

        Scene scene = new Scene(loginView.root(), 620, 520);
        stage.setTitle("ITS Login");
        stage.setScene(scene);
        stage.show();
    }

    private static void initializeLoginFlow(LoginView loginView) {
        try {
            ApplicationContext context = ApplicationContext.fromEnvironment();
            var controller = new LoginController(
                    loginView,
                    context.authenticationService(),
                    context.dashboardService()
            );
            controller.bind();
            loginView.setStatus("Oracle repository ready.");
        } catch (Exception exception) {
            loginView.setLoginEnabled(false);
            loginView.setStatus("Oracle repository failed: " + exception.getMessage());
            loginView.showWarning("DB failed");
        }
    }
}
