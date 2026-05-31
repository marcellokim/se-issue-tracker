package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class IssueDetailScreen extends VBox {

    private final IssueController issueController;
    private final long issueId;
    private final Label messageLabel = new Label();
    private Runnable onBack;

    IssueDetailScreen(IssueController issueController, long issueId){
        this.issueController = issueController;
        this.issueId = issueId;
        setPadding(new Insets(20));
        setSpacing(12);

        Button backButton = new Button("← Issues");
        backButton.setOnAction(event -> { if (onBack != null) onBack.run(); });

        messageLabel.setStyle("-fx-text-fill: red;");

        getChildren().add(backButton);
        loadDetail();
    }

    void setOnBack(Runnable action){ this.onBack = action; }

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
            peopleBox.getChildren().add(new Label("Reporter: " + userName(detail.reporter())));
            if (detail.assignee() != null) peopleBox.getChildren().add(new Label("Assignee: " + userName(detail.assignee())));
            if (detail.verifier() != null) peopleBox.getChildren().add(new Label("Verifier: " + userName(detail.verifier())));
            if (detail.fixer() != null) peopleBox.getChildren().add(new Label("Fixer: " + userName(detail.fixer())));
            if (detail.resolver() != null) peopleBox.getChildren().add(new Label("Resolver: " + userName(detail.resolver())));

            FlowPane actionButtons = new FlowPane(8, 8);
            actionButtons.setAlignment(Pos.CENTER_LEFT);
            List<String> actions = detail.availableActions();
            for (String action : actions){
                Button btn = new Button(action);
                btn.setOnAction(event -> messageLabel.setText(action + " action will be implemented in follow-up issue"));
                actionButtons.getChildren().add(btn);
            }

            Label commentsTitle = new Label("Comments (" + detail.comments().size() + ")");
            commentsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            ListView<CommentResult> commentList = new ListView<>();
            commentList.getItems().setAll(detail.comments());
            commentList.setCellFactory(list -> new CommentCell());
            commentList.setPrefHeight(200);

            Label historyTitle = new Label("History (" + detail.histories().size() + "entries) | Dependencies (" + detail.dependencies().size() + "entries)");
            historyTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            getChildren().addAll(titleLabel, statusLabel, new Separator(),
                    descriptionLabel, new Separator(),
                    peopleBox, new Separator(),
                    new Label("Available Actions:"), actionButtons, new Separator(),
                    commentsTitle, commentList,
                    historyTitle, messageLabel);
        } catch (Exception exception){
            messageLabel.setText("Issue load failed: " + exception.getMessage());
            getChildren().add(messageLabel);
        }
    }

    private static String userName(com.github.marcellokim.issuetracker.service.UserResult user){
        return user.loginId() + " (" + user.name() + ")";
    }

    private static class CommentCell extends ListCell<CommentResult> {
        @Override
        protected void updateItem(CommentResult comment, boolean empty){
            super.updateItem(comment, empty);
            if (empty || comment == null){ setText(null); return; }
            setText(String.format("[%s] %s: %s", comment.purpose(), comment.writerLoginId(), comment.content()));
        }
    }
}
