package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueController.CommentView;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class LoginView {

    private static final String LOGIN_FAILURE_MESSAGE = "잘못된 로그인 ID, 비밀번호 입니다";
    private static final String INACTIVE_ACCOUNT_MESSAGE = "현재 귀하의 계정이 비활성화 상태입니다. admin에게 문의 바랍니다";

    private final Label statusLabel = new Label();
    private final Label resultLabel = new Label();
    private final TextField loginIdField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Login");
    private final Button logoutButton = new Button("Logout");
    private final BorderPane root = new BorderPane();
    private final GridPane loginForm = loginForm();

    private final TextField projectIdField = field("project id");
    private final TextField projectNameField = field("new project name");
    private final TextField projectDescriptionField = field("project description");
    private final TextField participantLoginIdField = field("participant loginId");
    private final TextField createAccountLoginIdField = field("new account loginId");
    private final TextField createAccountNameField = field("new account name");
    private final PasswordField createAccountPasswordField = new PasswordField();
    private final ComboBox<Role> createAccountRoleBox = new ComboBox<>();
    private final TextField updateAccountLoginIdField = field("account loginId");
    private final TextField updateAccountNameField = field("updated account name");
    private final ComboBox<Role> updateAccountRoleBox = new ComboBox<>();
    private final TextField accountStatusLoginIdField = field("account loginId");
    private final TextField issueIdField = field("issue id");
    private final TextField issueTitleField = field("issue title");
    private final TextField issueDescriptionField = field("issue description");
    private final ComboBox<Priority> priorityBox = new ComboBox<>();
    private final TextField assigneeLoginIdField = field("assignee DEV loginId");
    private final TextField verifierLoginIdField = field("verifier TESTER loginId");
    private final TextField blockingIssueIdField = field("blocking issue id");
    private final TextField blockedIssueIdField = field("blocked issue id");
    private final TextField dependencyIdField = field("dependency id");
    private final TextField commentIdField = field("comment id");
    private final TextArea commentArea = area("general comment content");
    private final TextArea reasonArea = area("status/delete reason");
    private final TextArea workflowOutputArea = area("");
    private final TextArea detailOutputArea = area("");

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

    public void showSuccess(User user) {
        currentUser = user;
        selectedIssue = null;
        selectedProject = null;
        clearWorkflowInputs();
        refreshDashboard("Login succeeded. Select a project or issue, then run a workflow action.");
    }

    public void showLoginForm() {
        currentUser = null;
        selectedIssue = null;
        selectedProject = null;
        currentIssues = List.of();
        currentProjects = List.of();
        currentUsers = List.of();
        loginIdField.clear();
        passwordField.clear();
        resultLabel.setText("");
        workflowOutputArea.clear();
        clearWorkflowInputs();
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
        createAccountPasswordField.setPromptText("temporary password");
        resultLabel.setMinHeight(26);
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.MAJOR);
        createAccountRoleBox.getItems().setAll(Role.PL, Role.DEV, Role.TESTER);
        createAccountRoleBox.setValue(Role.DEV);
        updateAccountRoleBox.getItems().setAll(Role.PL, Role.DEV, Role.TESTER);
        updateAccountRoleBox.setValue(Role.DEV);
        workflowOutputArea.setEditable(false);
        workflowOutputArea.setWrapText(true);
        workflowOutputArea.setPrefRowCount(6);
        detailOutputArea.setEditable(false);
        detailOutputArea.setWrapText(true);
        detailOutputArea.setPrefRowCount(4);
        commentArea.setPrefRowCount(3);
        reasonArea.setPrefRowCount(3);
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

    private void refreshDashboard(String message) {
        currentIssues = dashboardController.viewRelatedIssues();
        currentProjects = dashboardController.viewProjects();
        currentUsers = dashboardController.viewUsers();
        keepSelectionIfStillVisible();
        root.setCenter(null);
        root.setCenter(scroll(dashboardView()));
        workflowOutputArea.setText(message);
        workflowOutputArea.setStyle("-fx-text-fill: #111827;");
    }

    private void clearWorkflowOutput() {
        workflowOutputArea.clear();
        workflowOutputArea.setStyle("-fx-text-fill: #111827;");
    }

    private void clearDetailOutput() {
        detailOutputArea.clear();
        detailOutputArea.setStyle("-fx-text-fill: #111827;");
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

        HBox dashboard = new HBox(16, issuePanel(), projectPanel());
        dashboard.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(14, userBar, dashboard, projectCreatePanel());
        if (isAdmin()) {
            box.getChildren().add(accountManagementPanel());
        }
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private VBox projectCreatePanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #ffffff;");
        workflowOutputArea.setPrefRowCount(3);
        panel.getChildren().addAll(
                sectionLabel("Project Create"),
                fieldsGrid(
                        "Project Name", projectNameField,
                        "Description", projectDescriptionField),
                actionRow(actionButton("Create Project", isAdmin(), () -> {
                    var project = projectController.createProject(
                            requiredText(projectNameField, "projectName"),
                            text(projectDescriptionField));
                    projectIdField.setText(String.valueOf(project.getId()));
                    return "Project created: " + project.getId() + " / " + project.getName();
                })),
                workflowOutputArea);
        return panel;
    }

    private VBox accountManagementPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #ffffff;");
        VBox createPanel = panel("Create Account");
        createPanel.getChildren().addAll(
                fieldsGrid(
                        "Login ID", createAccountLoginIdField,
                        "Name", createAccountNameField,
                        "Password", createAccountPasswordField,
                        "Role", createAccountRoleBox),
                actionRow(actionButton("Create Account", true, () -> {
                    User user = accountController.createAccount(
                            requiredText(createAccountLoginIdField, "loginId"),
                            requiredText(createAccountNameField, "name"),
                            requiredText(createAccountPasswordField, "password"),
                            createAccountRoleBox.getValue());
                    createAccountPasswordField.clear();
                    return "Account created: " + formatAccount(user);
                })));

        VBox updatePanel = panel("Update Account");
        updatePanel.getChildren().addAll(
                fieldsGrid(
                        "Login ID", updateAccountLoginIdField,
                        "Name", updateAccountNameField,
                        "Role", updateAccountRoleBox),
                actionRow(actionButton("Update Account", true, () -> {
                    User user = accountController.updateAccount(
                            requiredText(updateAccountLoginIdField, "loginId"),
                            requiredText(updateAccountNameField, "name"),
                            updateAccountRoleBox.getValue());
                    return "Account updated: " + formatAccount(user);
                })));

        VBox activePanel = panel("Activation");
        activePanel.getChildren().addAll(
                fieldsGrid("Login ID", accountStatusLoginIdField),
                actionRow(
                        actionButton("Activate", true, () -> {
                            User user = accountController.activateAccount(
                                    requiredText(accountStatusLoginIdField, "loginId"));
                            return "Account activated: " + formatAccount(user);
                        }),
                        actionButton("Deactivate", true, () -> {
                            User user = accountController.deactivateAccount(
                                    requiredText(accountStatusLoginIdField, "loginId"));
                            return "Account deactivated: " + formatAccount(user);
                        })));

        panel.getChildren().addAll(
                sectionLabel("Account Management"),
                new HBox(12, createPanel, updatePanel, activePanel));
        return panel;
    }

    private VBox issuePanel() {
        VBox list = panel(currentUser.getRole() == Role.ADMIN ? "All Issues" : "Related Issues");
        if (currentIssues.isEmpty()) {
            list.getChildren().add(emptyLabel("No issues to show."));
        }
        for (Issue issue : currentIssues) {
            Button button = card("""
                    %s
                    id=%d / project=%d
                    %s / %s
                    """.formatted(issue.title(), issue.id(), issue.projectId(), issue.status(), issue.priority()));
            button.setOnAction(event -> selectIssue(issue));
            list.getChildren().add(button);
        }
        return scrollablePanel(list);
    }

    private VBox projectPanel() {
        VBox list = panel(currentUser.getRole() == Role.ADMIN ? "All Projects" : "My Projects");
        if (currentProjects.isEmpty()) {
            list.getChildren().add(emptyLabel("No projects to show."));
        }
        for (DashboardProjectView project : currentProjects) {
            Button button = card("""
                    %s
                    projectId=%d
                    members=%d / issues=%d / deleted=%d
                    """.formatted(
                    project.projectName(),
                    project.projectId(),
                    project.memberCount(),
                    project.visibleIssueCount(),
                    project.deletedIssueCount()));
            button.setOnAction(event -> selectProject(project));
            list.getChildren().add(button);
        }
        if (!currentUsers.isEmpty()) {
            list.getChildren().add(sectionLabel("Users"));
            for (User account : currentUsers) {
                Button accountButton = card(formatAccount(account));
                accountButton.setOnAction(event -> selectAccount(account));
                list.getChildren().add(accountButton);
            }
        }
        return scrollablePanel(list);
    }

    private void selectAccount(User account) {
        updateAccountLoginIdField.setText(account.getLoginId());
        updateAccountNameField.setText(account.getName());
        if (account.getRole() != Role.ADMIN) {
            updateAccountRoleBox.setValue(account.getRole());
        }
        accountStatusLoginIdField.setText(account.getLoginId());
    }

    private VBox workflowPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #ffffff;");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().setAll(
                actionTab("Project", projectActionContent()),
                actionTab("Issue", issueActionContent()),
                actionTab("Assignment", assignmentActionContent()),
                actionTab("Status", stateActionContent()),
                actionTab("Dependency", dependencyActionContent()),
                actionTab("Deleted/Stats", deletedAndStatisticsActionContent()));
        workflowOutputArea.setPrefRowCount(4);
        panel.getChildren().addAll(sectionLabel("Workflow Actions"), contextGrid(), tabs, workflowOutputArea);
        return panel;
    }

    private GridPane contextGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        addInput(grid, 0, 0, "Project ID", projectIdField);
        addInput(grid, 2, 0, "Issue ID", issueIdField);
        grid.add(new Label("Reason"), 4, 0);
        grid.add(reasonArea, 5, 0);
        return grid;
    }

    private VBox projectActionContent() {
        return actionContent(
                fieldsGrid(
                        "Project Name", projectNameField,
                        "Description", projectDescriptionField,
                        "Participant Login ID", participantLoginIdField),
                actionRow(
                        actionButton("Create", isAdmin(), () -> {
                            var project = projectController.createProject(
                                    requiredText(projectNameField, "projectName"),
                                    text(projectDescriptionField));
                            projectIdField.setText(String.valueOf(project.getId()));
                            return "Project created: " + project.getId() + " / " + project.getName();
                        }),
                        actionButton("Delete", isAdmin(), () -> {
                            projectController.deleteProject(requiredLong(projectIdField, "projectId"));
                            return "Project deleted.";
                        }),
                        actionButton("Add Member", isAdmin(), () -> {
                            projectController.addProjectParticipant(
                                    requiredLong(projectIdField, "projectId"),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant added.";
                        }),
                        actionButton("Remove Member", isAdmin(), () -> {
                            projectController.removeProjectParticipant(
                                    requiredLong(projectIdField, "projectId"),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant removed.";
                        })));
    }

    private VBox issueActionContent() {
        return actionContent(
                fieldsGrid(
                        "Title", issueTitleField,
                        "Description", issueDescriptionField,
                        "Priority", priorityBox,
                        "Comment ID", commentIdField,
                        "Comment Content", commentArea),
                actionRow(
                        actionButton("Register", isIssueActor(), () -> {
                            var issue = issueController.registerIssue(
                                    requiredLong(projectIdField, "projectId"),
                                    requiredText(issueTitleField, "issueTitle"),
                                    requiredText(issueDescriptionField, "issueDescription"),
                                    priorityBox.getValue());
                            return "Issue registered: " + issue.issueId() + " / " + issue.status();
                        }),
                        actionButton("Update Title/Desc", canUpdateSelectedIssue(), () -> {
                            var issue = issueController.updateIssue(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(issueTitleField, "issueTitle"),
                                    requiredText(issueDescriptionField, "issueDescription"));
                            return "Issue updated: " + issue.issueId() + " / " + issue.status();
                        }),
                        actionButton("Change Priority", canChangePriority(), () -> {
                            var issue = issueController.changePriority(
                                    requiredLong(issueIdField, "issueId"),
                                    priorityBox.getValue());
                            return "Priority changed: " + issue.issueId() + " / " + issue.priority();
                        }),
                        actionButton("Add Comment", isIssueActor(), () -> {
                            var comment = issueController.addComment(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(commentArea, "comment"));
                            return "Comment added: " + comment.commentId() + " / " + comment.purpose();
                        }),
                        actionButton("Delete Comment", isIssueActor(), () -> {
                            issueController.deleteComment(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredLong(commentIdField, "commentId"));
                            return "General comment deleted.";
                        })));
    }

    private VBox assignmentActionContent() {
        return actionContent(
                fieldsGrid(
                        "Assignee DEV", assigneeLoginIdField,
                        "Verifier TESTER", verifierLoginIdField),
                actionRow(
                        actionButton("Candidates", canStartAssignment(), () -> {
                            AssignmentOptions options = assignmentController
                                    .startAssignment(requiredLong(issueIdField, "issueId"));
                            return formatAssignmentOptions(options);
                        }),
                        actionButton("Assign", canAssign(), () -> {
                            var result = assignmentController.assignIssue(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(assigneeLoginIdField, "assignee"),
                                    requiredText(verifierLoginIdField, "verifier"));
                            return "Assigned: " + result.issueId() + " / " + result.status();
                        }),
                        actionButton("Reassign DEV", canReassign(), () -> {
                            var result = assignmentController.reassignIssue(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(assigneeLoginIdField, "assignee"));
                            return "Assignee changed: " + result.issueId();
                        }),
                        actionButton("Change TESTER", canChangeVerifier(), () -> {
                            var result = assignmentController.changeVerifier(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(verifierLoginIdField, "verifier"));
                            return "Verifier changed: " + result.issueId();
                        })));
    }

    private VBox stateActionContent() {
        return actionContent(actionRow(
                actionButton("ASSIGNED -> FIXED", canMarkFixed(), () -> changeStatus(IssueStatus.FIXED)),
                actionButton("FIXED -> ASSIGNED", canRejectFix(), () -> changeStatus(IssueStatus.ASSIGNED)),
                actionButton("FIXED -> RESOLVED", canResolve(), () -> changeStatus(IssueStatus.RESOLVED)),
                actionButton("RESOLVED -> CLOSED", canClose(), () -> changeStatus(IssueStatus.CLOSED)),
                actionButton("RESOLVED/CLOSED -> REOPENED", canReopen(), () -> changeStatus(IssueStatus.REOPENED))));
    }

    private VBox dependencyActionContent() {
        return actionContent(
                fieldsGrid(
                        "Blocking Issue ID", blockingIssueIdField,
                        "Blocked Issue ID", blockedIssueIdField,
                        "Dependency ID", dependencyIdField),
                actionRow(
                        actionButton("Add", isPl(), () -> {
                            var result = issueController.addDependency(
                                    requiredLong(blockingIssueIdField, "blockingIssueId"),
                                    requiredLong(blockedIssueIdField, "blockedIssueId"));
                            dependencyIdField.setText(result.dependencyId());
                            return "Dependency added: " + result.dependencyId();
                        }),
                        actionButton("Remove", isPl(), () -> {
                            issueController.removeDependency(requiredText(dependencyIdField, "dependencyId"));
                            return "Dependency removed.";
                        })));
    }

    private VBox deletedAndStatisticsActionContent() {
        return actionContent(actionRow(
                actionButton("Soft Delete", canDeleteSelectedIssue(), () -> {
                    var issue = deletedIssueController.deleteIssue(
                            requiredLong(issueIdField, "issueId"),
                            requiredText(reasonArea, "reason"));
                    return "Issue deleted: " + issue.getIssueId() + " / " + issue.status();
                }),
                actionButton("Deleted List", isPl(), () -> deletedIssueController
                        .viewDeletedIssues(requiredLong(projectIdField, "projectId")).stream()
                        .map(issue -> issue.id() + " / " + issue.getIssueId() + " / " + issue.status())
                        .reduce("Deleted issues", (left, right) -> left + System.lineSeparator() + right)),
                actionButton("Restore", isPl(), () -> {
                    var issue = deletedIssueController.restoreIssue(
                            requiredLong(issueIdField, "issueId"),
                            requiredText(reasonArea, "reason"));
                    return "Issue restored: " + issue.getIssueId() + " / " + issue.status();
                }),
                actionButton("Purge Overflow", isPl(), () -> {
                    int count = deletedIssueController
                            .purgeOverflow(requiredLong(projectIdField, "projectId"));
                    return "Purged deleted issues: " + count;
                }),
                actionButton("Statistics", isIssueActor(), () -> formatStatistics(
                        statisticsController.viewStatistics(requiredLong(projectIdField, "projectId"))))));
    }

    private String changeStatus(IssueStatus targetStatus) {
        var result = issueStateController.changeStatus(
                requiredLong(issueIdField, "issueId"),
                targetStatus,
                requiredText(reasonArea, "reason"));
        return "Status changed: " + result.issueId() + " / " + result.status();
    }

    private Button actionButton(String text, boolean enabled, Supplier<String> action) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setOnAction(event -> runAction(action));
        return button;
    }

    private Button detailActionButton(String text, boolean enabled, Supplier<String> action) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setOnAction(event -> runDetailAction(action));
        return button;
    }

    private void runAction(Supplier<String> action) {
        try {
            refreshDashboard(action.get());
        } catch (RuntimeException exception) {
            workflowOutputArea.setText("Failed: " + exception.getMessage());
            workflowOutputArea.setStyle("-fx-text-fill: #b91c1c;");
        }
    }

    private void runDetailAction(Supplier<String> action) {
        try {
            refreshIssueDetail(action.get());
        } catch (RuntimeException exception) {
            detailOutputArea.setText("Failed: " + exception.getMessage());
            detailOutputArea.setStyle("-fx-text-fill: #b91c1c;");
        }
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
        detailOutputArea.setText(message);
        detailOutputArea.setStyle("-fx-text-fill: #111827;");
        if (selectedIssue == null) {
            refreshDashboard(message);
            return;
        }
        showIssueDetail(selectedIssue);
    }

    private void selectIssue(Issue issue) {
        selectedIssue = issue;
        clearDetailOutput();
        issueIdField.setText(String.valueOf(issue.id()));
        projectIdField.setText(String.valueOf(issue.projectId()));
        issueTitleField.setText(issue.title());
        issueDescriptionField.setText(issue.description());
        priorityBox.setValue(issue.priority());
        assigneeLoginIdField.setText(valueOrBlank(issue.assigneeId()));
        verifierLoginIdField.setText(valueOrBlank(issue.verifierId()));
        blockedIssueIdField.setText(String.valueOf(issue.id()));
        showIssueDetail(issue);
    }

    private void selectProject(DashboardProjectView project) {
        selectedProject = project;
        clearDetailOutput();
        projectIdField.setText(String.valueOf(project.projectId()));
        showProjectPage(project);
    }

    private void showIssueDetail(Issue issue) {
        root.setCenter(null);
        Label title = new Label("Issue Detail");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        Label body = new Label("""
                ID: %d
                Issue ID: %s
                Project ID: %d
                Title: %s
                Description: %s
                Reported Date: %s
                Priority: %s
                Status: %s
                Reporter Login ID: %s
                Assignee Login ID: %s
                Verifier Login ID: %s
                Fixer Login ID: %s
                Resolver Login ID: %s
                Updated At: %s
                """.formatted(
                issue.id(),
                issue.getIssueId(),
                issue.projectId(),
                issue.title(),
                issue.description(),
                issue.reportedDate(),
                issue.priority(),
                issue.status(),
                valueOrBlank(issue.reporterId()),
                valueOrBlank(issue.assigneeId()),
                valueOrBlank(issue.verifierId()),
                valueOrBlank(issue.fixerId()),
                valueOrBlank(issue.resolverId()),
                issue.updatedAt()));
        body.setStyle("-fx-font-size: 22px;");
        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> {
            clearWorkflowOutput();
            refreshDashboard("");
        });
        VBox box = new VBox(
                16,
                title,
                body,
                issueStatusActionPanel(),
                issueCommentsList(issue.id()),
                issueCommentActionPanel(),
                detailOutputArea,
                backButton);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(scroll(box));
    }

    private VBox issueStatusActionPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 1.5; -fx-background-color: #ffffff;");
        panel.getChildren().addAll(
                sectionLabel("Status Transition"),
                fieldsGrid("Status Reason", reasonArea),
                actionRow(
                        detailActionButton("ASSIGNED -> FIXED", canMarkFixed(),
                                () -> changeStatusInDetail(IssueStatus.FIXED)),
                        detailActionButton("FIXED -> ASSIGNED", canRejectFix(),
                                () -> changeStatusInDetail(IssueStatus.ASSIGNED)),
                        detailActionButton("FIXED -> RESOLVED", canResolve(),
                                () -> changeStatusInDetail(IssueStatus.RESOLVED)),
                        detailActionButton("RESOLVED -> CLOSED", canClose(),
                                () -> changeStatusInDetail(IssueStatus.CLOSED)),
                        detailActionButton("RESOLVED/CLOSED -> REOPENED", canReopen(),
                                () -> changeStatusInDetail(IssueStatus.REOPENED))));
        return panel;
    }

    private VBox issueCommentActionPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 1.5; -fx-background-color: #ffffff;");
        panel.getChildren().addAll(
                sectionLabel("Comments"),
                fieldsGrid("New Comment", commentArea),
                actionRow(
                        detailActionButton("Add Comment", isIssueActor(), () -> {
                            var comment = issueController.addComment(
                                    requiredLong(issueIdField, "issueId"),
                                    requiredText(commentArea, "comment"));
                            return "Comment added: " + comment.commentId() + " / " + comment.purpose();
                        })));
        return panel;
    }

    private VBox issueCommentsList(long issueId) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 1.5; -fx-background-color: #ffffff;");
        panel.getChildren().add(sectionLabel("Comment List"));
        List<CommentView> comments = issueController.viewComments(issueId);
        if (comments.isEmpty()) {
            panel.getChildren().add(emptyLabel("No comments."));
            return panel;
        }
        for (CommentView comment : comments) {
            panel.getChildren().add(commentCard(issueId, comment));
        }
        return panel;
    }

    private HBox commentCard(long issueId, CommentView comment) {
        boolean writer = isCommentWriter(comment);
        Label meta = new Label("ID %s / %s / %s / %s -> %s%s".formatted(
                comment.commentId(),
                comment.writerLoginId(),
                comment.purpose(),
                comment.createdDate(),
                comment.updatedDate(),
                writer ? " / editable" : " / read-only"));
        meta.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        TextArea editor = area("");
        editor.setText(comment.content());
        editor.setPromptText(writer ? "Edit this comment here, then press Update" : "Only the writer can edit");
        editor.setPrefRowCount(2);
        editor.setEditable(writer);
        editor.setStyle(writer
                ? "-fx-font-size: 18px; -fx-border-color: #2563eb;"
                : "-fx-font-size: 18px; -fx-control-inner-background: #f3f4f6;");

        VBox text = new VBox(6, meta, editor);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

        VBox buttons = new VBox(
                8,
                commentActionButton("Update Comment", writer, () -> {
                    var updated = issueController.updateComment(
                            issueId,
                            commentDatabaseId(comment),
                            requiredText(editor, "comment"));
                    return "Comment updated: " + updated.commentId();
                }),
                commentActionButton("Delete Comment", canDeleteComment(comment), () -> {
                    issueController.deleteComment(issueId, commentDatabaseId(comment));
                    return "General comment deleted: " + comment.commentId();
                }));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(12, text, buttons);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #d1d5db; -fx-background-color: #f9fafb;");
        return card;
    }

    private Button commentActionButton(String text, boolean enabled, Supplier<String> action) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setMinWidth(120);
        button.setOnAction(event -> runDetailAction(action));
        return button;
    }

    private boolean isCommentWriter(CommentView comment) {
        return currentUser != null && currentUser.getLoginId().equals(comment.writerLoginId());
    }

    private boolean canDeleteComment(CommentView comment) {
        return isCommentWriter(comment) && comment.purpose() == CommentPurpose.GENERAL;
    }

    private static long commentDatabaseId(CommentView comment) {
        try {
            return Long.parseLong(comment.commentId());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("commentId must be a persisted numeric id");
        }
    }

    private String changeStatusInDetail(IssueStatus targetStatus) {
        var result = issueStateController.changeStatus(
                requiredLong(issueIdField, "issueId"),
                targetStatus,
                requiredText(reasonArea, "reason"));
        return "Status changed: " + result.issueId() + " / " + result.status();
    }

    private void showProjectPage(DashboardProjectView project) {
        Label title = new Label(project.projectName());
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        Label description = new Label(valueOrBlank(project.projectDescription()).isBlank()
                ? "No description."
                : project.projectDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 22px;");
        Label body = new Label("""
                Project ID: %d
                Members: %d
                PL: %d
                DEV: %d
                TESTER: %d
                Visible issues: %d
                Deleted issues: %d
                Status counts: %s
                """.formatted(
                project.projectId(),
                project.memberCount(),
                project.projectLeaderCount(),
                project.developerCount(),
                project.testerCount(),
                project.visibleIssueCount(),
                project.deletedIssueCount(),
                project.statusCounts()));
        body.setStyle("-fx-font-size: 22px;");
        VBox registerIssuePanel = panel("Register Issue");
        registerIssuePanel.getChildren().addAll(
                fieldsGrid(
                        "Title", issueTitleField,
                        "Description", issueDescriptionField,
                        "Priority", priorityBox),
                actionRow(projectActionButton(project, "Register Issue", isIssueActor(), () -> {
                    var issue = issueController.registerIssue(
                            project.projectId(),
                            requiredText(issueTitleField, "issueTitle"),
                            requiredText(issueDescriptionField, "issueDescription"),
                            priorityBox.getValue());
                    return "Issue registered: " + issue.issueId() + " / " + issue.status();
                })));
        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> {
            clearWorkflowOutput();
            refreshDashboard("");
        });
        VBox box = new VBox(16, title, description, body, registerIssuePanel, detailOutputArea, backButton);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(scroll(box));
    }

    private Button projectActionButton(
            DashboardProjectView project,
            String text,
            boolean enabled,
            Supplier<String> action
    ) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setOnAction(event -> runProjectAction(project.projectId(), action));
        return button;
    }

    private void runProjectAction(long projectId, Supplier<String> action) {
        try {
            String message = action.get();
            refreshProjectPage(projectId, message);
        } catch (RuntimeException exception) {
            detailOutputArea.setText("Failed: " + exception.getMessage());
            detailOutputArea.setStyle("-fx-text-fill: #b91c1c;");
        }
    }

    private void refreshProjectPage(long projectId, String message) {
        currentProjects = dashboardController.viewProjects();
        selectedProject = currentProjects.stream()
                .filter(project -> project.projectId() == projectId)
                .findFirst()
                .orElse(null);
        detailOutputArea.setText(message);
        detailOutputArea.setStyle("-fx-text-fill: #111827;");
        if (selectedProject == null) {
            refreshDashboard(message);
            return;
        }
        showProjectPage(selectedProject);
    }

    private boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    private boolean isPl() {
        return currentUser != null && currentUser.getRole() == Role.PL;
    }

    private boolean isIssueActor() {
        return currentUser != null && currentUser.getRole() != Role.ADMIN;
    }

    private boolean canStartAssignment() {
        return isPl() && selectedIssue != null
                && switch (selectedIssue.status()) {
                    case NEW, REOPENED, ASSIGNED, FIXED -> true;
                    default -> false;
                };
    }

    private boolean canAssign() {
        return isPl() && selectedIssue != null
                && (selectedIssue.status() == IssueStatus.NEW || selectedIssue.status() == IssueStatus.REOPENED);
    }

    private boolean canReassign() {
        return isPl() && selectedIssue != null && selectedIssue.status() == IssueStatus.ASSIGNED;
    }

    private boolean canChangeVerifier() {
        return isPl() && selectedIssue != null && selectedIssue.status() == IssueStatus.FIXED;
    }

    private boolean canUpdateSelectedIssue() {
        return selectedIssue != null
                && currentUser.getLoginId().equals(selectedIssue.reporterId())
                && (selectedIssue.status() == IssueStatus.NEW || selectedIssue.status() == IssueStatus.REOPENED);
    }

    private boolean canChangePriority() {
        return isPl() && selectedIssue != null;
    }

    private boolean canMarkFixed() {
        return selectedIssue != null
                && selectedIssue.status() == IssueStatus.ASSIGNED
                && currentUser.getLoginId().equals(selectedIssue.assigneeId());
    }

    private boolean canRejectFix() {
        return selectedIssue != null
                && selectedIssue.status() == IssueStatus.FIXED
                && currentUser.getLoginId().equals(selectedIssue.verifierId());
    }

    private boolean canResolve() {
        return selectedIssue != null
                && selectedIssue.status() == IssueStatus.FIXED
                && currentUser.getLoginId().equals(selectedIssue.verifierId());
    }

    private boolean canClose() {
        return isPl() && selectedIssue != null && selectedIssue.status() == IssueStatus.RESOLVED;
    }

    private boolean canReopen() {
        return isPl() && selectedIssue != null
                && (selectedIssue.status() == IssueStatus.RESOLVED || selectedIssue.status() == IssueStatus.CLOSED);
    }

    private boolean canDeleteSelectedIssue() {
        return isPl() && selectedIssue != null
                && (selectedIssue.status() == IssueStatus.NEW || selectedIssue.status() == IssueStatus.CLOSED);
    }

    private static String formatAssignmentOptions(AssignmentOptions options) {
        return "DEV candidates: " + formatCandidates(options.devAssigneeCandidates())
                + System.lineSeparator()
                + "TESTER candidates: " + formatCandidates(options.testerVerifierCandidates());
    }

    private static List<String> formatCandidates(List<AssignmentCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.user().getLoginId() + "(" + candidate.completedIssueCount() + ")")
                .toList();
    }

    private static String formatStatistics(StatisticsReport report) {
        return "Status counts: " + report.statusCounts()
                + System.lineSeparator()
                + "Priority counts: " + report.priorityCounts()
                + System.lineSeparator()
                + "Daily counts: " + report.dailyCounts()
                + System.lineSeparator()
                + "Monthly counts: " + report.monthlyCounts();
    }

    private static String formatAccount(User user) {
        return "%s / %s / %s / %s".formatted(
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.isActive() ? "ACTIVE" : "INACTIVE");
    }

    private static Tab actionTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private static VBox actionContent(Node... nodes) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14));
        box.getChildren().addAll(nodes);
        return box;
    }

    private static HBox actionRow(Node... buttons) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(buttons);
        return row;
    }

    private static GridPane fieldsGrid(Object... labelsAndFields) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        for (int index = 0; index < labelsAndFields.length; index += 2) {
            int row = index / 2;
            grid.add(new Label((String) labelsAndFields[index]), 0, row);
            grid.add((Node) labelsAndFields[index + 1], 1, row);
        }
        return grid;
    }

    private static void addInput(GridPane grid, int column, int row, String label, Node field) {
        grid.add(new Label(label), column, row);
        grid.add(field, column + 1, row);
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefColumnCount(12);
        return field;
    }

    private static TextArea area(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefColumnCount(24);
        area.setPrefRowCount(2);
        area.setWrapText(true);
        return area;
    }

    private static VBox panel(String title) {
        Label titleLabel = sectionLabel(title);
        VBox list = new VBox(12, titleLabel);
        list.setPadding(new Insets(16));
        return list;
    }

    private static VBox scrollablePanel(VBox list) {
        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(500);
        scrollPane.setPrefViewportHeight(420);
        VBox container = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        container.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #f9fafb;");
        return container;
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    private static Button card(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #4b5563;
                -fx-border-width: 1.5;
                -fx-padding: 14;
                -fx-font-size: 20px;
                """);
        return button;
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        return label;
    }

    private static Label emptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #6b7280;");
        return label;
    }

    private void clearWorkflowInputs() {
        projectIdField.clear();
        projectNameField.clear();
        projectDescriptionField.clear();
        participantLoginIdField.clear();
        createAccountLoginIdField.clear();
        createAccountNameField.clear();
        createAccountPasswordField.clear();
        createAccountRoleBox.setValue(Role.DEV);
        updateAccountLoginIdField.clear();
        updateAccountNameField.clear();
        updateAccountRoleBox.setValue(Role.DEV);
        accountStatusLoginIdField.clear();
        issueIdField.clear();
        issueTitleField.clear();
        issueDescriptionField.clear();
        priorityBox.setValue(Priority.MAJOR);
        assigneeLoginIdField.clear();
        verifierLoginIdField.clear();
        blockingIssueIdField.clear();
        blockedIssueIdField.clear();
        dependencyIdField.clear();
        commentIdField.clear();
        commentArea.clear();
        reasonArea.clear();
    }

    private static long requiredLong(TextField field, String fieldName) {
        try {
            return Long.parseLong(requiredText(field, fieldName));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
    }

    private static String requiredText(TextInputControl field, String fieldName) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String text(TextInputControl field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
