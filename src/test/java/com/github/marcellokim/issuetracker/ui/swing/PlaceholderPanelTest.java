package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing placeholder panel")
class PlaceholderPanelTest {

    @Test
    @DisplayName("shows destination and invokes logout action")
    void showsDestinationAndInvokesLogoutAction() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            AtomicBoolean loggedOut = new AtomicBoolean();
            UserResult user = new UserResult(
                    "admin",
                    "Admin User",
                    Role.ADMIN,
                    true,
                    LocalDateTime.of(2026, 5, 31, 0, 0),
                    LocalDateTime.of(2026, 5, 31, 0, 0));
            var panel = new PlaceholderPanel("Admin dashboard", user, () -> loggedOut.set(true));

            JLabel title = SwingComponentTestSupport.find(panel, "placeholderTitle", JLabel.class);
            JLabel userLabel = SwingComponentTestSupport.find(panel, "placeholderUser", JLabel.class);
            JButton logout = SwingComponentTestSupport.find(panel, "logoutButton", JButton.class);
            logout.doClick();

            assertEquals("Admin dashboard", title.getText());
            assertEquals("Admin User (ADMIN)", userLabel.getText());
            assertTrue(loggedOut.get());
        });
    }
}
