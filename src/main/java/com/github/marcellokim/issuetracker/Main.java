package com.github.marcellokim.issuetracker;

import com.github.marcellokim.issuetracker.ui.javafx.JavaFXApp;
import com.github.marcellokim.issuetracker.ui.swing.SwingApp;
import javafx.application.Application;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (shouldLaunchSwing(args)) {
            SwingApp.launch();
            return;
        }
        Application.launch(JavaFXApp.class, args);
    }

    static boolean shouldLaunchSwing(String[] args) {
        return args != null && args.length > 0 && "--swing".equals(args[0]);
    }
}
