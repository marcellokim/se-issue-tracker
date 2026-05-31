package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.AssignmentCandidateResult;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

final class IssueDetailScreen extends VBox {

    private final IssueController issueController;
    private final IssueStateController issueStateController;
    private final AssignmentController assignmentController;
    private final long issueId;
    private final Button backButton;
    private final Label messageLabel = ScreenComponents.messageLabel();
    private Runnable onBack;

    IssueDetailScreen(IssueController issueController, IssueStateController issueStateController, AssignmentController assignmentController, long issueId){
        this.issueController = issueController;
        this.issueStateController = issueStateController;
        this.assignmentController = assignmentController;
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

            Label historyTitle = new Label("History (" + detail.histories().size() + " entries) | Dependencies (" + detail.dependencies().size() + " entries)");
            historyTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            getChildren().addAll(titleLabel, statusLabel, new Separator(),
                    descriptionLabel, new Separator(),
                    peopleBox, new Separator(),
                    new Label("Available Actions:"), actionButtons, new Separator(),
                    commentsTitle, commentList,
                    historyTitle, messageLabel);
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
            case "START_ASSIGNMENT", "ASSIGN" -> btn.setOnAction(e -> handleAssignment());
            default -> {
                btn.setDisable(true);
                btn.setTooltip(new Tooltip("Coming soon"));
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

    private void handleAssignment(){
        try{
            AssignmentOptionsResult options = assignmentController.startAssignment(issueId);
            showAssignmentDialog(options);
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
        }
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
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        textArea.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? textArea.getText().trim() : null);
        return dialog.showAndWait();
    }

    private void showAssignmentDialog(AssignmentOptionsResult options){
        List<AssignmentCandidateResult> devCandidates = mergedCandidates(
                options.devAssigneeCandidates(), options.allDevAssignees());
        List<AssignmentCandidateResult> testerCandidates = mergedCandidates(
                options.testerVerifierCandidates(), options.allTesterVerifiers());
        if (devCandidates.isEmpty() || testerCandidates.isEmpty()){
            ScreenComponents.showInfo(messageLabel, "No candidates available for assignment");
            return;
        }
        Set<String> recommendedDevIds = candidateIds(options.devAssigneeCandidates());
        Set<String> recommendedTesterIds = candidateIds(options.testerVerifierCandidates());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Issue");
        ComboBox<AssignmentCandidateResult> devBox = new ComboBox<>();
        devBox.getItems().addAll(devCandidates);
        devBox.setConverter(candidateConverter(recommendedDevIds));
        devBox.setMaxWidth(Double.MAX_VALUE);
        ComboBox<AssignmentCandidateResult> testerBox = new ComboBox<>();
        testerBox.getItems().addAll(testerCandidates);
        testerBox.setConverter(candidateConverter(recommendedTesterIds));
        testerBox.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(8,
                new Label("Assignee (DEV):"), devBox,
                new Label("Verifier (TESTER):"), testerBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        devBox.valueProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || testerBox.getValue() == null));
        testerBox.valueProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || devBox.getValue() == null));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                assignmentController.assignIssue(issueId,
                        devBox.getValue().loginId(), testerBox.getValue().loginId());
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
