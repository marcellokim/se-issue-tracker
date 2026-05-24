package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.DashboardController.DashboardProjectView;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
    private final StatisticsController statisticsController;
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
    private final TextField issueKeywordField = field("title or description");
    private final ComboBox<String> issueStatusFilterBox = new ComboBox<>();
    private final ComboBox<String> issuePriorityFilterBox = new ComboBox<>();
    private final TextField dailyFromField = field("yyyy-mm-dd");
    private final TextField dailyToField = field("yyyy-mm-dd");
    private final TextField monthlyFromField = field("yyyy-mm");
    private final TextField monthlyToField = field("yyyy-mm");
    private final TextArea statisticsOutputArea = area("");

    public ProjectBoardView(
            User currentUser,
            ProjectController projectController,
            IssueController issueController,
            StatisticsController statisticsController,
            Consumer<Issue> onIssueSelected,
            Consumer<DashboardProjectView> onProjectSelected,
            Consumer<String> onDashboardChanged,
            BiConsumer<Long, String> onProjectChanged
    ) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
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
            List<Issue> issues,
            List<DashboardProjectView> projects,
            List<User> users,
            Consumer<User> onAccountSelected,
            Node adminAccountManagement
    ) {
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
            Runnable onBack
    ) {
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
        Label summary = new Label(projectSummary(project));
        summary.setStyle("-fx-font-size: 22px;");

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> onBack.run());

        VBox root = new VBox(
                16,
                title,
                description,
                summary);
        if (isAdmin()) {
            root.getChildren().add(projectMemberPanel(project, users));
        } else {
            root.getChildren().addAll(
                    relatedProjectIssuesPanel(project),
                    projectIssueSearchPanel(project),
                    registerIssuePanel(project),
                    statisticsPanel(project));
        }
        root.getChildren().addAll(messageLabel(message), backButton);
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

    private VBox projectMemberPanel(DashboardProjectView project, List<User> users) {
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

    private String projectSummary(DashboardProjectView project) {
        String baseSummary = """
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
        if (isAdmin()) {
            return baseSummary;
        }
        return baseSummary + """
                Visible issues: %d
                Deleted issues: %d
                Status counts: %s
                """.formatted(
                project.visibleIssueCount(),
                project.deletedIssueCount(),
                project.statusCounts());
    }

    private VBox registerIssuePanel(DashboardProjectView project) {
        VBox box = borderedPanel("Register Issue");
        box.getChildren().addAll(
                fieldsGrid(
                        "Title", issueTitleField,
                        "Description", issueDescriptionArea,
                        "Priority", priorityBox),
                actionRow(actionButton("Register Issue", issueController.canRegisterIssue(project.projectId()), () -> {
                    var issue = issueController.registerIssue(
                            project.projectId(),
                            requiredText(issueTitleField, "issueTitle"),
                            requiredText(issueDescriptionArea, "issueDescription"),
                            priorityBox.getValue());
                    return "Issue registered: " + issue.issueId() + " / " + issue.status();
                }, message -> onProjectChanged.accept(project.projectId(), message))));
        return box;
    }

    private VBox relatedProjectIssuesPanel(DashboardProjectView project) {
        VBox box = borderedPanel("My Related Issues");
        try {
            List<Issue> issues = issueController.viewRelatedProjectIssues(project.projectId());
            if (issues.isEmpty()) {
                box.getChildren().add(emptyLabel("No related issues in this project."));
                return box;
            }
            for (Issue issue : issues) {
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
                        "Priority", issuePriorityFilterBox),
                actionRow(actionButton("Search Issues", true, () -> {
                    renderIssueSearchResults(project, results);
                    return "Search completed.";
                }, ignored -> { })),
                results);
        results.getChildren().add(emptyLabel("Use search to find project issues."));
        return box;
    }

    private void renderIssueSearchResults(DashboardProjectView project, VBox results) {
        results.getChildren().clear();
        try {
            List<Issue> issues = issueController.searchProjectIssues(
                    project.projectId(),
                    text(issueKeywordField),
                    selectedStatus(),
                    selectedPriorityFilter());
            if (issues.isEmpty()) {
                results.getChildren().add(emptyLabel("No project issues match the current filters."));
                return;
            }
            for (Issue issue : issues) {
                results.getChildren().add(issueCard(issue));
            }
        } catch (RuntimeException exception) {
            results.getChildren().add(messageLabel("Failed: " + exception.getMessage()));
        }
    }

    private Button issueCard(Issue issue) {
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
        button.setOnAction(event -> onIssueSelected.accept(issue));
        return button;
    }

    private VBox statisticsPanel(DashboardProjectView project) {
        VBox box = borderedPanel("Statistics");
        statisticsOutputArea.setEditable(false);
        statisticsOutputArea.setPrefRowCount(22);
        if (!statisticsController.canViewStatistics(project.projectId())) {
            statisticsOutputArea.setText("Statistics are not available for this account.");
        } else {
            try {
                statisticsOutputArea.setText(formatStatistics(statisticsController.viewStatistics(project.projectId())));
            } catch (RuntimeException exception) {
                statisticsOutputArea.setText("Failed: " + exception.getMessage());
            }
        }
        box.getChildren().add(statisticsOutputArea);
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

    private static String formatStatistics(StatisticsReport report) {
        return """
                Daily Issue Registrations
                %s

                Monthly Issue Registrations
                %s

                Daily Status Changes
                %s

                Monthly Status Changes
                %s

                Current Status Counts
                %s

                Current Priority Counts
                %s

                Monthly Priority Counts
                %s

                Daily Comments
                %s

                Monthly Comments
                %s
                """.formatted(
                formatDailyCounts(report),
                formatMonthlyCounts(report),
                formatDailyCounts(report.dailyStatusChangeCounts(), "No daily status change data."),
                formatMonthlyCounts(report.monthlyStatusChangeCounts(), "No monthly status change data."),
                formatStatusCounts(report),
                formatPriorityCounts(report),
                formatMonthlyPriorityCounts(report),
                formatDailyCounts(report.dailyCommentCounts(), "No daily comment data."),
                formatMonthlyCounts(report.monthlyCommentCounts(), "No monthly comment data."));
    }

    private static String formatStatusCounts(StatisticsReport report) {
        StringBuilder builder = new StringBuilder();
        for (IssueStatus status : IssueStatus.values()) {
            if (status == IssueStatus.DELETED) {
                continue;
            }
            int count = report.statusCounts().getOrDefault(status, 0);
            builder.append(status).append(": ").append(count).append(" ").append(bar(count)).append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private static String formatPriorityCounts(StatisticsReport report) {
        StringBuilder builder = new StringBuilder();
        for (Priority priority : Priority.values()) {
            int count = report.priorityCounts().getOrDefault(priority, 0);
            builder.append(priority).append(": ").append(count).append(" ").append(bar(count)).append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private static String formatDailyCounts(StatisticsReport report) {
        return formatDailyCounts(report.dailyCounts(), "No daily issue registration data.");
    }

    private static String formatMonthlyCounts(StatisticsReport report) {
        return formatMonthlyCounts(report.monthlyCounts(), "No monthly issue registration data.");
    }

    private static String formatDailyCounts(List<DailyIssueCount> counts, String emptyMessage) {
        if (counts.isEmpty()) {
            return emptyMessage;
        }
        return counts.stream()
                .map(count -> count.date() + ": " + count.count() + " " + bar(count.count()))
                .reduce((first, second) -> first + System.lineSeparator() + second)
                .orElse(emptyMessage);
    }

    private static String formatMonthlyCounts(List<MonthlyIssueCount> counts, String emptyMessage) {
        if (counts.isEmpty()) {
            return emptyMessage;
        }
        return counts.stream()
                .map(count -> count.month() + ": " + count.count() + " " + bar(count.count()))
                .reduce((first, second) -> first + System.lineSeparator() + second)
                .orElse(emptyMessage);
    }

    private static String formatMonthlyStatusCounts(StatisticsReport report) {
        if (report.monthlyStatusCounts().isEmpty()) {
            return "No monthly status data.";
        }
        return report.monthlyStatusCounts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + System.lineSeparator() + formatStatusMonth(entry.getValue()))
                .reduce((first, second) -> first + System.lineSeparator() + second)
                .orElse("No monthly status data.");
    }

    private static String formatStatusMonth(Map<IssueStatus, Integer> counts) {
        StringBuilder builder = new StringBuilder();
        for (IssueStatus status : IssueStatus.values()) {
            if (status == IssueStatus.DELETED) {
                continue;
            }
            int count = counts.getOrDefault(status, 0);
            builder.append("  ").append(status).append(": ").append(count).append(" ").append(bar(count))
                    .append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private static String formatMonthlyPriorityCounts(StatisticsReport report) {
        if (report.monthlyPriorityCounts().isEmpty()) {
            return "No monthly priority data.";
        }
        return report.monthlyPriorityCounts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + System.lineSeparator() + formatPriorityMonth(entry.getValue()))
                .reduce((first, second) -> first + System.lineSeparator() + second)
                .orElse("No monthly priority data.");
    }

    private static String formatPriorityMonth(Map<Priority, Integer> counts) {
        StringBuilder builder = new StringBuilder();
        for (Priority priority : Priority.values()) {
            int count = counts.getOrDefault(priority, 0);
            builder.append("  ").append(priority).append(": ").append(count).append(" ").append(bar(count))
                    .append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private static String bar(int count) {
        return "#".repeat(Math.min(Math.max(count, 0), 40));
    }

    private static LocalDate optionalDate(TextInputControl field, String fieldName) {
        String value = text(field);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-mm-dd");
        }
    }

    private static YearMonth optionalMonth(TextInputControl field, String fieldName) {
        String value = text(field);
        if (value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-mm");
        }
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
