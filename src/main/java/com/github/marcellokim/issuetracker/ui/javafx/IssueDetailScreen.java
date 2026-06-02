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
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.ArrayList;
import java.util.HashSet;
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

            Label titleLabel = new Label(String.format("[%s] %s", ScreenComponents.shortIssueId(detail.issueId()), detail.title()));
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
                if ("START_ASSIGNMENT".equals(action)) continue;
                actionButtons.getChildren().add(createActionButton(action));
            }

            Label commentsTitle = new Label("Comments (" + detail.comments().size() + ")");
            commentsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            Set<String> editableComments = new HashSet<>();
            Set<String> deletableComments = new HashSet<>();
            for (CommentResult c : detail.comments()){
                long cid = Long.parseLong(c.commentId());
                try{ if (issueController.canUpdateComment(issueId, cid)) editableComments.add(c.commentId()); } catch (Exception ignored){}
                try{ if (issueController.canDeleteComment(issueId, cid)) deletableComments.add(c.commentId()); } catch (Exception ignored){}
            }

            ListView<CommentResult> commentList = new ListView<>();
            commentList.getItems().setAll(detail.comments());
            commentList.setCellFactory(list -> new CommentCell(editableComments, deletableComments));
            commentList.setPrefHeight(200);

            Label blockedByTitle = new Label("Blocked by (" + detail.blockedByDependencies().size() + ")");
            blockedByTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            ListView<String> blockedByListView = new ListView<>();
            for (DependencyResult dep : detail.blockedByDependencies()){
                blockedByListView.getItems().add(String.format("%s blocks %s", ScreenComponents.shortIssueId(dep.blockingIssueKey()), ScreenComponents.shortIssueId(dep.blockedIssueKey())));
            }
            blockedByListView.setPrefHeight(Math.min(80, detail.blockedByDependencies().size() * 26 + 4));

            Label blockingTitle = new Label("Blocking (" + detail.blockingDependencies().size() + ")");
            blockingTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            ListView<String> blockingListView = new ListView<>();
            for (DependencyResult dep : detail.blockingDependencies()){
                blockingListView.getItems().add(String.format("%s blocks %s", ScreenComponents.shortIssueId(dep.blockingIssueKey()), ScreenComponents.shortIssueId(dep.blockedIssueKey())));
            }
            blockingListView.setPrefHeight(Math.min(80, detail.blockingDependencies().size() * 26 + 4));

            Label historyTitle = new Label("History (" + detail.histories().size() + " entries)");
            historyTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            VBox content = new VBox(8, titleLabel, statusLabel, new Separator(),
                    descriptionLabel, new Separator(),
                    peopleBox, new Separator(),
                    new Label("Available Actions:"), actionButtons, new Separator(),
                    commentsTitle, commentList,
                    blockedByTitle, blockedByListView,
                    blockingTitle, blockingListView,
                    historyTitle, messageLabel);
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
            scrollPane.setFitToWidth(true);
            VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
            getChildren().add(scrollPane);
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
        Button okButton = setupDialogButtons(dialog);
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
        Button okButton = setupDialogButtons(dialog);
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
        List<IssueSummary> projectIssues;
        try{
            projectIssues = issueController.viewProjectIssues(currentDetail.projectId());
        } catch (Exception exception){
            ScreenComponents.showError(messageLabel, exception);
            return;
        }
        Set<Long> existingBlockedByIds = currentDetail.blockedByDependencies().stream()
                .map(DependencyResult::blockingIssueId).collect(java.util.stream.Collectors.toSet());
        Set<Long> existingBlockingIds = currentDetail.blockingDependencies().stream()
                .map(DependencyResult::blockedIssueId).collect(java.util.stream.Collectors.toSet());
        List<IssueSummary> allCandidates = projectIssues.stream()
                .filter(i -> i.id() != issueId)
                .toList();
        if (allCandidates.isEmpty()){
            ScreenComponents.showInfo(messageLabel, "No other issues available");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Dependency");
        ComboBox<String> directionBox = new ComboBox<>();
        directionBox.getItems().addAll("Blocked by", "Blocks");
        directionBox.setValue("Blocked by");
        ComboBox<IssueSummary> issueBox = new ComboBox<>();
        issueBox.setConverter(new StringConverter<>(){
            @Override public String toString(IssueSummary s){
                return s == null ? "" : String.format("[%s] %s (%s)", ScreenComponents.shortIssueId(s.issueId()), s.title(), s.status());
            }
            @Override public IssueSummary fromString(String str){ return null; }
        });
        issueBox.setMaxWidth(Double.MAX_VALUE);
        Runnable refreshCandidates = () -> {
            issueBox.getItems().clear();
            issueBox.setValue(null);
            Set<Long> excluded = "Blocked by".equals(directionBox.getValue()) ? existingBlockedByIds : existingBlockingIds;
            allCandidates.stream().filter(i -> !excluded.contains(i.id())).forEach(issueBox.getItems()::add);
        };
        directionBox.valueProperty().addListener((obs, old, val) -> refreshCandidates.run());
        refreshCandidates.run();
        dialog.getDialogPane().setContent(new VBox(8,
                new Label("Direction:"), directionBox,
                new Label("Issue:"), issueBox));
        dialog.getDialogPane().setPrefWidth(500);
        Button okButton = setupDialogButtons(dialog);
        okButton.setDisable(true);
        issueBox.valueProperty().addListener((obs, old, val) -> okButton.setDisable(val == null));
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK && issueBox.getValue() != null){
            try{
                long selectedId = issueBox.getValue().id();
                if ("Blocked by".equals(directionBox.getValue())){
                    issueController.addDependency(selectedId, issueId);
                } else{
                    issueController.addDependency(issueId, selectedId);
                }
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }

    private void handleRemoveDependency(){
        if (currentDetail == null
                || (currentDetail.blockedByDependencies().isEmpty() && currentDetail.blockingDependencies().isEmpty())){
            ScreenComponents.showInfo(messageLabel, "No dependencies to remove");
            return;
        }
        Dialog<DependencyResult> dialog = new Dialog<>();
        dialog.setTitle("Remove Dependency");
        ComboBox<DependencyResult> depBox = new ComboBox<>();
        depBox.getItems().addAll(currentDetail.blockedByDependencies());
        depBox.getItems().addAll(currentDetail.blockingDependencies());
        depBox.setConverter(new StringConverter<>(){
            @Override public String toString(DependencyResult d){
                if (d == null) return "";
                String direction = d.blockedIssueId() == issueId ? "[Blocked by] " : "[Blocking] ";
                return direction + String.format("%s blocks %s", d.blockingIssueKey(), d.blockedIssueKey());
            }
            @Override public DependencyResult fromString(String s){ return null; }
        });
        depBox.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(new VBox(8, new Label("Select dependency to remove:"), depBox));
        Button okButton = setupDialogButtons(dialog);
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
        Button okButton = setupDialogButtons(dialog);
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
        dialog.getDialogPane().setPrefWidth(500);
        Button okButton = setupDialogButtons(dialog);
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

    private static Button setupDialogButtons(Dialog<?> dialog){
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("OK");
        return okButton;
    }

    private static String formatUser(UserResult user){
        return user.loginId() + " (" + user.name() + ")";
    }

    private class CommentCell extends ListCell<CommentResult> {
        private final Set<String> editable;
        private final Set<String> deletable;

        CommentCell(Set<String> editable, Set<String> deletable){
            this.editable = editable;
            this.deletable = deletable;
        }

        @Override
        protected void updateItem(CommentResult comment, boolean empty){
            super.updateItem(comment, empty);
            if (empty || comment == null){ setText(null); setGraphic(null); return; }
            setText(null);
            Label text = new Label(String.format("[%s] %s: %s", comment.purpose(), comment.writerLoginId(), comment.content()));
            text.setWrapText(true);
            FlowPane buttons = new FlowPane(4, 0);
            if (editable.contains(comment.commentId())){
                Button editBtn = new Button("Edit");
                editBtn.setStyle("-fx-font-size: 10px;");
                editBtn.setOnAction(e -> handleEditComment(comment));
                buttons.getChildren().add(editBtn);
            }
            if (deletable.contains(comment.commentId())){
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-font-size: 10px;");
                deleteBtn.setOnAction(e -> handleDeleteComment(comment));
                buttons.getChildren().add(deleteBtn);
            }
            VBox box = new VBox(2, text);
            if (!buttons.getChildren().isEmpty()) box.getChildren().add(buttons);
            setGraphic(box);
        }
    }

    private void handleEditComment(CommentResult comment){
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Comment");
        TextArea textArea = new TextArea(comment.content());
        textArea.setPrefRowCount(3);
        dialog.getDialogPane().setContent(textArea);
        Button okButton = setupDialogButtons(dialog);
        okButton.setDisable(true);
        textArea.textProperty().addListener((obs, old, val) ->
                okButton.setDisable(val == null || val.isBlank()));
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? textArea.getText().trim() : null);
        dialog.showAndWait().ifPresent(content -> {
            try{
                issueController.updateComment(issueId, Long.parseLong(comment.commentId()), content);
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        });
    }

    private void handleDeleteComment(CommentResult comment){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete Comment");
        dialog.setHeaderText("Delete this comment?");
        dialog.getDialogPane().setContent(new Label(comment.content()));
        setupDialogButtons(dialog);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            try{
                issueController.deleteComment(issueId, Long.parseLong(comment.commentId()));
                reload();
            } catch (Exception exception){
                ScreenComponents.showError(messageLabel, exception);
            }
        }
    }
}
