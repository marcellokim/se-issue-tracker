package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.domain.Role;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class JavaFXApp extends Application {

    private ApplicationContext context;
    private Exception initFailure;
    private Stage primaryStage;

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
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Issue Tracker");
        if (initFailure != null){
            primaryStage.setScene(new Scene(new StackPane(new Label("DB connection failed: " + initFailure.getMessage())), 600, 400));
            primaryStage.show();
            return;
        }
        showLogin();
        primaryStage.show();
    }

    private void showLogin(){
        LoginScreen loginScreen = new LoginScreen(context.authenticationController());
        loginScreen.setOnLoginSuccess(user -> {
            if (user.role() == Role.ADMIN){
                showAdminDashboard();
            } else{
                showProjectList();
            }
        });
        primaryStage.setScene(new Scene(new StackPane(loginScreen), 1024, 768));
    }

    private void showAdminDashboard(){
        AdminDashboardScreen screen = new AdminDashboardScreen(context.dashboardController());
        screen.setOnAccountManage(this::showAccountManage);
        screen.setOnProjectManage(this::showProjectManage);
        screen.setOnLogout(() -> {
            context.authenticationController().logout();
            showLogin();
        });
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showAccountManage(){
        AccountManageScreen screen = new AccountManageScreen(context.dashboardController());
        screen.setOnBack(this::showAdminDashboard);
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showProjectManage(){
        ProjectManageScreen screen = new ProjectManageScreen(context.dashboardController());
        screen.setOnProjectSelected(project -> showProjectDetail(project.projectId()));
        screen.setOnBack(this::showAdminDashboard);
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showProjectDetail(long projectId){
        ProjectDetailScreen screen = new ProjectDetailScreen(context.projectController(), projectId);
        screen.setOnBack(this::showProjectManage);
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showProjectList(){
        ProjectListScreen screen = new ProjectListScreen(context.dashboardController());
        screen.setOnProjectSelected(project -> showIssueList(project.projectId()));
        screen.setOnLogout(() -> {
            context.authenticationController().logout();
            showLogin();
        });
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showIssueList(long projectId){
        IssueListScreen screen = new IssueListScreen(context.issueController(), context.projectController(), projectId);
        screen.setOnIssueSelected(issue -> showIssueDetail(issue.id(), projectId));
        screen.setOnBack(this::showProjectList);
        screen.setOnDeletedIssueManage(() -> showDeletedIssueManage(projectId));
        screen.setOnStatistics(() -> showStatistics(projectId));
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showIssueDetail(long issueId, long projectId){
        IssueDetailScreen screen = new IssueDetailScreen(context.issueController(), issueId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showDeletedIssueManage(long projectId){
        DeletedIssueScreen screen = new DeletedIssueScreen(context.deletedIssueController(), projectId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }

    private void showStatistics(long projectId){
        StatisticsScreen screen = new StatisticsScreen(context.statisticsController(), projectId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(new Scene(screen, 1024, 768));
    }
}
