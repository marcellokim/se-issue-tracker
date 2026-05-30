package com.github.marcellokim.issuetracker;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        javafx.application.Application.launch(com.github.marcellokim.issuetracker.ui.javafx.JavaFXApp.class, args);
    }
}
