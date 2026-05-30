package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

final class LoginPanel extends JPanel implements LoginView {

    private final JTextField loginIdField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton signInButton = new JButton("Sign in");
    private final JLabel messageLabel = new JLabel(" ");
    private Runnable loginRequested = () -> {
    };

    LoginPanel() {
        setLayout(new GridBagLayout());
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

        JLabel title = new JLabel("Issue Tracker");
        SwingStyles.applyTitle(title);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        surface.add(title);
        surface.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));

        JLabel loginIdLabel = new JLabel("ID");
        loginIdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        surface.add(loginIdLabel);
        surface.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        loginIdField.setName("loginIdField");
        SwingStyles.fixHeight(loginIdField, SwingStyles.FIELD_HEIGHT);
        loginIdField.setMaximumSize(loginIdField.getPreferredSize());
        loginIdField.setAlignmentX(Component.LEFT_ALIGNMENT);
        surface.add(loginIdField);
        surface.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        surface.add(passwordLabel);
        surface.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        passwordField.setName("passwordField");
        SwingStyles.fixHeight(passwordField, SwingStyles.FIELD_HEIGHT);
        passwordField.setMaximumSize(passwordField.getPreferredSize());
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.addActionListener(event -> loginRequested.run());
        surface.add(passwordField);
        surface.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));

        signInButton.setName("signInButton");
        SwingStyles.applyPrimaryButton(signInButton);
        signInButton.setMaximumSize(signInButton.getPreferredSize());
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInButton.addActionListener(event -> loginRequested.run());
        surface.add(signInButton);
        surface.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));

        messageLabel.setName("messageLabel");
        SwingStyles.applyMuted(messageLabel);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        surface.add(messageLabel);

        add(surface);
    }

    /**
     * Registers the action to run when the user requests login.
     *
     * <p>The action is invoked synchronously on the Swing Event Dispatch Thread by this panel's action
     * listeners. Callers that perform authentication or other blocking controller/service work must offload
     * that work from the EDT and return promptly.
     */
    void onLoginRequested(Runnable action) {
        loginRequested = Objects.requireNonNull(action, "action");
    }

    @Override
    public String loginId() {
        return loginIdField.getText();
    }

    @Override
    public String password() {
        return new String(passwordField.getPassword());
    }

    @Override
    public void setLoginEnabled(boolean enabled) {
        signInButton.setEnabled(enabled);
    }

    @Override
    public void showMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.setForeground(error ? SwingStyles.ERROR_TEXT : SwingStyles.MUTED_TEXT);
    }

    @Override
    public void clearPassword() {
        passwordField.setText("");
    }
}
