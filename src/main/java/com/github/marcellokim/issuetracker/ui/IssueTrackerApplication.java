package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
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
            bindLoginAction(loginView, context.authenticationService(), context.dashboardPresenter());
            loginView.setStatus("Oracle repository ready.");
        } catch (Exception exception) {
            loginView.setLoginEnabled(false);
            loginView.setStatus("Oracle repository failed: " + exception.getMessage());
            loginView.showWarning("DB failed");
        }
    }

    private static void bindLoginAction(
            LoginView loginView,
            AuthenticationService authenticationService,
            DemoDashboardPresenter dashboardPresenter
    ) {
        loginView.loginButton().setOnAction(event -> {
            var result = authenticationService.login(loginView.loginId(), loginView.password());
            if (result.success()) {
                loginView.showSuccess(result.message(), dashboardPresenter.buildSummary(result.user()));
            } else {
                loginView.showFailure(result.message());
            }
        });
    }
}
