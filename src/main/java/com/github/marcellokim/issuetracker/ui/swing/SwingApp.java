package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.config.ApplicationContext;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

public final class SwingApp {

    private SwingApp() {
    }

    public static void main(String[] args) {
        StartupState startupState = initialize();
        EventQueue.invokeLater(() -> {
            setSystemLookAndFeel();
            if (startupState.failure() == null) {
                new SwingAppFrame(startupState.context()).setVisible(true);
            } else {
                startupFailureFrame(startupState.failure()).setVisible(true);
            }
        });
    }

    private static StartupState initialize() {
        try {
            return new StartupState(ApplicationContext.fromEnvironment(), null);
        } catch (Exception exception) {
            return new StartupState(null, exception);
        }
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException
                 | InstantiationException
                 | IllegalAccessException
                 | UnsupportedLookAndFeelException exception) {
            // The default cross-platform look and feel is acceptable when the system one is unavailable.
        }
    }

    private static JFrame startupFailureFrame(Exception failure) {
        JFrame frame = new JFrame("Issue Tracker - Startup Failed");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(SwingStyles.WINDOW_SIZE);
        frame.setMinimumSize(SwingStyles.MINIMUM_SIZE);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(SwingStyles.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        JPanel surface = new JPanel(new BorderLayout());
        surface.setBackground(SwingStyles.SURFACE);
        surface.setBorder(SwingStyles.surfaceBorder());

        JLabel message = new JLabel(startupFailureHtml(failure));
        message.setForeground(SwingStyles.BODY_TEXT);
        surface.add(message, BorderLayout.CENTER);
        root.add(surface);

        frame.setContentPane(root);
        return frame;
    }

    private static String startupFailureHtml(Exception failure) {
        String detail = failure.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = "No additional error details were provided.";
        }
        return "<html><body style='width: 520px;'>"
                + "<h2>Issue Tracker could not start</h2>"
                + "<p>Check the database configuration and try again.</p>"
                + "<p><b>Reason:</b> " + escapeHtml(detail) + "</p>"
                + "</body></html>";
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record StartupState(ApplicationContext context, Exception failure) {
    }
}
