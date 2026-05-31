package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class SwingAppFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final SwingAppPanel appPanel;

    public SwingAppFrame(ApplicationContext context) {
        this(
                Objects.requireNonNull(context, "context").authenticationController(),
                context.dashboardController(),
                context.accountController(),
                context.projectController());
    }

    SwingAppFrame(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController) {
        super("Issue Tracker");
        this.appPanel = new SwingAppPanel(
                authenticationController,
                dashboardController,
                accountController,
                projectController,
                this::setTitle);
        setContentPane(appPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(SwingStyles.WINDOW_SIZE);
        setMinimumSize(SwingStyles.MINIMUM_SIZE);
        setLocationRelativeTo(null);
    }

}
