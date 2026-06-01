package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.domain.Role;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class JavaFXApp extends Application {

    private ApplicationContext context;
    private Exception initFailure;
    private Stage primaryStage;
    private Role currentUserRole;

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
            primaryStage.setScene(styledScene(new StackPane(new Label("DB connection failed: " + initFailure.getMessage())), 600, 400));
            primaryStage.show();
            return;
        }
        showLogin();
        primaryStage.show();
    }

    private void showLogin(){
        currentUserRole = null;
        LoginScreen loginScreen = new LoginScreen(context.authenticationController());
        loginScreen.setOnLoginSuccess(user -> {
            currentUserRole = user.role();
            if (user.role() == Role.ADMIN){
                showAdminDashboard();
            } else{
                showProjectList();
            }
        });
        primaryStage.setScene(styledScene(new StackPane(loginScreen), 1024, 768));
    }

    private void showAdminDashboard(){
        AdminDashboardScreen screen = new AdminDashboardScreen(context.dashboardController());
        screen.setOnAccountManage(this::showAccountManage);
        screen.setOnProjectManage(this::showProjectManage);
        screen.setOnLogout(() -> {
            context.authenticationController().logout();
            showLogin();
        });
        primaryStage.setScene(styledScene(screen));
    }

    private void showAccountManage(){
        AccountManageScreen screen = new AccountManageScreen(context.dashboardController(), context.accountController());
        screen.setOnBack(this::showAdminDashboard);
        primaryStage.setScene(styledScene(screen));
    }

    private void showProjectManage(){
        ProjectManageScreen screen = new ProjectManageScreen(context.dashboardController(), context.projectController());
        screen.setOnProjectSelected(project -> showProjectDetail(project.projectId()));
        screen.setOnBack(this::showAdminDashboard);
        primaryStage.setScene(styledScene(screen));
    }

    private void showProjectDetail(long projectId){
        ProjectDetailScreen screen = new ProjectDetailScreen(context.projectController(), projectId);
        screen.setOnBack(this::showProjectManage);
        primaryStage.setScene(styledScene(screen));
    }

    private void showProjectList(){
        ProjectListScreen screen = new ProjectListScreen(context.dashboardController());
        screen.setOnProjectSelected(project -> showIssueList(project.projectId()));
        screen.setOnLogout(() -> {
            context.authenticationController().logout();
            showLogin();
        });
        primaryStage.setScene(styledScene(screen));
    }

    private void showIssueList(long projectId){
        boolean isPl = currentUserRole == Role.PL;
        IssueListScreen screen = new IssueListScreen(context.issueController(), context.projectController(), projectId, isPl);
        screen.setOnIssueSelected(issue -> showIssueDetail(issue.id(), projectId));
        screen.setOnBack(this::showProjectList);
        screen.setOnDeletedIssueManage(() -> showDeletedIssueManage(projectId));
        screen.setOnStatistics(() -> showStatistics(projectId));
        screen.setOnGraph(() -> showIssueGraph(projectId));
        primaryStage.setScene(styledScene(screen));
    }

    private void showIssueGraph(long projectId){
        IssueGraphScreen screen = new IssueGraphScreen(context.issueController(), projectId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(styledScene(screen));
    }

    private void showIssueDetail(long issueId, long projectId){
        IssueDetailScreen screen = new IssueDetailScreen(context.issueController(), context.issueStateController(), context.assignmentController(), context.deletedIssueController(), issueId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(styledScene(screen));
    }

    private void showDeletedIssueManage(long projectId){
        DeletedIssueScreen screen = new DeletedIssueScreen(context.deletedIssueController(), projectId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(styledScene(screen));
    }

    private void showStatistics(long projectId){
        StatisticsScreen screen = new StatisticsScreen(context.statisticsController(), projectId);
        screen.setOnBack(() -> showIssueList(projectId));
        primaryStage.setScene(styledScene(screen));
    }

    private static Scene styledScene(Parent root){
        return styledScene(root, 1024, 768);
    }

    private static Scene styledScene(Parent root, double width, double height){
        Scene scene = new Scene(root, width, height);
        ScreenComponents.applyStylesheet(scene);
        return scene;
    }
}
