package com.github.marcellokim.issuetracker.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ScreenComponents {

    private ScreenComponents(){}

    static void applyScreenDefaults(VBox screen){
        screen.setPadding(new Insets(20));
        screen.setSpacing(12);
    }

    static Label titleLabel(String text){
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        return label;
    }

    static Button backButton(String text, Runnable onBack){
        Button button = new Button(text);
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

    static HBox headerWithGrow(javafx.scene.Node... nodes){
        HBox header = new HBox(nodes);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        if (nodes.length > 1){
            HBox.setHgrow(nodes[1], Priority.ALWAYS);
        }
        return header;
    }

    static Label messageLabel(){
        Label label = new Label();
        label.setStyle("-fx-text-fill: #666;");
        return label;
    }

    static void showError(Label messageLabel, Exception exception){
        messageLabel.setText(exception.getMessage());
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    static void showInfo(Label messageLabel, String text){
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: #666;");
    }
}
