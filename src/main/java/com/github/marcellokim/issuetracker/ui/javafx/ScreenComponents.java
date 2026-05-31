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
