package com.github.marcellokim.issuetracker.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ScreenComponents {

    private static final String STYLESHEET = "/javafx/app.css";

    private ScreenComponents(){}

    static void applyScreenDefaults(VBox screen){
        screen.setPadding(new Insets(20));
        screen.setSpacing(12);
        screen.setStyle("-fx-background-color: #f1f5f9;");
    }

    static void applyStylesheet(javafx.scene.Scene scene){
        java.net.URL css = ScreenComponents.class.getResource(STYLESHEET);
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }

    static Label titleLabel(String text){
        Label label = new Label(text);
        label.getStyleClass().add("title-label");
        return label;
    }

    static Button backButton(String text, Runnable onBack){
        Button button = new Button(text);
        button.getStyleClass().add("back-button");
        button.setOnAction(event -> { if (onBack != null) onBack.run(); });
        return button;
    }

    static HBox header(Button... nodes){
        HBox header = new HBox(nodes);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        if (nodes.length > 0){
            HBox.setHgrow(nodes[0], Priority.NEVER);
        }
        return header;
    }

    static HBox header(javafx.scene.Node... nodes){
        HBox header = new HBox(nodes);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        return header;
    }

    static void growInHeader(javafx.scene.Node node){
        HBox.setHgrow(node, Priority.ALWAYS);
    }

    static Label messageLabel(){
        Label label = new Label();
        label.getStyleClass().add("message-label");
        return label;
    }

    static void showError(Label messageLabel, Exception exception){
        messageLabel.setText(exception.getMessage());
        messageLabel.getStyleClass().removeAll("message-label");
        messageLabel.getStyleClass().add("error-label");
    }

    static void showInfo(Label messageLabel, String text){
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("error-label");
        messageLabel.getStyleClass().add("message-label");
    }

    static String shortIssueId(String issueId){
        if (issueId == null) return "";
        if (issueId.matches("issue-\\d+")){
            return issueId;
        }
        if (issueId.startsWith("ISSUE-")){
            return issueId.length() > 14 ? issueId.substring(0, 14) + "..." : issueId;
        }
        return issueId.length() > 8 ? issueId.substring(0, 8) + "..." : issueId;
    }

    static <T> void setupListDoubleClick(javafx.scene.control.ListView<T> listView, java.util.function.Consumer<T> onSelected){
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                T selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && onSelected != null) onSelected.accept(selected);
            }
        });
    }

    static <T> void loadList(javafx.scene.control.ListView<T> listView, Label messageLabel,
                             java.util.function.Supplier<java.util.List<T>> loader){
        try{
            listView.getItems().setAll(loader.get());
        } catch (Exception exception){
            showError(messageLabel, exception);
        }
    }
}
