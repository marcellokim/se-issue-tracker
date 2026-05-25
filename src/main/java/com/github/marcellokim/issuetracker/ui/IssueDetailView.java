package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.AssignmentCandidateResult;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.UserResult;

import java.util.List;
import java.util.Objects;
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

public final class IssueDetailView {

    private final IssueDetailResult issue;
    private final IssueController issueController;
    private final AssignmentController assignmentController;
    private final IssueStateController issueStateController;
    private final DeletedIssueController deletedIssueController;
    private final Runnable onBack;
    private final Consumer<String> onIssueChanged;
    private final IssueWorkflowActions actions;
    private final TextField titleField = field("issue title");
    private final TextArea descriptionArea = area("issue description");
    private final ComboBox<Priority> priorityBox = new ComboBox<>();
    private final Label selectedAssigneeLabel = selectedAssignmentLabel();
    private final Label selectedVerifierLabel = selectedAssignmentLabel();
    private final VBox assignmentCandidatesList = new VBox(8);
    private final TextArea reasonArea = area("status reason");
    private final TextArea deleteReasonArea = area("delete reason");
    private final TextArea newCommentArea = area("general comment content");
    private final TextArea outputArea = area("");
    private final VBox root = new VBox(16);
    private String selectedAssigneeId;
    private String selectedVerifierId;

    public IssueDetailView(
            IssueDetailResult issue,
            IssueController issueController,
            AssignmentController assignmentController,
            IssueStateController issueStateController,
            DeletedIssueController deletedIssueController,
            Runnable onBack,
            Consumer<String> onIssueChanged,
            String initialMessage) {
        this.issue = Objects.requireNonNull(issue, "issue");
        this.issueController = Objects.requireNonNull(issueController, "issueController");
        this.assignmentController = Objects.requireNonNull(assignmentController, "assignmentController");
        this.issueStateController = Objects.requireNonNull(issueStateController, "issueStateController");
        this.deletedIssueController = Objects.requireNonNull(deletedIssueController, "deletedIssueController");
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        this.onIssueChanged = Objects.requireNonNull(onIssueChanged, "onIssueChanged");
        this.actions = issueController.viewAvailableActions(issue.id());
        configure(initialMessage);
    }

    public Parent root() {
        return root;
    }

    private void configure(String initialMessage) {
        titleField.setText(issue.title());
        descriptionArea.setText(issue.description());
        priorityBox.getItems().setAll(Priority.values());
        priorityBox.setValue(issue.priority());
        selectAssignee(valueOrBlank(loginId(issue.assignee())));
        selectVerifier(valueOrBlank(loginId(issue.verifier())));
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(4);
        outputArea.setText(valueOrBlank(initialMessage));
        outputArea.setStyle(messageStyle(initialMessage));

        Button backButton = new Button("Back to Dashboard");
        backButton.setOnAction(event -> onBack.run());

        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(
                actionRow(backButton, sectionLabel("Issue Detail")),
                issueSummary(),
                editIssuePanel(),
                assignmentPanel(),
                statusTransitionPanel(),
                commentsList(),
                addCommentPanel(),
                deletePanel(),
                outputArea);
    }

    private Label issueSummary() {
        Label body = new Label("""
                ID: %d
                Issue ID: %s
                Project ID: %d
                Title: %s
                Description: %s
                Reported At: %s
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
                issue.issueId(),
                issue.projectId(),
                issue.title(),
                issue.description(),
                issue.reportedDate(),
                issue.priority(),
                issue.status(),
                valueOrBlank(loginId(issue.reporter())),
                valueOrBlank(loginId(issue.assignee())),
                valueOrBlank(loginId(issue.verifier())),
                valueOrBlank(loginId(issue.fixer())),
                valueOrBlank(loginId(issue.resolver())),
                issue.updatedAt()));
        body.setStyle("-fx-font-size: 22px;");
        return body;
    }

    private VBox editIssuePanel() {
        VBox panel = borderedPanel("Issue Edit");
        panel.getChildren().addAll(
                fieldsGrid(
                        "Title", titleField,
                        "Description", descriptionArea,
                        "Priority", priorityBox),
                actionRow(
                        actionButton("Update Title/Desc", actions.canUpdateIssue(), () -> {
                            var result = issueController.updateIssue(
                                    issue.id(),
                                    requiredText(titleField, "issueTitle"),
                                    requiredText(descriptionArea, "issueDescription"));
                            return "Issue updated: " + result.issueId() + " / " + result.status();
                        }, true),
                        actionButton("Change Priority", actions.canChangePriority(), () -> {
                            var result = issueController.changePriority(issue.id(), priorityBox.getValue());
                            return "Priority changed: " + result.issueId() + " / " + result.priority();
                        }, true)));
        return panel;
    }

    private VBox assignmentPanel() {
        VBox panel = borderedPanel("Assignment");
        assignmentCandidatesList.setPadding(new Insets(6));
        assignmentCandidatesList.getChildren().setAll(emptyLabel("Select assignee or verifier to load candidates."));
        ScrollPane candidatesScroll = new ScrollPane(assignmentCandidatesList);
        candidatesScroll.setFitToWidth(true);
        candidatesScroll.setPrefViewportWidth(560);
        candidatesScroll.setPrefViewportHeight(170);
        VBox controls = new VBox(
                10,
                assignmentSelectionRow(
                        "Assignee DEV",
                        selectedAssigneeLabel,
                        "Select Assignee",
                        actions.canAssign() || actions.canReassign(),
                        () -> showAssigneeCandidates()),
                assignmentSelectionRow(
                        "Verifier TESTER",
                        selectedVerifierLabel,
                        "Select Verifier",
                        actions.canAssign() || actions.canChangeVerifier(),
                        () -> showVerifierCandidates()),
                actionRow(
                        actionButton("Assign", actions.canAssign(), () -> {
                            var result = assignmentController.assignIssue(
                                    issue.id(),
                                    requiredSelection(selectedAssigneeId, "assignee"),
                                    requiredSelection(selectedVerifierId, "verifier"));
                            return "Assigned: " + result.issueId() + " / " + result.status();
                        }, true),
                        actionButton("Reassign DEV", actions.canReassign(), () -> {
                            var result = assignmentController.reassignIssue(
                                    issue.id(),
                                    requiredSelection(selectedAssigneeId, "assignee"));
                            return "Assignee changed: " + result.issueId();
                        }, true),
                        actionButton("Change TESTER", actions.canChangeVerifier(), () -> {
                            var result = assignmentController.changeVerifier(
                                    issue.id(),
                                    requiredSelection(selectedVerifierId, "verifier"));
                            return "Verifier changed: " + result.issueId();
                        }, true)));
        VBox candidates = new VBox(6, new Label("Candidates"), candidatesScroll);
        HBox content = new HBox(24, controls, candidates);
        content.setAlignment(Pos.TOP_LEFT);
        panel.getChildren().add(content);
        return panel;
    }

    private HBox assignmentSelectionRow(
            String label,
            Label selectedLabel,
            String buttonText,
            boolean enabled,
            Runnable action) {
        Label title = new Label(label);
        title.setMinWidth(110);
        Button button = new Button(buttonText);
        button.setDisable(!enabled);
        button.setOnAction(event -> action.run());
        HBox row = new HBox(10, title, selectedLabel, button);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showAssigneeCandidates() {
        try {
            AssignmentOptionsResult options = assignmentController.startAssignment(issue.id());
            renderCandidateGroups(
                    "Recommended DEV Candidates",
                    options.devAssigneeCandidates(),
                    "All Project DEV",
                    options.allDevAssignees(),
                    this::selectAssignee);
        } catch (RuntimeException exception) {
            showCandidateError(exception);
        }
    }

    private void showVerifierCandidates() {
        try {
            AssignmentOptionsResult options = assignmentController.startAssignment(issue.id());
            renderCandidateGroups(
                    "Recommended TESTER Candidates",
                    options.testerVerifierCandidates(),
                    "All Project TESTER",
                    options.allTesterVerifiers(),
                    this::selectVerifier);
        } catch (RuntimeException exception) {
            showCandidateError(exception);
        }
    }

    private void renderCandidateGroups(
            String recommendedTitle,
            List<AssignmentCandidateResult> recommended,
            String allTitle,
            List<AssignmentCandidateResult> allCandidates,
            Consumer<String> selector) {
        assignmentCandidatesList.getChildren().clear();
        addCandidateGroup(recommendedTitle, recommended, selector);
        addCandidateGroup(allTitle, allCandidates, selector);
    }

    private void addCandidateGroup(
            String title,
            List<AssignmentCandidateResult> candidates,
            Consumer<String> selector) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        assignmentCandidatesList.getChildren().add(titleLabel);
        if (candidates.isEmpty()) {
            assignmentCandidatesList.getChildren().add(emptyLabel("No candidates."));
            return;
        }
        for (AssignmentCandidateResult candidate : candidates) {
            assignmentCandidatesList.getChildren().add(candidateButton(candidate, selector));
        }
    }

    private Button candidateButton(AssignmentCandidateResult candidate, Consumer<String> selector) {
        Button button = new Button("%s / %s / %s / completed=%d".formatted(
                candidate.loginId(),
                candidate.name(),
                candidate.role(),
                candidate.completedIssueCount()));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> selector.accept(candidate.loginId()));
        return button;
    }

    private void showCandidateError(RuntimeException exception) {
        assignmentCandidatesList.getChildren().setAll(messageLabel("Failed: " + exception.getMessage()));
    }

    private void selectAssignee(String loginId) {
        selectedAssigneeId = blankToNull(loginId);
        selectedAssigneeLabel.setText(selectedAssigneeId == null ? "(not selected)" : selectedAssigneeId);
    }

    private void selectVerifier(String loginId) {
        selectedVerifierId = blankToNull(loginId);
        selectedVerifierLabel.setText(selectedVerifierId == null ? "(not selected)" : selectedVerifierId);
    }

    private VBox statusTransitionPanel() {
        VBox panel = borderedPanel("Status Transition");
        panel.getChildren().addAll(
                fieldsGrid("Status Reason", reasonArea),
                actionRow(
                        actionButton("ASSIGNED -> FIXED", actions.canMarkFixed(),
                                () -> changeStatus(IssueStatus.FIXED), true),
                        actionButton("FIXED -> ASSIGNED", actions.canRejectFix(),
                                () -> changeStatus(IssueStatus.ASSIGNED), true),
                        actionButton("FIXED -> RESOLVED", actions.canResolve(),
                                () -> changeStatus(IssueStatus.RESOLVED), true),
                        actionButton("RESOLVED -> CLOSED", actions.canClose(),
                                () -> changeStatus(IssueStatus.CLOSED), true),
                        actionButton("RESOLVED/CLOSED -> REOPENED", actions.canReopen(),
                                () -> changeStatus(IssueStatus.REOPENED), true)));
        if (issue.status() == IssueStatus.FIXED && !actions.canResolve()) {
            panel.getChildren().add(emptyLabel(
                    "FIXED -> RESOLVED is unavailable until verifier permission and blocking dependencies allow it."));
        }
        return panel;
    }

    private VBox commentsList() {
        VBox panel = borderedPanel("Comment List");
        List<CommentResult> comments = issue.comments();
        if (comments.isEmpty()) {
            panel.getChildren().add(emptyLabel("No comments."));
            return panel;
        }
        for (CommentResult comment : comments) {
            panel.getChildren().add(commentCard(comment));
        }
        return panel;
    }

    private HBox commentCard(CommentResult comment) {
        long commentId = commentDatabaseId(comment);
        boolean canUpdate = issueController.canUpdateComment(issue.id(), commentId);
        boolean canDelete = issueController.canDeleteComment(issue.id(), commentId);
        Label meta = new Label("ID %s / %s / %s / %s -> %s%s".formatted(
                comment.commentId(),
                comment.writerLoginId(),
                comment.purpose(),
                comment.createdDate(),
                comment.updatedDate(),
                canUpdate ? " / editable" : " / read-only"));
        meta.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        TextArea editor = area("");
        editor.setText(comment.content());
        editor.setPromptText(canUpdate ? "Edit this comment here, then press Update" : "Only the writer can edit");
        editor.setPrefRowCount(2);
        editor.setEditable(canUpdate);
        editor.setStyle(canUpdate
                ? "-fx-font-size: 18px; -fx-border-color: #2563eb;"
                : "-fx-font-size: 18px; -fx-control-inner-background: #f3f4f6;");

        VBox text = new VBox(6, meta, editor);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

        VBox buttons = new VBox(
                8,
                actionButton("Update", canUpdate, () -> {
                    var updated = issueController.updateComment(
                            issue.id(),
                            commentId,
                            requiredText(editor, "comment"));
                    return "Comment updated: " + updated.commentId();
                }, true),
                actionButton("Delete", canDelete, () -> {
                    issueController.deleteComment(issue.id(), commentId);
                    return "General comment deleted: " + comment.commentId();
                }, true));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(12, text, buttons);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #d1d5db; -fx-background-color: #f9fafb;");
        return card;
    }

    private VBox addCommentPanel() {
        VBox panel = borderedPanel("Comments");
        panel.getChildren().addAll(
                fieldsGrid("New Comment", newCommentArea),
                actionRow(actionButton("Add Comment", actions.canAddComment(), () -> {
                    var comment = issueController.addComment(
                            issue.id(),
                            requiredText(newCommentArea, "comment"));
                    return "Comment added: " + comment.commentId() + " / " + comment.purpose();
                }, true)));
        return panel;
    }

    private VBox deletePanel() {
        VBox panel = borderedPanel("Deleted");
        panel.getChildren().add(actionRow(
                new Label("Delete Reason"),
                deleteReasonArea,
                actionButton("Soft Delete", actions.canSoftDelete(), () -> {
                    var deleted = deletedIssueController.deleteIssue(
                            issue.id(),
                            requiredText(deleteReasonArea, "reason"));
                    return "Issue deleted: " + deleted.issueId() + " / " + deleted.status();
                }, true)));
        return panel;
    }

    private Button actionButton(
            String text,
            boolean enabled,
            Supplier<String> action,
            boolean refreshAfterSuccess) {
        Button button = new Button(text);
        button.setDisable(!enabled);
        button.setOnAction(event -> run(action, refreshAfterSuccess));
        return button;
    }

    private void run(Supplier<String> action, boolean refreshAfterSuccess) {
        try {
            String message = action.get();
            if (refreshAfterSuccess) {
                onIssueChanged.accept(message);
            } else {
                outputArea.setText(message);
                outputArea.setStyle(messageStyle(message));
            }
        } catch (RuntimeException exception) {
            String message = "Failed: " + exception.getMessage();
            outputArea.setText(message);
            outputArea.setStyle(messageStyle(message));
        }
    }

    private String changeStatus(IssueStatus targetStatus) {
        var result = issueStateController.changeStatus(
                issue.id(),
                targetStatus,
                requiredText(reasonArea, "reason"));
        return "Status changed: " + result.issueId() + " / " + result.status();
    }

    private static long commentDatabaseId(CommentResult comment) {
        try {
            return Long.parseLong(comment.commentId());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("commentId must be a persisted numeric id");
        }
    }

    private static VBox borderedPanel(String title) {
        VBox panel = new VBox(10, sectionLabel(title));
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 1.5; -fx-background-color: #ffffff;");
        return panel;
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

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefColumnCount(16);
        return field;
    }

    private static TextArea area(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefColumnCount(32);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        return area;
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

    private static Label messageLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(messageStyle(text));
        return label;
    }

    private static Label selectedAssignmentLabel() {
        Label label = new Label("(not selected)");
        label.setMinWidth(180);
        label.setStyle("-fx-font-size: 16px; -fx-border-color: #d1d5db; -fx-padding: 6; -fx-background-color: #f9fafb;");
        return label;
    }

    private static String requiredText(TextInputControl field, String fieldName) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String requiredSelection(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be selected");
        }
        return value.trim();
    }

    private static String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String loginId(UserResult user) {
        return user == null ? null : user.loginId();
    }

    private static String messageStyle(String message) {
        return valueOrBlank(message).startsWith("Failed:")
                ? "-fx-text-fill: #b91c1c;"
                : "-fx-text-fill: #111827;";
    }
}
