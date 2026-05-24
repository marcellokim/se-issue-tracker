package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class ProjectBoardView {

    private final User currentUser;
    private final ProjectController projectController;
    private final IssueController issueController;
    private final Consumer<Issue> onIssueSelected;
    private final Consumer<DashboardProjectView> onProjectSelected;
    private final Consumer<String> onDashboardChanged;
    private final BiConsumer<Long, String> onProjectChanged;
    private final TextField projectNameField = field("new project name");
    private final TextField projectDescriptionField = field("project description");
    private final TextField participantLoginIdField = field("participant loginId");
    private final TextField issueTitleField = field("issue title");
    private final TextArea issueDescriptionArea = area("issue description");
    private final ComboBox<Priority> priorityBox = new ComboBox<>();

    public ProjectBoardView(
            User currentUser,
            ProjectController projectController,
            IssueController issueController,
            Consumer<Issue> onIssueSelected,
            Consumer<DashboardProjectView> onProjectSelected,
            Consumer<String> onDashboardChanged,
            BiConsumer<Long, String> onProjectChanged
    ) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.onIssueSelected = Objects.requireNonNull(onIssueSelected, "onIssueSelected");
        this.onProjectSelected = Objects.requireNonNull(onProjectSelected, "onProjectSelected");
        this.onDashboardChanged = Objects.requireNonNull(onDashboardChanged, "onDashboardChanged");
        this.onProjectChanged = Objects.requireNonNull(onProjectChanged, "onProjectChanged");
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.MAJOR);
    }

    public Parent dashboard(
            List<Issue> issues,
            List<DashboardProjectView> projects,
            List<User> users,
            Consumer<User> onAccountSelected,
            Node adminAccountManagement
    ) {
        HBox lists = new HBox(16, issueList(issues), projectList(projects, users, onAccountSelected));
        lists.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, lists);
        if (isAdmin()) {
            root.getChildren().add(projectCreatePanel());
            if (adminAccountManagement != null) {
                root.getChildren().add(adminAccountManagement);
            }
        }
        root.setAlignment(Pos.TOP_LEFT);
        return root;
    }

    public Parent projectDetail(DashboardProjectView project, String message, Runnable onBack) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(onBack, "onBack");
        Label title = sectionLabel(project.projectName());
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        Label description = new Label(valueOrBlank(project.projectDescription()).isBlank()
                ? "No description."
                : project.projectDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 22px;");
        Label summary = new Label("""
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
        summary.setStyle("-fx-font-size: 22px;");

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> onBack.run());

        VBox root = new VBox(
                16,
                title,
                description,
                summary,
                projectMemberPanel(project),
                registerIssuePanel(project),
                messageLabel(message),
                backButton);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        return root;
    }

    private VBox issueList(List<Issue> issues) {
        VBox list = panel(currentUser.getRole() == Role.ADMIN ? "All Issues" : "Related Issues");
        if (issues.isEmpty()) {
            list.getChildren().add(emptyLabel("No issues to show."));
        }
        for (Issue issue : issues) {
            Button button = card("""
                    %s
                    id=%d / project=%d
                    %s / %s
                    """.formatted(issue.title(), issue.id(), issue.projectId(), issue.status(), issue.priority()));
            button.setOnAction(event -> onIssueSelected.accept(issue));
            list.getChildren().add(button);
        }
        return scrollablePanel(list);
    }

    private VBox projectList(
            List<DashboardProjectView> projects,
            List<User> users,
            Consumer<User> onAccountSelected
    ) {
        VBox list = panel(currentUser.getRole() == Role.ADMIN ? "All Projects" : "My Projects");
        if (projects.isEmpty()) {
            list.getChildren().add(emptyLabel("No projects to show."));
        }
        for (DashboardProjectView project : projects) {
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
            button.setOnAction(event -> onProjectSelected.accept(project));
            list.getChildren().add(button);
        }
        if (isAdmin() && !users.isEmpty()) {
            list.getChildren().add(sectionLabel("Users"));
            for (User user : users) {
                Button button = card(formatAccount(user));
                button.setOnAction(event -> onAccountSelected.accept(user));
                list.getChildren().add(button);
            }
        }
        return scrollablePanel(list);
    }

    private VBox projectCreatePanel() {
        VBox box = borderedPanel("Project Create");
        box.getChildren().addAll(
                fieldsGrid(
                        "Project Name", projectNameField,
                        "Description", projectDescriptionField),
                actionRow(actionButton("Create Project", true, () -> {
                    var project = projectController.createProject(
                            requiredText(projectNameField, "projectName"),
                            text(projectDescriptionField));
                    return "Project created: " + project.getId() + " / " + project.getName();
                }, onDashboardChanged)));
        return box;
    }

    private VBox projectMemberPanel(DashboardProjectView project) {
        VBox box = borderedPanel("Project Management");
        box.getChildren().addAll(
                fieldsGrid("Participant Login ID", participantLoginIdField),
                actionRow(
                        actionButton("Add Member", isAdmin(), () -> {
                            projectController.addProjectParticipant(
                                    project.projectId(),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant added.";
                        }, message -> onProjectChanged.accept(project.projectId(), message)),
                        actionButton("Remove Member", isAdmin(), () -> {
                            projectController.removeProjectParticipant(
                                    project.projectId(),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant removed.";
                        }, message -> onProjectChanged.accept(project.projectId(), message)),
                        actionButton("Delete Project", isAdmin(), () -> {
                            projectController.deleteProject(project.projectId());
                            return "Project deleted.";
                        }, onDashboardChanged)));
        return box;
    }

    private VBox registerIssuePanel(DashboardProjectView project) {
        VBox box = borderedPanel("Register Issue");
        box.getChildren().addAll(
                fieldsGrid(
                        "Title", issueTitleField,
                        "Description", issueDescriptionArea,
                        "Priority", priorityBox),
                actionRow(actionButton("Register Issue", isIssueActor(), () -> {
                    var issue = issueController.registerIssue(
                            project.projectId(),
                            requiredText(issueTitleField, "issueTitle"),
                            requiredText(issueDescriptionArea, "issueDescription"),
                            priorityBox.getValue());
                    return "Issue registered: " + issue.issueId() + " / " + issue.status();
                }, message -> onProjectChanged.accept(project.projectId(), message))));
        return box;
    }

    private Button actionButton(
            String text,
            boolean enabled,
            Supplier<String> action,
            Consumer<String> onSuccess
    ) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setOnAction(event -> run(action, onSuccess));
        return button;
    }

    private void run(Supplier<String> action, Consumer<String> onSuccess) {
        try {
            onSuccess.accept(action.get());
        } catch (RuntimeException exception) {
            onSuccess.accept("Failed: " + exception.getMessage());
        }
    }

    private boolean isAdmin() {
        return currentUser.getRole() == Role.ADMIN;
    }

    private boolean isIssueActor() {
        return currentUser.getRole() != Role.ADMIN;
    }

    private static VBox borderedPanel(String title) {
        VBox box = panel(title);
        box.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #ffffff;");
        return box;
    }

    private static VBox panel(String title) {
        VBox box = new VBox(12, sectionLabel(title));
        box.setPadding(new Insets(16));
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

    private static Label messageLabel(String message) {
        Label label = new Label(valueOrBlank(message));
        label.setMinHeight(36);
        label.setWrapText(true);
        label.setStyle(valueOrBlank(message).startsWith("Failed:")
                ? "-fx-text-fill: #b91c1c; -fx-font-size: 17px;"
                : "-fx-text-fill: #111827; -fx-font-size: 17px;");
        return label;
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefColumnCount(16);
        return field;
    }

    private static TextArea area(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefColumnCount(30);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        return area;
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

    private static String formatAccount(User user) {
        return "%s / %s / %s / %s".formatted(
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.isActive() ? "ACTIVE" : "INACTIVE");
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
