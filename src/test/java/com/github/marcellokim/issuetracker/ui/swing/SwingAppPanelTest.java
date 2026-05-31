package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing app panel")
class SwingAppPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("offloads login and uses click-time credentials")
    void offloadsLoginAndUsesClickTimeCredentials() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        var panelRef = new AtomicReference<SwingAppPanel>();
        AuthenticationController controller = controller(passwordHashing, user("admin", Role.ADMIN));

        SwingComponentTestSupport.onEdt(() -> {
            var panel = new SwingAppPanel(controller, titles::update);
            panelRef.set(panel);
            JTextField loginId = SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class);
            JPasswordField password = SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class);
            JButton signIn = SwingComponentTestSupport.find(panel, "signInButton", JButton.class);

            loginId.setText("admin");
            password.setText("submitted-password");
            signIn.doClick();

            loginId.setText("changed");
            password.setText("changed-password");

            assertFalse(signIn.isEnabled());
            assertFalse(loginId.isEnabled());
            assertFalse(password.isEnabled());
        });

        assertTrue(passwordHashing.awaitMatch());
        assertFalse(passwordHashing.matchCalledOnEdt());
        assertEquals("submitted-password", passwordHashing.matchedPassword());
        assertTrue(titles.awaitAdminDashboard());
        assertTrue(waitForLoginWorkerCleared(panelRef.get()));

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = panelRef.get();
            JLabel title = SwingComponentTestSupport.find(panel, "placeholderTitle", JLabel.class);
            JButton logout = SwingComponentTestSupport.find(panel, "logoutButton", JButton.class);

            assertEquals("Admin dashboard", title.getText());
            logout.doClick();

            JTextField loginId = SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class);
            JPasswordField password = SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class);
            JButton signIn = SwingComponentTestSupport.find(panel, "signInButton", JButton.class);

            assertTrue(signIn.isEnabled());
            assertTrue(loginId.isEnabled());
            assertTrue(password.isEnabled());
            assertEquals("", new String(password.getPassword()));
        });
    }

    private static boolean waitForLoginWorkerCleared(SwingAppPanel panel) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            AtomicReference<Object> worker = new AtomicReference<>();
            SwingComponentTestSupport.onEdt(() -> worker.set(loginWorker(panel)));
            if (worker.get() == null) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    private static Object loginWorker(SwingAppPanel panel) throws ReflectiveOperationException {
        Field worker = SwingAppPanel.class.getDeclaredField("loginWorker");
        worker.setAccessible(true);
        return worker.get(panel);
    }

    private static AuthenticationController controller(PasswordHashing passwordHashing, User... users) {
        var repository = new InMemoryUserRepository(users);
        var service = new AuthenticationService(repository, passwordHashing, new SessionStore());
        return new AuthenticationController(service);
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private static final class RecordingPasswordHashing implements PasswordHashing {

        private final CountDownLatch matchCalled = new CountDownLatch(1);
        private final AtomicReference<String> matchedPassword = new AtomicReference<>();
        private final AtomicBoolean matchCalledOnEdt = new AtomicBoolean(true);

        @Override
        public String hash(String password) {
            return password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            matchedPassword.set(password);
            matchCalledOnEdt.set(SwingUtilities.isEventDispatchThread());
            matchCalled.countDown();
            return true;
        }

        @Override
        public boolean isHashed(String storedCredential) {
            return true;
        }

        @Override
        public String saltOf(String storedCredential) {
            return "salt";
        }

        @Override
        public String hashOf(String storedCredential) {
            return storedCredential;
        }

        private boolean awaitMatch() throws InterruptedException {
            return matchCalled.await(5, TimeUnit.SECONDS);
        }

        private boolean matchCalledOnEdt() {
            return matchCalledOnEdt.get();
        }

        private String matchedPassword() {
            return matchedPassword.get();
        }
    }

    private static final class RecordingTitleUpdater {

        private final CountDownLatch adminDashboardShown = new CountDownLatch(1);

        private void update(String title) {
            if ("Admin dashboard".equals(title)) {
                adminDashboardShown.countDown();
            }
        }

        private boolean awaitAdminDashboard() throws InterruptedException {
            return adminDashboardShown.await(5, TimeUnit.SECONDS);
        }
    }
}
