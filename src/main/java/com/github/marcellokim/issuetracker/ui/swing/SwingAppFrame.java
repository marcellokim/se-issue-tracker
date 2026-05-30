package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class SwingAppFrame extends JFrame implements SwingNavigator {

    private static final long serialVersionUID = 1L;

    private final SwingAppPanel appPanel;

    public SwingAppFrame(ApplicationContext context) {
        this(Objects.requireNonNull(context, "context").authenticationController());
    }

    SwingAppFrame(AuthenticationController authenticationController) {
        super("Issue Tracker");
        this.appPanel = new SwingAppPanel(authenticationController, this::setTitle);
        setContentPane(appPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(SwingStyles.WINDOW_SIZE);
        setMinimumSize(SwingStyles.MINIMUM_SIZE);
        setLocationRelativeTo(null);
    }

    @Override
    public void showLogin() {
        appPanel.showLogin();
    }

    @Override
    public void showAdminDashboard(UserResult user) {
        appPanel.showAdminDashboard(user);
    }

    @Override
    public void showProjectList(UserResult user) {
        appPanel.showProjectList(user);
    }

    void logout() {
        appPanel.logout();
    }
}
