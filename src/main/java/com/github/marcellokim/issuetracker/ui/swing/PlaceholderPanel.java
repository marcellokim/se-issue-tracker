package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class PlaceholderPanel extends JPanel {

    PlaceholderPanel(String destination, UserResult user, Runnable onLogout) {
        this(destination, user, null, onLogout);
    }

    PlaceholderPanel(String destination, UserResult user, Runnable onBack, Runnable onLogout) {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onLogout, "onLogout");

        setLayout(new BorderLayout());
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        JPanel surface = new JPanel();
        surface.setLayout(new BoxLayout(surface, BoxLayout.Y_AXIS));
        surface.setBackground(SwingStyles.SURFACE);
        surface.setBorder(SwingStyles.surfaceBorder());

        JLabel title = new JLabel(destination);
        title.setName("placeholderTitle");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applySectionTitle(title);
        surface.add(title);
        surface.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        JLabel userLabel = new JLabel(user.name() + " (" + user.role() + ")");
        userLabel.setName("placeholderUser");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        surface.add(userLabel);
        surface.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));

        if (onBack != null) {
            JButton backButton = new JButton("Back");
            backButton.setName("backButton");
            backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            backButton.addActionListener(event -> onBack.run());
            surface.add(backButton);
            surface.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        }

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName("logoutButton");
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.addActionListener(event -> onLogout.run());
        surface.add(logoutButton);

        add(surface, BorderLayout.CENTER);
    }
}
