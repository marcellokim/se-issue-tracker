package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class JavaFXApp extends Application {

    private ApplicationContext context;
    private Exception initFailure;

    @Override
    public void init(){
        try{
            context = ApplicationContext.fromEnvironment();
        } catch (Exception exception){
            initFailure = exception;
        }
    }

    @Override
    public void start(Stage primaryStage){
        if (initFailure != null){
            primaryStage.setTitle("Issue Tracker - Connection Failed");
            primaryStage.setScene(new Scene(new StackPane(new Label("DB connection failed: " + initFailure.getMessage())), 600, 400));
            primaryStage.show();
            return;
        }
        primaryStage.setTitle("Issue Tracker");
        primaryStage.setScene(new Scene(new StackPane(new Label("Issue Tracker")), 1024, 768));
        primaryStage.show();
    }
}
