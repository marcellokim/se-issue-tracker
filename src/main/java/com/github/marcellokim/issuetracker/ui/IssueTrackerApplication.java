package com.github.marcellokim.issuetracker.ui;

import javafx.application.Application;
import javafx.stage.Stage;

public final class IssueTrackerApplication extends Application {

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Issue Tracking System");
        stage.show();
    }
}
