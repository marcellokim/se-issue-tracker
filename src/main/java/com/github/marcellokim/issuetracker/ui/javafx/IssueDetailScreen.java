package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.AssignmentCandidateResult;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

final class IssueDetailScreen extends VBox {

    private final IssueController issueController;
    private final IssueStateController issueStateController;
    private final AssignmentController assignmentController;
    private final DeletedIssueController deletedIssueController;
    private final long issueId;
    private final Button backButton;
    private final Label messageLabel = ScreenComponents.messageLabel();
    private IssueDetailResult currentDetail;
    private Runnable onBack;

    IssueDetailScreen(IssueController issueController, IssueStateController issueStateController, AssignmentController assignmentController, DeletedIssueController deletedIssueController, long issueId){
        this.issueController = issueController;
        this.issueStateController = issueStateController;
        this.assignmentController = assignmentController;
        this.deletedIssueController = deletedIssueController;
        this.issueId = issueId;
        ScreenComponents.applyScreenDefaults(this);
        this.backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        getChildren().add(backButton);
        loadDetail();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void reload(){
        getChildren().clear();
        getChildren().add(backButton);
        loadDetail();
    }

    private void loadDetail(){
        try{
            IssueDetailResult detail = issueController.viewIssueDetail(issueId);
            this.currentDetail = detail;

            Label titleLabel = new Label(String.format("[%s] %s", detail.issueId(), detail.title()));
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Label statusLabel = new Label(String.format("Status: %s | Priority: %s", detail.status(), detail.priority()));
            statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");

            Label descriptionLabel = new Label(detail.description());
            descriptionLabel.setWrapText(true);

            VBox peopleBox = new VBox(4);
            peopleBox.getChildren().add(new Label("Reporter: " + formatUser(detail.reporter())));
            if (detail.assignee() != null) peopleBox.getChildren().add(new Label("Assignee: " + formatUser(detail.assignee())));
            if (detail.verifier() != null) peopleBox.getChildren().add(new Label("Verifier: " + formatUser(detail.verifier())));
            if (detail.fixer() != null) peopleBox.getChildren().add(new Label("Fixer: " + formatUser(detail.fixer())));
            if (detail.resolver() != null) peopleBox.getChildren().add(new Label("Resolver: " + formatUser(detail.resolver())));

            FlowPane actionButtons = new FlowPane(8, 8);
            actionButtons.setAlignment(Pos.CENTER_LEFT);
            for (String action : detail.availableActions()){
                actionButtons.getChildren().add(createActionButton(action));
            }

            Label commentsTitle = new Label("Comments (" + detail.comments().size() + ")");
            commentsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            ListView<CommentResult> commentList = new ListView<>();
            commentList.getItems().setAll(detail.comments());
            commentList.setCellFactory(list -> new CommentCell());
            commentList.setPrefHeight(200);

            VBox dependencyBox = new VBox(4);
            Label dependencyTitle = new Label("Dependencies (" + detail.dependencies().size() + ")");
            dependencyTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            dependencyBox.getChildren().add(dependencyTitle);
            for (DependencyResult dep : detail.dependencies()){
                dependencyBox.getChildren().add(new Label(String.format("  %s blocks %s", dep.blockingIssueKey(), dep.blockedIssueKey())));
            }

            Label historyTitle = new Label("History (" + detail.histories().size() + " entries)");
            historyTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            getChildren().addAll(titleLabel, statusLabel, new Separator(),
                    descriptionLabel, new Separator(),
                    peopleBox, new Separator(),
                    new Label("Available Actions:"), actionButtons, new Separator(),
                    commentsTitle, commentList,
                    dependencyBox, historyTitle, messageLabel);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
            getChildren().add(messageLabel);
        }
    }

    private Button createActionButton(String action){
        Button btn = new Button(action);
        switch (action){
            case "ADD_COMMENT" -> btn.setOnAction(e -> handleAddComment());
            case "MARK_FIXED" -> btn.setOnAction(e -> handleStatusChange(IssueStatus.FIXED));
            case "RESOLVE" -> btn.setOnAction(e -> handleStatusChange(IssueStatus.RESOLVED));
            case "CLOSE" -> btn.setOnAction(e -> handleStatusChange(IssueStatus.CLOSED));
            case "REJECT_FIX" -> btn.setOnAction(e -> handleStatusChange(IssueStatus.ASSIGNED));
            case "REOPEN" -> btn.setOnAction(e -> handleStatusChange(IssueStatus.REOPENED));
            case "ASSIGN" -> btn.setOnAction(e -> handleAssignment(true, true));
            case "REASSIGN_DEV" -> btn.setOnAction(e -> handleAssignment(true, false));
            case "CHANGE_TESTER" -> btn.setOnAction(e -> handleAssignment(false, true));
            case "CHANGE_PRIORITY" -> btn.setOnAction(e -> handleChangePriority());
            case "SOFT_DELETE" -> btn.setOnAction(e -> handleSoftDelete());
            case "UPDATE_ISSUE" -> btn.setOnAction(e -> handleUpdateIssue());
            case "ADD_DEPENDENCY" -> btn.setOnAction(e -> handleAddDependency());
            case "REMOVE_DEPENDENCY" -> btn.setOnAction(e -> handleRemoveDependency());
            default -> {
                btn.setDisable(true);
                btn.setTooltip(new Tooltip("Not available"));
            }
        }
        return btn;
    }

    private void handleAddComment(){
        showTextInputDialog("Add Comment", "Enter comment:").ifPresent(content -> {
            try{
                issueController.addComment(issueId, content);
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleStatusChange(IssueStatus targetStatus){
        showTextInputDialog("Change Status", "Reason for " + targetStatus + ":").ifPresent(comment -> {
            try{
                issueStateController.changeStatus(issueId, targetStatus, comment);
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleAssignment(boolean selectDev, boolean selectTester){
        try{
            AssignmentOptionsResult options = assignmentController.startAssignment(issueId);
            showAssignmentDialog(options, selectDev, selectTester);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void handleChangePriority(){
        Dialog<Priority> dialog = new Dialog<>();
        dialog.setTitle("Change Priority");
        ComboBox<Priority> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(Priority.values());
        if (currentDetail != null) priorityBox.setValue(currentDetail.priority());
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(new VBox(8, new Label("Priority:"), priorityBox));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        priorityBox.valueProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val == currentDetail.priority()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? priorityBox.getValue() : null);
        dialog.showAndWait().ifPresent(priority -> {
            try{
                issueController.changePriority(issueId, priority);
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleSoftDelete(){
        showTextInputDialog("Delete Issue", "Reason for deletion:").ifPresent(comment -> {
            try{
                deletedIssueController.deleteIssue(issueId, comment);
                if (onBack != null) onBack.run();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleUpdateIssue(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Issue");
        TextField titleField = new TextField(currentDetail != null ? currentDetail.title() : "");
        titleField.setPromptText("Title");
        TextArea descField = new TextArea(currentDetail != null ? currentDetail.description() : "");
        descField.setPromptText("Description");
        descField.setPrefRowCount(4);
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("Title:"), titleField,
                new Label("Description:"), descField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        Runnable validate = () -> okButton.setDisable(
                titleField.getText() == null || titleField.getText().isBlank()
                || descField.getText() == null || descField.getText().isBlank());
        titleField.textProperty().addListener((obs, old, val) -> validate.run());
        descField.textProperty().addListener((obs, old, val) -> validate.run());
        validate.run();
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                issueController.updateIssue(issueId, titleField.getText().trim(), descField.getText().trim());
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private void handleAddDependency(){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Dependency");
        dialog.setHeaderText("This issue will be blocked by the specified issue.");
        TextField blockingIdField = new TextField();
        blockingIdField.setPromptText("Blocking issue ID (number)");
        dialog.getDialogPane().setContent(new VBox(8, new Label("Blocking Issue ID:"), blockingIdField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        blockingIdField.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || !val.matches("[1-9]\\d*")));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? blockingIdField.getText().trim() : null);
        dialog.showAndWait().ifPresent(blockingIdText -> {
            try{
                long blockingIssueId = Long.parseLong(blockingIdText);
                issueController.addDependency(blockingIssueId, issueId);
                reload();
            } catch (NumberFormatException exception){
                ScreenComponents.showError(messageLabel, new IllegalArgumentException("Issue ID must be a number."));
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleRemoveDependency(){
        if (currentDetail == null || currentDetail.dependencies().isEmpty()){
            ScreenComponents.showInfo(messageLabel, "No dependencies to remove");
            return;
        }
        Dialog<DependencyResult> dialog = new Dialog<>();
        dialog.setTitle("Remove Dependency");
        ComboBox<DependencyResult> depBox = new ComboBox<>();
        depBox.getItems().addAll(currentDetail.dependencies());
        depBox.setConverter(new StringConverter<>(){
            @Override public String toString(DependencyResult d){
                if (d == null) return "";
                return String.format("%s blocks %s", d.blockingIssueKey(), d.blockedIssueKey());
            }
            @Override public DependencyResult fromString(String s){ return null; }
        });
        depBox.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(new VBox(8, new Label("Select dependency to remove:"), depBox));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        depBox.valueProperty().addListener((obs, old, val) -> okButton.setDisable(val == null));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? depBox.getValue() : null);
        dialog.showAndWait().ifPresent(dep -> {
            try{
                issueController.removeDependency(dep.blockingIssueId(), dep.blockedIssueId());
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private Optional<String> showTextInputDialog(String title, String headerText){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        TextArea textArea = new TextArea();
        textArea.setPromptText("Enter text...");
        textArea.setPrefRowCount(3);
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        textArea.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? textArea.getText().trim() : null);
        return dialog.showAndWait();
    }

    private void showAssignmentDialog(AssignmentOptionsResult options, boolean selectDev, boolean selectTester){
        List<AssignmentCandidateResult> devCandidates = mergedCandidates(
                options.devAssigneeCandidates(), options.allDevAssignees());
        List<AssignmentCandidateResult> testerCandidates = mergedCandidates(
                options.testerVerifierCandidates(), options.allTesterVerifiers());
        if ((selectDev && devCandidates.isEmpty()) || (selectTester && testerCandidates.isEmpty())){
            ScreenComponents.showInfo(messageLabel, "No candidates available for assignment");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Issue");
        VBox content = new VBox(8);
        ComboBox<AssignmentCandidateResult> devBox = new ComboBox<>();
        ComboBox<AssignmentCandidateResult> testerBox = new ComboBox<>();
        if (selectDev){
            devBox.getItems().addAll(devCandidates);
            devBox.setConverter(candidateConverter(candidateIds(options.devAssigneeCandidates())));
            devBox.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().addAll(new Label("Assignee (DEV):"), devBox);
        }
        if (selectTester){
            testerBox.getItems().addAll(testerCandidates);
            testerBox.setConverter(candidateConverter(candidateIds(options.testerVerifierCandidates())));
            testerBox.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().addAll(new Label("Verifier (TESTER):"), testerBox);
        }
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        dialog.getDialogPane().setPrefWidth(500);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        okButton.setDisable(true);
        if (selectDev) devBox.valueProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || (selectTester && testerBox.getValue() == null)));
        if (selectTester) testerBox.valueProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || (selectDev && devBox.getValue() == null)));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                if (selectDev && selectTester){
                    assignmentController.assignIssue(issueId, devBox.getValue().loginId(), testerBox.getValue().loginId());
                } else if (selectDev){
                    assignmentController.reassignIssue(issueId, devBox.getValue().loginId());
                } else{
                    assignmentController.changeVerifier(issueId, testerBox.getValue().loginId());
                }
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private static List<AssignmentCandidateResult> mergedCandidates(
            List<AssignmentCandidateResult> recommended, List<AssignmentCandidateResult> all){
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<AssignmentCandidateResult> merged = new ArrayList<>();
        for (AssignmentCandidateResult c : recommended){
            if (seen.add(c.loginId())) merged.add(c);
        }
        for (AssignmentCandidateResult c : all){
            if (seen.add(c.loginId())) merged.add(c);
        }
        return merged;
    }

    private static Set<String> candidateIds(List<AssignmentCandidateResult> candidates){
        Set<String> ids = new LinkedHashSet<>();
        for (AssignmentCandidateResult c : candidates) ids.add(c.loginId());
        return ids;
    }

    private static StringConverter<AssignmentCandidateResult> candidateConverter(Set<String> recommendedIds){
        return new StringConverter<>(){
            @Override public String toString(AssignmentCandidateResult c){
                if (c == null) return "";
                String prefix = recommendedIds.contains(c.loginId()) ? "[Recommended] " : "";
                return String.format("%s%s (%s) - %s", prefix, c.loginId(), c.name(), c.reason());
            }
            @Override public AssignmentCandidateResult fromString(String s){ return null; }
        };
    }

    private static String formatUser(UserResult user){
        return user.loginId() + " (" + user.name() + ")";
    }

    private static class CommentCell extends ListCell<CommentResult> {
        @Override
        protected void updateItem(CommentResult comment, boolean empty){
            super.updateItem(comment, empty);
            if (empty || comment == null){ setText(null); setGraphic(null); return; }
            setText(String.format("[%s] %s: %s", comment.purpose(), comment.writerLoginId(), comment.content()));
        }
    }
}
