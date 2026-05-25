package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
// import java.time.LocalDate;
import java.time.YearMonth;
// import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
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
    private final DeletedIssueController deletedIssueController;
    private final StatisticsController statisticsController;
    private final Consumer<IssueDetailResult> onIssueSelected;
    private final Consumer<DashboardProjectView> onProjectSelected;
    private final Consumer<String> onDashboardChanged;
    private final BiConsumer<Long, String> onProjectChanged;
    private final TextField projectNameField = field("new project name");
    private final TextField projectDescriptionField = field("project description");
    private final TextField participantLoginIdField = field("participant loginId");
    private final TextField issueTitleField = field("issue title");
    private final TextArea issueDescriptionArea = area("issue description");
    private final ComboBox<Priority> priorityBox = new ComboBox<>();
    private final TextField issueKeywordField = field("title or description");
    private final TextField issueReporterFilterField = field("reporter loginId");
    private final TextField issueAssigneeFilterField = field("assignee loginId");
    private final TextField issueVerifierFilterField = field("verifier loginId");
    private final ComboBox<String> issueStatusFilterBox = new ComboBox<>();
    private final ComboBox<String> issuePriorityFilterBox = new ComboBox<>();
    private final TextField blockingIssueIdField = field("blocking issue id");
    private final TextField blockedIssueIdField = field("blocked issue id");
    private final TextField dependencyIdField = field("dependency id");
    private final TextArea deletedIssueReasonArea = area("restore/delete reason");
    // private final TextField dailyFromField = field("yyyy-mm-dd");
    // private final TextField dailyToField = field("yyyy-mm-dd");
    // private final TextField monthlyFromField = field("yyyy-mm");
    // private final TextField monthlyToField = field("yyyy-mm");

    public ProjectBoardView(
            User currentUser,
            ProjectController projectController,
            IssueController issueController,
            DeletedIssueController deletedIssueController,
            StatisticsController statisticsController,
            Consumer<IssueDetailResult> onIssueSelected,
            Consumer<DashboardProjectView> onProjectSelected,
            Consumer<String> onDashboardChanged,
            BiConsumer<Long, String> onProjectChanged) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.deletedIssueController = Objects.requireNonNull(deletedIssueController, "deletedIssueController");
        this.statisticsController = Objects.requireNonNull(statisticsController, "statisticsController");
        this.onIssueSelected = Objects.requireNonNull(onIssueSelected, "onIssueSelected");
        this.onProjectSelected = Objects.requireNonNull(onProjectSelected, "onProjectSelected");
        this.onDashboardChanged = Objects.requireNonNull(onDashboardChanged, "onDashboardChanged");
        this.onProjectChanged = Objects.requireNonNull(onProjectChanged, "onProjectChanged");
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(Priority.MAJOR);
        issueStatusFilterBox.getItems().setAll(filterValues(IssueStatus.values()));
        issueStatusFilterBox.setValue("ALL");
        issuePriorityFilterBox.getItems().setAll(filterValues(Priority.values()));
        issuePriorityFilterBox.setValue("ALL");
    }

    public Parent dashboard(
            List<IssueSummary> issues,
            List<DashboardProjectView> projects,
            List<User> users,
            Consumer<User> onAccountSelected,
            Node adminAccountManagement) {
        HBox lists = isAdmin()
                ? new HBox(16, projectList(projects), userList(users, onAccountSelected))
                : new HBox(16, projectList(projects));
        lists.setAlignment(Pos.CENTER_LEFT);
        lists.setFillHeight(true);

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

    public Parent projectDetail(
            DashboardProjectView project,
            List<User> users,
            String message,
            Runnable onBack) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(users, "users");
        Objects.requireNonNull(onBack, "onBack");
        Label title = sectionLabel(project.projectName());
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        Label description = new Label(valueOrBlank(project.projectDescription()).isBlank()
                ? "No description."
                : project.projectDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 22px;");
        Label summary = new Label(projectMembershipSummary(project));
        summary.setStyle("-fx-font-size: 22px;");

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> onBack.run());

        VBox root = new VBox(
                16,
                actionRow(backButton),
                title,
                description,
                summary);
        if (isAdmin()) {
            root.getChildren().add(projectMemberPanel(project, users, message));
        } else {
            root.getChildren().addAll(
                    relatedProjectIssuesPanel(project),
                    projectIssueSearchPanel(project),
                    registerIssuePanel(project, message),
                    statisticsPanel(project));
            if (isProjectLead()) {
                root.getChildren().addAll(
                        dependencyManagementPanel(project, message),
                        deletedIssueManagementPanel(project, message));
            }
        }
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        return root;
    }

    private VBox projectList(List<DashboardProjectView> projects) {
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
        return scrollablePanel(list);
    }

    private VBox userList(List<User> users, Consumer<User> onAccountSelected) {
        VBox list = panel("Users");
        if (users.isEmpty()) {
            list.getChildren().add(emptyLabel("No users to show."));
        }
        for (User user : users) {
            Button button = card(formatAccount(user));
            button.setOnAction(event -> onAccountSelected.accept(user));
            list.getChildren().add(button);
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

    private VBox projectMemberPanel(DashboardProjectView project, List<User> users, String message) {
        VBox box = borderedPanel("Project Management");
        box.getChildren().addAll(
                inputWithMessage(
                        fieldsGrid("Participant Login ID", participantLoginIdField),
                        message),
                actionRow(
                        actionButton("Add Member", isAdmin(), () -> {
                            projectController.addProjectParticipant(
                                    project.projectId(),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant added.";
                        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage)),
                        actionButton("Remove Member", isAdmin(), () -> {
                            projectController.removeProjectParticipant(
                                    project.projectId(),
                                    requiredText(participantLoginIdField, "participant"));
                            return "Project participant removed.";
                        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage)),
                        actionButton("Delete Project", isAdmin(), () -> {
                            projectController.deleteProject(project.projectId());
                            return "Project deleted.";
                        }, onDashboardChanged)),
                projectParticipantsList(project, users));
        return box;
    }

    private VBox projectParticipantsList(DashboardProjectView project, List<User> users) {
        VBox list = new VBox(8, sectionLabel("Participants"));
        try {
            List<ProjectMember> participants = projectController.viewProjectParticipants(project.projectId());
            if (participants.isEmpty()) {
                list.getChildren().add(emptyLabel("No participants."));
                return list;
            }
            for (ProjectMember participant : participants) {
                list.getChildren().add(infoCard(formatParticipant(participant, users)));
            }
        } catch (RuntimeException exception) {
            list.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
        }
        return list;
    }

    private String projectMembershipSummary(DashboardProjectView project) {
        return """
                Project ID: %d
                Members: %d
                PL: %d
                DEV: %d
                TESTER: %d
                """.formatted(
                project.projectId(),
                project.memberCount(),
                project.projectLeaderCount(),
                project.developerCount(),
                project.testerCount());
    }

    private VBox registerIssuePanel(DashboardProjectView project, String message) {
        VBox box = borderedPanel("Register Issue");
        box.getChildren().addAll(
                inputWithMessage(
                        fieldsGrid(
                                "Title", issueTitleField,
                                "Description", issueDescriptionArea,
                                "Priority", priorityBox),
                        message),
                actionRow(actionButton("Register Issue", issueController.canRegisterIssue(project.projectId()), () -> {
                    var issue = issueController.registerIssue(
                            project.projectId(),
                            requiredText(issueTitleField, "issueTitle"),
                            requiredText(issueDescriptionArea, "issueDescription"),
                            priorityBox.getValue());
                    return "Issue registered: " + issue.issueId() + " / " + issue.status();
                }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage))));
        return box;
    }

    private VBox relatedProjectIssuesPanel(DashboardProjectView project) {
        VBox box = borderedPanel("My Related Issues");
        try {
            List<IssueSummary> issues = issueController.viewRelatedProjectIssues(project.projectId());
            if (issues.isEmpty()) {
                box.getChildren().add(emptyLabel("No related issues in this project."));
                return box;
            }
            for (IssueSummary issue : issues) {
                box.getChildren().add(issueCard(issue));
            }
        } catch (RuntimeException exception) {
            box.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
        }
        return box;
    }

    private VBox projectIssueSearchPanel(DashboardProjectView project) {
        VBox box = borderedPanel("Project Issues");
        VBox results = new VBox(10);
        box.getChildren().addAll(
                fieldsGrid(
                        "Keyword", issueKeywordField,
                        "Status", issueStatusFilterBox,
                        "Priority", issuePriorityFilterBox,
                        "Reporter", issueReporterFilterField,
                        "Assignee", issueAssigneeFilterField,
                        "Verifier", issueVerifierFilterField),
                actionRow(actionButton("Search Issues", true, () -> {
                    renderIssueSearchResults(project, results);
                    return "Search completed.";
                }, ignored -> {
                })),
                results);
        results.getChildren().add(emptyLabel("Use search to find project issues."));
        return box;
    }

    private VBox dependencyManagementPanel(DashboardProjectView project, String message) {
        VBox box = borderedPanel("Dependency Management");
        box.getChildren().addAll(
                inputWithMessage(
                        fieldsGrid(
                                "Blocking Issue ID", blockingIssueIdField,
                                "Blocked Issue ID", blockedIssueIdField,
                                "Dependency ID", dependencyIdField),
                        message),
                actionRow(
                        actionButton("Add Dependency", isProjectLead(), () -> {
                            var dependency = issueController.addDependency(
                                    requiredLong(blockingIssueIdField, "blockingIssueId"),
                                    requiredLong(blockedIssueIdField, "blockedIssueId"));
                            dependencyIdField.setText(dependency.dependencyId());
                            return "Dependency added: " + dependency.dependencyId();
                        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage)),
                        actionButton("Remove Dependency", isProjectLead(), () -> {
                            issueController.removeDependency(requiredText(dependencyIdField, "dependencyId"));
                            return "Dependency removed.";
                        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage))));
        return box;
    }

    private VBox deletedIssueManagementPanel(DashboardProjectView project, String message) {
        VBox box = borderedPanel("Deleted Issue Management");
        box.getChildren().add(inputWithMessage(
                fieldsGrid("Reason", deletedIssueReasonArea),
                message));
        box.getChildren().add(actionRow(actionButton("Purge Overflow", isProjectLead(), () -> {
            int purged = deletedIssueController.purgeOverflow(project.projectId());
            return "Deleted issue overflow purged: " + purged;
        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage))));
        try {
            List<IssueSummary> deletedIssues = deletedIssueController.viewDeletedIssues(project.projectId());
            if (deletedIssues.isEmpty()) {
                box.getChildren().add(emptyLabel("No deleted issues in this project."));
                return box;
            }
            for (IssueSummary issue : deletedIssues) {
                box.getChildren().add(deletedIssueCard(project, issue));
            }
        } catch (RuntimeException exception) {
            box.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
        }
        return box;
    }

    private HBox deletedIssueCard(DashboardProjectView project, IssueSummary issue) {
        Label detail = infoCard("""
                ID=%d / issueId=%s
                %s
                reporter=%s
                %s / %s / updated=%s
                """.formatted(
                issue.id(),
                issue.issueId(),
                issue.title(),
                issue.reporterId(),
                issue.status(),
                issue.priority(),
                issue.updatedAt()));
        HBox.setHgrow(detail, javafx.scene.layout.Priority.ALWAYS);
        Button restoreButton = actionButton("Restore", isProjectLead(), () -> {
            IssueSummary restored = deletedIssueController.restoreIssue(
                    issue.id(),
                    requiredText(deletedIssueReasonArea, "reason"));
            return "Issue restored: " + restored.issueId() + " / " + restored.status();
        }, resultMessage -> onProjectChanged.accept(project.projectId(), resultMessage));
        HBox row = new HBox(12, detail, restoreButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void renderIssueSearchResults(DashboardProjectView project, VBox results) {
        results.getChildren().clear();
        try {
            List<IssueSummary> issues = issueController.searchIssues(
                    project.projectId(),
                    text(issueKeywordField),
                    selectedStatus(),
                    selectedPriorityFilter(),
                    text(issueReporterFilterField),
                    text(issueAssigneeFilterField),
                    text(issueVerifierFilterField));
            if (issues.isEmpty()) {
                results.getChildren().add(emptyLabel("No project issues match the current filters."));
                return;
            }
            for (IssueSummary issue : issues) {
                results.getChildren().add(issueSummaryCard(issue));
            }
        } catch (RuntimeException exception) {
            results.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
        }
    }

    private Button issueCard(IssueSummary issue) {
        Button button = card("""
                ID=%d
                %s
                reporter=%s
                %s / %s
                """.formatted(
                issue.id(),
                issue.title(),
                issue.reporterId(),
                issue.status(),
                issue.priority()));
        button.setOnAction(event -> onIssueSelected.accept(issueController.viewIssueDetail(issue.id())));
        return button;
    }

    private Button issueSummaryCard(IssueSummary issue) {
        Button button = card("""
                ID=%d
                %s
                reporter=%s
                %s / %s
                """.formatted(
                issue.id(),
                issue.title(),
                issue.reporterId(),
                issue.status(),
                issue.priority()));
        button.setOnAction(event -> onIssueSelected.accept(issueController.viewIssueDetail(issue.id())));
        return button;
    }

    private VBox statisticsPanel(DashboardProjectView project) {
        VBox box = borderedPanel("Statistics");
        if (!statisticsController.canViewStatistics(project.projectId())) {
            box.getChildren().add(emptyLabel("Statistics are not available for this account."));
        } else {
            try {
                box.getChildren().add(statisticsCharts(
                        statisticsController.viewStatistics(project.projectId()),
                        project.deletedIssueCount()));
            } catch (RuntimeException exception) {
                box.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
            }
        }
        return box;
    }

    private Button actionButton(
            String text,
            boolean enabled,
            Supplier<String> action,
            Consumer<String> onSuccess) {
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

    private boolean isProjectLead() {
        return currentUser.getRole() == Role.PL;
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

    private static HBox inputWithMessage(Node input, String message) {
        Label label = messageLabel(message);
        label.setMinWidth(360);
        HBox row = new HBox(16, input, label);
        row.setAlignment(Pos.CENTER_LEFT);
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
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);
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

    private static Label infoCard(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        long lineCount = Math.max(1L, text.lines().count());
        label.setMinHeight(22 + lineCount * 26);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #d1d5db;
                -fx-border-width: 1;
                -fx-padding: 10;
                -fx-font-size: 18px;
                """);
        return label;
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

    private static long requiredLong(TextInputControl field, String fieldName) {
        try {
            return Long.parseLong(requiredText(field, fieldName));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
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

    private static String formatParticipant(ProjectMember participant, List<User> users) {
        return users.stream()
                .filter(user -> user.getLoginId().equals(participant.userId()))
                .findFirst()
                .map(user -> """
                        %s / %s / %s
                        joined=%s
                        """.formatted(
                        user.getLoginId(),
                        user.getName(),
                        user.getRole(),
                        participant.joinedAt()))
                .orElse("""
                        %s / unknown / unknown
                        joined=%s
                        """.formatted(
                        participant.userId(),
                        participant.joinedAt()));
    }

    private IssueStatus selectedStatus() {
        String value = issueStatusFilterBox.getValue();
        if (value == null || "ALL".equals(value)) {
            return null;
        }
        return IssueStatus.valueOf(value);
    }

    private Priority selectedPriorityFilter() {
        String value = issuePriorityFilterBox.getValue();
        if (value == null || "ALL".equals(value)) {
            return null;
        }
        return Priority.valueOf(value);
    }

    private static <E extends Enum<E>> List<String> filterValues(E[] values) {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        items.add("ALL");
        for (E value : values) {
            items.add(value.name());
        }
        return List.copyOf(items);
    }

    private static VBox statisticsCharts(StatisticsReport report, int deletedIssueCount) {
        VBox charts = new VBox(16);
        charts.getChildren().add(summaryCards(report, deletedIssueCount));
        charts.getChildren().addAll(
                chartRow(
                        pieChart("Current Status Counts", statusPieData(report, deletedIssueCount)),
                        pieChart("Current Priority Counts", priorityPieData(report))),
                chartRow(
                        dailyBarChart("Daily Issue Registrations", report.dailyCounts()),
                        monthlyBarChart("Monthly Issue Registrations", report.monthlyCounts())),
                chartRow(
                        dailyBarChart("Daily Status Changes", report.dailyStatusChangeCounts()),
                        monthlyBarChart("Monthly Status Changes", report.monthlyStatusChangeCounts())),
                chartRow(
                        dailyBarChart("Daily Comments", report.dailyCommentCounts()),
                        monthlyBarChart("Monthly Comments", report.monthlyCommentCounts())),
                chartRow(
                        monthlyStatusBarChart(report),
                        monthlyPriorityBarChart(report)));
        return charts;
    }

    private static HBox summaryCards(StatisticsReport report, int deletedIssueCount) {
        HBox row = new HBox(12);
        row.getChildren().addAll(
                metricCard("Total Issues", totalVisibleIssues(report)),
                metricCard("Deleted Issues", deletedIssueCount),
                metricCard("Total Status Changes", totalDailyCounts(report.dailyStatusChangeCounts())),
                metricCard("Total Comments", totalDailyCounts(report.dailyCommentCounts())));
        return row;
    }

    private static Label metricCard(String label, int value) {
        Label card = new Label(label + System.lineSeparator() + value);
        card.setMinWidth(180);
        card.setStyle("""
                -fx-background-color: #f8fafc;
                -fx-border-color: #cbd5e1;
                -fx-border-width: 1;
                -fx-padding: 12;
                -fx-font-size: 18px;
                """);
        return card;
    }

    private static HBox chartRow(Node left, Node right) {
        HBox row = new HBox(14, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static int totalVisibleIssues(StatisticsReport report) {
        return report.statusCounts().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private static int totalDailyCounts(List<DailyIssueCount> counts) {
        return counts.stream()
                .mapToInt(DailyIssueCount::count)
                .sum();
    }

    private static PieChart pieChart(String title, List<PieChart.Data> data) {
        PieChart chart = new PieChart();
        chart.setTitle(title);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setMinHeight(300);
        chart.setPrefWidth(460);
        chart.setMaxWidth(460);
        chart.getData().setAll(data);
        if (data.isEmpty()) {
            chart.setTitle(title + " (no data)");
        }
        return chart;
    }

    private static List<PieChart.Data> statusPieData(StatisticsReport report, int deletedIssueCount) {
        List<PieChart.Data> data = new ArrayList<>();
        for (IssueStatus status : IssueStatus.values()) {
            int count = status == IssueStatus.DELETED
                    ? deletedIssueCount
                    : report.statusCounts().getOrDefault(status, 0);
            if (count > 0) {
                data.add(new PieChart.Data(pieLabel(status.name(), count), count));
            }
        }
        return data;
    }

    private static List<PieChart.Data> priorityPieData(StatisticsReport report) {
        List<PieChart.Data> data = new ArrayList<>();
        for (Priority priority : Priority.values()) {
            int count = report.priorityCounts().getOrDefault(priority, 0);
            if (count > 0) {
                data.add(new PieChart.Data(pieLabel(priority.name(), count), count));
            }
        }
        return data;
    }

    private static String pieLabel(String label, int count) {
        return label + ": " + count;
    }

    private static BarChart<String, Number> dailyBarChart(String title, List<DailyIssueCount> counts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Count");
        for (DailyIssueCount count : counts) {
            series.getData().add(new XYChart.Data<>(count.date().toString(), count.count()));
        }
        return singleSeriesBarChart(title, series);
    }

    private static BarChart<String, Number> monthlyBarChart(String title, List<MonthlyIssueCount> counts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Count");
        for (MonthlyIssueCount count : counts) {
            series.getData().add(new XYChart.Data<>(count.month().toString(), count.count()));
        }
        return singleSeriesBarChart(title, series);
    }

    private static BarChart<String, Number> singleSeriesBarChart(
            String title,
            XYChart.Series<String, Number> series) {
        BarChart<String, Number> chart = barChart(title);
        chart.setLegendVisible(false);
        chart.getData().add(series);
        if (series.getData().isEmpty()) {
            chart.setTitle(title + " (no data)");
        }
        return chart;
    }

    private static BarChart<String, Number> monthlyStatusBarChart(StatisticsReport report) {
        BarChart<String, Number> chart = barChart("Monthly Status Counts");
        for (IssueStatus status : IssueStatus.values()) {
            if (status == IssueStatus.DELETED) {
                continue;
            }
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(status.name());
            for (Map.Entry<YearMonth, Map<IssueStatus, Integer>> entry : report.monthlyStatusCounts().entrySet()) {
                series.getData().add(new XYChart.Data<>(
                        entry.getKey().toString(),
                        entry.getValue().getOrDefault(status, 0)));
            }
            chart.getData().add(series);
        }
        if (report.monthlyStatusCounts().isEmpty()) {
            chart.setTitle("Monthly Status Counts (no data)");
        }
        return chart;
    }

    private static BarChart<String, Number> monthlyPriorityBarChart(StatisticsReport report) {
        BarChart<String, Number> chart = barChart("Monthly Priority Counts");
        for (Priority priority : Priority.values()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(priority.name());
            for (Map.Entry<YearMonth, Map<Priority, Integer>> entry : report.monthlyPriorityCounts().entrySet()) {
                series.getData().add(new XYChart.Data<>(
                        entry.getKey().toString(),
                        entry.getValue().getOrDefault(priority, 0)));
            }
            chart.getData().add(series);
        }
        if (report.monthlyPriorityCounts().isEmpty()) {
            chart.setTitle("Monthly Priority Counts (no data)");
        }
        return chart;
    }

    private static BarChart<String, Number> barChart(String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setMinHeight(300);
        chart.setPrefWidth(460);
        chart.setMaxWidth(460);
        chart.setCategoryGap(40);
        chart.setBarGap(8);
        return chart;
    }

    // private static LocalDate optionalDate(TextInputControl field, String
    // fieldName) {
    // String value = text(field);
    // if (value.isBlank()) {
    // return null;
    // }
    // try {
    // return LocalDate.parse(value);
    // } catch (DateTimeParseException exception) {
    // throw new IllegalArgumentException(fieldName + " must use yyyy-mm-dd");
    // }
    // }

    // private static YearMonth optionalMonth(TextInputControl field, String
    // fieldName) {
    // String value = text(field);
    // if (value.isBlank()) {
    // return null;
    // }
    // try {
    // return YearMonth.parse(value);
    // } catch (DateTimeParseException exception) {
    // throw new IllegalArgumentException(fieldName + " must use yyyy-mm");
    // }
    // }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
