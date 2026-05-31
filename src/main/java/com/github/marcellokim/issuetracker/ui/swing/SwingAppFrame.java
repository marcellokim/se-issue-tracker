package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class SwingAppFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final SwingAppPanel appPanel;

    public SwingAppFrame(ApplicationContext context) {
        this(swingControllers(context), issueActionSupport(context));
    }

    SwingAppFrame(SwingControllers controllers, IssueActionSupport issueActionSupport) {
        super("Issue Tracker");
        this.appPanel = new SwingAppPanel(
                controllers,
                issueActionSupport,
                this::setTitle);
        setContentPane(appPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(SwingStyles.WINDOW_SIZE);
        setMinimumSize(SwingStyles.MINIMUM_SIZE);
        setLocationRelativeTo(null);
    }

    private static SwingControllers swingControllers(ApplicationContext context) {
        ApplicationContext checked = Objects.requireNonNull(context, "context");
        return new SwingControllers(
                checked.authenticationController(),
                checked.dashboardController(),
                checked.accountController(),
                checked.projectController(),
                checked.issueController(),
                checked.statisticsController());
    }

    private static IssueActionSupport issueActionSupport(ApplicationContext context) {
        ApplicationContext checked = Objects.requireNonNull(context, "context");
        return IssueActionSupport.dialogs(checked.issueStateController(), checked.assignmentController());
    }

}
