package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
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
        var workerRef = new AtomicReference<SwingWorker<?, ?>>();
        var workerDone = new CountDownLatch(1);
        SwingControllerFixture controllers = controllers(passwordHashing, List.of(), user("admin", Role.ADMIN));

        SwingComponentTestSupport.onEdt(() -> {
            var panel = new SwingAppPanel(
                    controllers.authenticationController(),
                    controllers.dashboardController(),
                    titles::update);
            panelRef.set(panel);
            JTextField loginId = SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class);
            JPasswordField password = SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class);
            JButton signIn = SwingComponentTestSupport.find(panel, "signInButton", JButton.class);

            loginId.setText("admin");
            password.setText("submitted-password");
            signIn.doClick();
            SwingWorker<?, ?> worker = loginWorker(panel);
            worker.addPropertyChangeListener(event -> {
                if ("state".equals(event.getPropertyName())
                        && SwingWorker.StateValue.DONE == event.getNewValue()) {
                    workerDone.countDown();
                }
            });
            if (SwingWorker.StateValue.DONE == worker.getState()) {
                workerDone.countDown();
            }
            workerRef.set(worker);

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
        assertNotNull(workerRef.get());
        assertTrue(workerDone.await(5, TimeUnit.SECONDS));
        SwingComponentTestSupport.onEdt(() -> assertNull(loginWorker(panelRef.get())));

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = panelRef.get();
            JTable projects = SwingComponentTestSupport.find(panel, "adminProjectTable", JTable.class);
            JButton logout = SwingComponentTestSupport.find(panel, "adminLogoutButton", JButton.class);

            assertEquals(0, projects.getRowCount());
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

    @Test
    @DisplayName("admin login shows dashboard data")
    void adminLoginShowsDashboardData() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        var panelRef = new AtomicReference<SwingAppPanel>();
        var workerDone = new CountDownLatch(1);
        SwingControllerFixture controllers = controllers(
                passwordHashing,
                List.of(projectSnapshot(1L, "Alpha", 3, 7)),
                user("admin", Role.ADMIN));

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = new SwingAppPanel(
                    controllers.authenticationController(),
                    controllers.dashboardController(),
                    titles::update);
            panelRef.set(panel);
            SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class).setText("admin");
            SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class).setText("submitted-password");
            SwingComponentTestSupport.find(panel, "signInButton", JButton.class).doClick();
            SwingWorker<?, ?> worker = loginWorker(panel);
            worker.addPropertyChangeListener(event -> {
                if ("state".equals(event.getPropertyName())
                        && SwingWorker.StateValue.DONE == event.getNewValue()) {
                    workerDone.countDown();
                }
            });
            if (SwingWorker.StateValue.DONE == worker.getState()) {
                workerDone.countDown();
            }
        });

        assertTrue(passwordHashing.awaitMatch());
        assertTrue(titles.awaitAdminDashboard());
        assertTrue(workerDone.await(5, TimeUnit.SECONDS));
        awaitProjectRows(panelRef.get(), 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panelRef.get(), "adminProjectTable", JTable.class);
            assertEquals(1, projects.getRowCount());
            assertEquals("Alpha", projects.getValueAt(0, 1));
            assertEquals(7, projects.getValueAt(0, 3));
        });
    }

    @Test
    @DisplayName("admin placeholder can return to dashboard")
    void adminPlaceholderCanReturnToDashboard() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        var panelRef = new AtomicReference<SwingAppPanel>();
        var workerDone = new CountDownLatch(1);
        SwingControllerFixture controllers = controllers(
                passwordHashing,
                List.of(projectSnapshot(1L, "Alpha", 3, 7)),
                user("admin", Role.ADMIN));

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = new SwingAppPanel(
                    controllers.authenticationController(),
                    controllers.dashboardController(),
                    titles::update);
            panelRef.set(panel);
            SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class).setText("admin");
            SwingComponentTestSupport.find(panel, "passwordField", JPasswordField.class).setText("submitted-password");
            SwingComponentTestSupport.find(panel, "signInButton", JButton.class).doClick();
            SwingWorker<?, ?> worker = loginWorker(panel);
            worker.addPropertyChangeListener(event -> {
                if ("state".equals(event.getPropertyName())
                        && SwingWorker.StateValue.DONE == event.getNewValue()) {
                    workerDone.countDown();
                }
            });
            if (SwingWorker.StateValue.DONE == worker.getState()) {
                workerDone.countDown();
            }
        });

        assertTrue(passwordHashing.awaitMatch());
        assertTrue(titles.awaitAdminDashboard());
        assertTrue(workerDone.await(5, TimeUnit.SECONDS));
        awaitProjectRows(panelRef.get(), 1);

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panelRef.get(), "accountManagementButton", JButton.class).doClick());
        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Account management",
                    SwingComponentTestSupport.find(panelRef.get(), "placeholderTitle", JLabel.class).getText());
            SwingComponentTestSupport.find(panelRef.get(), "backButton", JButton.class).doClick();
        });

        awaitProjectRows(panelRef.get(), 1);
        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "Alpha",
                SwingComponentTestSupport.find(panelRef.get(), "adminProjectTable", JTable.class).getValueAt(0, 1)));
    }

    private static SwingWorker<?, ?> loginWorker(SwingAppPanel panel) throws ReflectiveOperationException {
        Field worker = SwingAppPanel.class.getDeclaredField("loginWorker");
        worker.setAccessible(true);
        return (SwingWorker<?, ?>) worker.get(panel);
    }

    private static void awaitProjectRows(SwingAppPanel panel, int expectedRows) throws Exception {
        CountDownLatch rowsReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    int rowCount = SwingComponentTestSupport.find(panel, "adminProjectTable", JTable.class).getRowCount();
                    if (rowCount == expectedRows) {
                        ((Timer) event.getSource()).stop();
                        rowsReady.countDown();
                    }
                } catch (AssertionError ignored) {
                    // Dashboard panel is installed asynchronously after admin login.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = rowsReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertEquals(
                        expectedRows,
                        SwingComponentTestSupport.find(panel, "adminProjectTable", JTable.class).getRowCount());
            }
        });
    }

    private static SwingControllerFixture controllers(
            PasswordHashing passwordHashing,
            List<DashboardProjectSnapshot> projects,
            User... users) {
        var repository = new InMemoryUserRepository(users);
        var service = new AuthenticationService(repository, passwordHashing, new SessionStore());
        return new SwingControllerFixture(
                new AuthenticationController(service),
                new DashboardController(
                        service,
                        new DashboardSummaryService(
                                new FakeDashboardSummaryRepository(projects),
                                repository,
                                new PermissionPolicy())));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private static DashboardProjectSnapshot projectSnapshot(
            long projectId,
            String projectName,
            int memberCount,
            int visibleIssueCount) {
        return new DashboardProjectSnapshot(
                projectId,
                projectName,
                "Demo project",
                memberCount,
                1,
                1,
                1,
                visibleIssueCount,
                Map.of(IssueStatus.NEW, visibleIssueCount));
    }

    private record SwingControllerFixture(
            AuthenticationController authenticationController,
            DashboardController dashboardController) {
    }

    private record FakeDashboardSummaryRepository(
            List<DashboardProjectSnapshot> projects) implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return projects;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return List.of();
        }
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
