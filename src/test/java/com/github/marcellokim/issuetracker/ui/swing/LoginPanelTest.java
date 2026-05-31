package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing login panel")
class LoginPanelTest {

    @Test
    @DisplayName("exposes entered credentials and invokes login action")
    void exposesEnteredCredentialsAndInvokesLoginAction() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            var panel = new LoginPanel();
            JTextField loginId = SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class);
            JPasswordField password = SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class);
            JButton signIn = SwingComponentTestSupport.find(panel, "signInButton", JButton.class);
            AtomicBoolean invoked = new AtomicBoolean();
            AtomicBoolean invokedOnEdt = new AtomicBoolean();

            panel.onLoginRequested(() -> {
                invoked.set(true);
                invokedOnEdt.set(SwingUtilities.isEventDispatchThread());
            });
            loginId.setText("admin");
            password.setText("secret");
            signIn.doClick();

            assertTrue(invoked.get());
            assertTrue(invokedOnEdt.get());
            assertEquals("admin", panel.loginId());
            assertEquals("secret", panel.password());
        });
    }

    @Test
    @DisplayName("updates button and message state through LoginView methods")
    void updatesButtonAndMessageStateThroughViewMethods() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            var panel = new LoginPanel();
            JButton signIn = SwingComponentTestSupport.find(panel, "signInButton", JButton.class);
            JLabel message = SwingComponentTestSupport.find(panel, "messageLabel", JLabel.class);
            JTextField loginId = SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class);
            JPasswordField password = SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class);
            JLabel loginIdLabel = SwingComponentTestSupport.find(panel, "loginIdLabel", JLabel.class);
            JLabel passwordLabel = SwingComponentTestSupport.find(panel, "passwordLabel", JLabel.class);

            panel.setLoginEnabled(false);
            panel.showMessage("Invalid ID or password.", true);
            password.setText("secret");
            panel.clearPassword();

            assertFalse(signIn.isEnabled());
            assertFalse(loginId.isEnabled());
            assertFalse(password.isEnabled());
            assertEquals(SwingStyles.DISABLED_BUTTON_BACKGROUND, signIn.getBackground());
            assertEquals(loginId, loginIdLabel.getLabelFor());
            assertEquals(password, passwordLabel.getLabelFor());
            assertEquals("Invalid ID or password.", message.getText());
            assertEquals(SwingStyles.ERROR_TEXT, message.getForeground());
            assertEquals("", panel.password());
        });
    }
}
