package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class LoginView {

    private static final String LOGIN_FAILURE_MESSAGE = "\uC798\uBABB\uB41C \uB85C\uADF8\uC778 ID, \uBE44\uBC00\uBC88\uD638 \uC785\uB2C8\uB2E4.";
    private static final String INACTIVE_ACCOUNT_MESSAGE = "\uD604\uC7AC \uADC0\uD558\uC758 \uACC4\uC815\uC774 \uBE44\uD65C\uC131\uD654 "
            + "\uC0C1\uD0DC\uC785\uB2C8\uB2E4. admin\uC5D0\uAC8C \uBB38\uC758 "
            + "\uBC14\uB78D\uB2C8\uB2E4.";

    private final Label statusLabel = new Label();
    private final Label resultLabel = new Label();
    private final TextField loginIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Login");
    private final Button logoutButton = new Button("Logout");
    private final BorderPane root = new BorderPane();
    private final GridPane loginForm = loginForm();

    private User currentUser;
    private AuthenticationController authenticationController;
    private AccountController accountController;
    private DashboardController dashboardController;
    private ProjectController projectController;
    private IssueController issueController;
    private AssignmentController assignmentController;
    private IssueStateController issueStateController;
    private DeletedIssueController deletedIssueController;
    private StatisticsController statisticsController;
    private List<Issue> currentIssues = List.of();
    private List<DashboardProjectView> currentProjects = List.of();
    private List<User> currentUsers = List.of();
    private Issue selectedIssue;
    private DashboardProjectView selectedProject;
    private String dashboardMessage = "";
    private String issueDetailMessage = "";
    private String projectDetailMessage = "";
    private AdminDashboardView adminDashboardView;

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

    public Button logoutButton() {
        return logoutButton;
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

    public void bindControllers(
            AuthenticationController authenticationController,
            AccountController accountController,
            DashboardController dashboardController,
            ProjectController projectController,
            IssueController issueController,
            AssignmentController assignmentController,
            IssueStateController issueStateController,
            DeletedIssueController deletedIssueController,
            StatisticsController statisticsController) {
        this.authenticationController = Objects.requireNonNull(authenticationController, "authenticationController");
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.assignmentController = Objects.requireNonNull(assignmentController, "assignmentController");
        this.issueStateController = Objects.requireNonNull(issueStateController, "issueStateController");
        this.deletedIssueController = Objects.requireNonNull(deletedIssueController, "deletedIssueController");
        this.statisticsController = Objects.requireNonNull(statisticsController, "statisticsController");
        loginButton.setOnAction(event -> login());
        logoutButton.setOnAction(event -> logout());
    }

    public void setLoginEnabled(boolean enabled) {
        loginButton.setDisable(!enabled);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void showSuccess(User user) {
        currentUser = user;
        selectedIssue = null;
        selectedProject = null;
        issueDetailMessage = "";
        projectDetailMessage = "";
        refreshDashboard("Login succeeded. Select a project or issue.");
    }

    public void showLoginForm() {
        currentUser = null;
        selectedIssue = null;
        selectedProject = null;
        currentIssues = List.of();
        currentProjects = List.of();
        currentUsers = List.of();
        dashboardMessage = "";
        issueDetailMessage = "";
        projectDetailMessage = "";
        loginIdField.clear();
        passwordField.clear();
        resultLabel.setText("");
        root.setCenter(loginForm);
        root.setBottom(null);
    }

    public void showFailure(String message) {
        resultLabel.setText("This account is inactive.".equals(message)
                ? INACTIVE_ACCOUNT_MESSAGE
                : LOGIN_FAILURE_MESSAGE);
        resultLabel.setStyle("-fx-text-fill: #b91c1c;");
    }

    public void showWarning(String message) {
        resultLabel.setText(message);
        resultLabel.setStyle("-fx-text-fill: #b45309;");
    }

    private void configureFields() {
        loginIdField.setPromptText("LOGIN_ID");
        passwordField.setPromptText("password");
        loginButton.setDefaultButton(true);
        resultLabel.setMinHeight(26);
    }

    private void configureLayout() {
        root.setPadding(new Insets(24));
        root.setStyle("-fx-font-size: 15px; -fx-background-color: #f3f4f6;");
        root.setTop(header());
        root.setCenter(loginForm);
    }

    private VBox header() {
        Label title = new Label("Issue Tracker");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        statusLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4b5563;");
        VBox box = new VBox(6, title, statusLabel);
        box.setPadding(new Insets(0, 0, 18, 0));
        return box;
    }

    private GridPane loginForm() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("ID"), 0, 0);
        grid.add(loginIdField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 1, 2);
        grid.add(resultLabel, 1, 3);
        return grid;
    }

    private void login() {
        var result = authenticationController.login(loginId(), password());
        if (result.success()) {
            showSuccess(result.user());
        } else {
            showFailure(result.message());
        }
    }

    private void logout() {
        authenticationController.logout();
        showLoginForm();
    }

    private void refreshDashboard(String message) {
        currentIssues = dashboardController.viewRelatedIssues();
        currentProjects = dashboardController.viewProjects();
        currentUsers = dashboardController.viewUsers();
        keepSelectionIfStillVisible();
        dashboardMessage = valueOrBlank(message);
        root.setCenter(scroll(dashboardView()));
    }

    private void keepSelectionIfStillVisible() {
        if (selectedIssue != null) {
            selectedIssue = currentIssues.stream()
                    .filter(issue -> issue.id() == selectedIssue.id())
                    .findFirst()
                    .orElse(null);
        }
        if (selectedProject != null) {
            selectedProject = currentProjects.stream()
                    .filter(project -> project.projectId() == selectedProject.projectId())
                    .findFirst()
                    .orElse(null);
        }
    }

    private VBox dashboardView() {
        Label userInfoLabel = new Label("Login: %s / %s / %s".formatted(
                currentUser.getLoginId(),
                currentUser.getName(),
                currentUser.getRole()));
        userInfoLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        HBox userBar = new HBox(16, userInfoLabel, logoutButton);
        userBar.setAlignment(Pos.CENTER_LEFT);
        userBar.setPadding(new Insets(0, 0, 4, 0));

        Node accountManagement = null;
        adminDashboardView = null;
        if (currentUser.getRole() == Role.ADMIN) {
            adminDashboardView = new AdminDashboardView(accountController, this::refreshDashboard);
            accountManagement = adminDashboardView.root();
        }

        ProjectBoardView boardView = new ProjectBoardView(
                currentUser,
                projectController,
                issueController,
                this::selectIssue,
                this::selectProject,
                this::refreshDashboard,
                this::refreshProjectPage);

        VBox box = new VBox(
                14,
                userBar,
                boardView.dashboard(currentIssues, currentProjects, currentUsers, this::selectAccount,
                        accountManagement),
                messageLabel(dashboardMessage));
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private void selectAccount(User account) {
        if (adminDashboardView != null) {
            adminDashboardView.selectAccount(account);
        }
    }

    private void selectIssue(Issue issue) {
        selectedIssue = issue;
        selectedProject = null;
        issueDetailMessage = "";
        showIssueDetail(issue);
    }

    private void refreshIssueDetail(String message) {
        currentIssues = dashboardController.viewRelatedIssues();
        if (selectedIssue != null) {
            long selectedIssueId = selectedIssue.id();
            selectedIssue = currentIssues.stream()
                    .filter(issue -> issue.id() == selectedIssueId)
                    .findFirst()
                    .orElse(null);
        }
        issueDetailMessage = valueOrBlank(message);
        if (selectedIssue == null) {
            refreshDashboard(message);
            return;
        }
        showIssueDetail(selectedIssue);
    }

    private void showIssueDetail(Issue issue) {
        IssueDetailView detailView = new IssueDetailView(
                issue,
                issueController,
                assignmentController,
                issueStateController,
                deletedIssueController,
                statisticsController,
                () -> refreshDashboard(""),
                this::refreshIssueDetail,
                issueDetailMessage);
        root.setCenter(scroll(detailView.root()));
    }

    private void selectProject(DashboardProjectView project) {
        selectedProject = project;
        selectedIssue = null;
        projectDetailMessage = "";
        showProjectPage(project);
    }

    private void refreshProjectPage(long projectId, String message) {
        currentProjects = dashboardController.viewProjects();
        selectedProject = currentProjects.stream()
                .filter(project -> project.projectId() == projectId)
                .findFirst()
                .orElse(null);
        projectDetailMessage = valueOrBlank(message);
        if (selectedProject == null) {
            refreshDashboard(message);
            return;
        }
        showProjectPage(selectedProject);
    }

    private void showProjectPage(DashboardProjectView project) {
        ProjectBoardView boardView = new ProjectBoardView(
                currentUser,
                projectController,
                issueController,
                this::selectIssue,
                this::selectProject,
                this::refreshDashboard,
                this::refreshProjectPage);
        root.setCenter(scroll(boardView.projectDetail(project, projectDetailMessage, () -> refreshDashboard(""))));
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private static Label messageLabel(String message) {
        Label label = new Label(valueOrBlank(message));
        label.setMinHeight(36);
        label.setWrapText(true);
        label.setStyle(valueOrBlank(message).startsWith("Failed:")
                ? "-fx-font-size: 17px; -fx-text-fill: #b91c1c;"
                : "-fx-font-size: 17px; -fx-text-fill: #111827;");
        return label;
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
