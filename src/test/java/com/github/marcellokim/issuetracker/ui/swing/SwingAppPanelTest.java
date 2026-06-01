package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.support.FakeDeletedIssueRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.FakeStatisticsRepository;
import com.github.marcellokim.issuetracker.support.InMemoryAssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
                    controllers.accountController(),
                    controllers.projectController(),
                    controllers.issueController(),
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
                    controllers.accountController(),
                    controllers.projectController(),
                    controllers.issueController(),
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
    @DisplayName("admin project management shows project table")
    void adminProjectManagementShowsProjectTable() throws Exception {
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
                    controllers.accountController(),
                    controllers.projectController(),
                    controllers.issueController(),
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
                SwingComponentTestSupport.find(panelRef.get(), "projectManagementButton", JButton.class).doClick());
        awaitManagedProjectRows(panelRef.get(), 1);

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Project management",
                    SwingComponentTestSupport.find(panelRef.get(), "projectManagementTitle", JLabel.class).getText());
            JTable projects = SwingComponentTestSupport.find(panelRef.get(), "projectManagementTable", JTable.class);
            assertEquals("Alpha", projects.getValueAt(0, 1));
            SwingComponentTestSupport.find(panelRef.get(), "projectManagementBackButton", JButton.class).doClick();
        });

        awaitProjectRows(panelRef.get(), 1);
        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "Alpha",
                SwingComponentTestSupport.find(panelRef.get(), "adminProjectTable", JTable.class).getValueAt(0, 1)));
    }

    @Test
    @DisplayName("admin project management opens project detail")
    void adminProjectManagementOpensProjectDetail() throws Exception {
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
                    controllers.accountController(),
                    controllers.projectController(),
                    controllers.issueController(),
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
            SwingComponentTestSupport.find(panelRef.get(), "projectManagementButton", JButton.class).doClick();
        });
        awaitManagedProjectRows(panelRef.get(), 1);
        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panelRef.get(), "projectManagementTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panelRef.get(), "openProjectDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panelRef.get(), "openProjectDetailButton", JButton.class).doClick());
        awaitProjectDetailName(panelRef.get(), "Alpha");

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Project detail",
                    SwingComponentTestSupport.find(panelRef.get(), "projectDetailTitle", JLabel.class).getText());
            assertEquals(
                    "Alpha",
                    SwingComponentTestSupport.find(panelRef.get(), "projectDetailNameValue", JLabel.class).getText());
            SwingComponentTestSupport.find(panelRef.get(), "projectDetailBackButton", JButton.class).doClick();
        });

        awaitManagedProjectRows(panelRef.get(), 1);
    }

    @Test
    @DisplayName("admin account management shows user table")
    void adminAccountManagementShowsUserTable() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        var panelRef = new AtomicReference<SwingAppPanel>();
        var workerDone = new CountDownLatch(1);
        SwingControllerFixture controllers = controllers(
                passwordHashing,
                List.of(projectSnapshot(1L, "Alpha", 3, 7)),
                user("admin", Role.ADMIN),
                user("dev1", Role.DEV));

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = new SwingAppPanel(
                    controllers.authenticationController(),
                    controllers.dashboardController(),
                    controllers.accountController(),
                    controllers.projectController(),
                    controllers.issueController(),
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
        awaitAccountRows(panelRef.get(), 2);

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Account management",
                    SwingComponentTestSupport.find(panelRef.get(), "accountManagementTitle", JLabel.class).getText());
            JTable users = SwingComponentTestSupport.find(panelRef.get(), "accountUserTable", JTable.class);
            assertEquals("admin", users.getValueAt(0, 0));
            assertEquals("dev1", users.getValueAt(1, 0));
        });
    }

    @Test
    @DisplayName("project user login shows project list data")
    void projectUserLoginShowsProjectListData() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(1L, "Alpha", 3, 7)),
                user("dev1", Role.DEV));

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Project list",
                    SwingComponentTestSupport.find(panel, "projectListTitle", JLabel.class).getText());
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            assertEquals("Alpha", projects.getValueAt(0, 1));
        });
    }

    @Test
    @DisplayName("project list opens issue list with selected project id")
    void projectListOpensIssueListWithSelectedProjectId() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User tester = user("tester1", Role.TESTER);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, tester)),
                tester);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Alpha",
                    SwingComponentTestSupport.find(panel, "issueListTitle", JLabel.class).getText());
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            assertEquals("Login bug", issues.getValueAt(0, 4));
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "[ISSUE-7] Login bug",
                    SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText());
            SwingComponentTestSupport.find(panel, "issueDetailBackButton", JButton.class).doClick();
        });
        awaitIssueRows(panel, 1);
    }

    @Test
    @DisplayName("issue list opens statistics and returns to issue list")
    void issueListOpensStatisticsAndReturnsToIssueList() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User dev = user("dev1", Role.DEV);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, dev)),
                dev);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);
        awaitButtonEnabled(panel, "statisticsButton");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "statisticsButton", JButton.class).doClick());
        awaitRows(panel, "statisticsStatusTable", 2);

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Statistics",
                    SwingComponentTestSupport.find(panel, "statisticsTitle", JLabel.class).getText());
            JTable statusTable = SwingComponentTestSupport.find(panel, "statisticsStatusTable", JTable.class);
            assertEquals(IssueStatus.NEW, statusTable.getValueAt(0, 0));
            SwingComponentTestSupport.find(panel, "statisticsBackButton", JButton.class).doClick();
        });

        awaitIssueRows(panel, 1);
    }

    @Test
    @DisplayName("issue list opens deleted issue management and refreshes restore and purge actions")
    void issueListOpensDeletedIssueManagementAndRefreshesActions() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        Issue restoreTarget = deletedIssue(7L, 7L, "Removed login bug", Priority.CRITICAL, pl);
        Issue purgeTarget = deletedIssue(8L, 7L, "Removed profile typo", Priority.MINOR, pl);
        AtomicLong restorePromptIssueId = new AtomicLong();
        AtomicLong purgePromptIssueId = new AtomicLong();
        DeletedIssuePrompt deletedIssuePrompt = new DeletedIssuePrompt() {
            @Override
            public Optional<String> requestRestoreComment(java.awt.Component parent, IssueSummary issue) {
                restorePromptIssueId.set(issue.id());
                return Optional.of("restore from Swing");
            }

            @Override
            public boolean confirmPurge(java.awt.Component parent, IssueSummary issue) {
                purgePromptIssueId.set(issue.id());
                return true;
            }
        };
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 0)),
                List.of(restoreTarget, purgeTarget),
                List.of(),
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                IssueDependencyDialogs::prompt,
                deletedIssuePrompt,
                IssueEditDialogs::prompt,
                pl);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 0);
        awaitButtonEnabled(panel, "deletedIssuesButton");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "deletedIssuesButton", JButton.class).doClick());
        awaitRows(panel, "deletedIssueTable", 2);

        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Deleted issue management",
                    SwingComponentTestSupport.find(panel, "deletedIssueTitle", JLabel.class).getText());
            assertEquals(
                    "Deleted issues 2/30",
                    SwingComponentTestSupport.find(panel, "deletedIssueCount", JLabel.class).getText());
            JTable deletedIssues = SwingComponentTestSupport.find(panel, "deletedIssueTable", JTable.class);
            deletedIssues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "restoreDeletedIssueButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "restoreDeletedIssueButton", JButton.class).doClick());
        awaitRows(panel, "deletedIssueTable", 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable deletedIssues = SwingComponentTestSupport.find(panel, "deletedIssueTable", JTable.class);
            deletedIssues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "purgeDeletedIssueButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "purgeDeletedIssueButton", JButton.class).doClick());
        awaitRows(panel, "deletedIssueTable", 0);

        assertEquals(restoreTarget.id(), restorePromptIssueId.get());
        assertEquals(purgeTarget.id(), purgePromptIssueId.get());

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "deletedIssueBackButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);
    }

    @Test
    @DisplayName("issue list deleted action surfaces controller failure for non PL")
    void issueListDeletedActionSurfacesControllerFailureForNonPl() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User dev = user("dev1", Role.DEV);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 1)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, dev)),
                dev);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);
        awaitButtonEnabled(panel, "deletedIssuesButton");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "deletedIssuesButton", JButton.class).doClick());

        awaitLabelText(panel, "deletedIssueMessage", "Only PL can manage deleted issues.");
        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "Deleted issue management",
                    SwingComponentTestSupport.find(panel, "deletedIssueTitle", JLabel.class).getText());
            assertEquals(0, SwingComponentTestSupport.find(panel, "deletedIssueTable", JTable.class).getRowCount());
        });
    }

    @Test
    @DisplayName("unsupported issue action stays on detail and shows an error")
    void unsupportedIssueActionStaysOnDetailAndShowsError() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 1)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, pl)),
                pl);

        openFirstIssueDetail(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_START_ASSIGNMENT");

        SwingComponentTestSupport.onEdt(() -> {
            IssueDetailPanel detail = SwingComponentTestSupport.find(panel, "issueDetailPanel", IssueDetailPanel.class);
            detail.showActions(new IssueWorkflowActions(
                    false,
                    false,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false));
            SwingComponentTestSupport.find(panel, "issueActionButton_START_ASSIGNMENT", JButton.class).doClick();

            assertEquals(
                    "[ISSUE-7] Login bug",
                    SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText());
            assertEquals(
                    "Unsupported issue action: START_ASSIGNMENT",
                    SwingComponentTestSupport.find(panel, "issueDetailMessage", JLabel.class).getText());
        });
    }

    @Test
    @DisplayName("issue detail status action changes status and reloads detail")
    void issueDetailStatusActionChangesStatusAndReloadsDetail() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(assignedIssue(7L, 7L, "Login bug", Priority.CRITICAL, assignee, verifier)),
                (parent, action, targetStatus) ->
                        Optional.of(new IssueStatusChangeRequest(targetStatus, "fixed through swing")),
                assignee,
                verifier);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_MARK_FIXED");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_MARK_FIXED", JButton.class).doClick());
        awaitIssueDetailState(panel, "FIXED / CRITICAL");

        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "FIXED / CRITICAL",
                SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText()));
    }

    @Test
    @DisplayName("issue detail start assignment action assigns issue and reloads detail")
    void issueDetailStartAssignmentAssignsIssueAndReloadsDetail() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        AtomicReference<IssueAssignmentMode> promptedMode = new AtomicReference<>();
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, pl)),
                IssueStatusChangeDialogs::prompt,
                (parent, mode, options) -> {
                    promptedMode.set(mode);
                    assertEquals(1, options.allDevAssignees().size());
                    assertEquals(1, options.allTesterVerifiers().size());
                    return Optional.of(new IssueAssignmentRequest(
                            mode,
                            assignee.getLoginId(),
                            verifier.getLoginId()));
                },
                pl,
                assignee,
                verifier);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_START_ASSIGNMENT");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_START_ASSIGNMENT", JButton.class).doClick());
        awaitIssueDetailState(panel, "ASSIGNED / CRITICAL");

        assertEquals(IssueAssignmentMode.ASSIGN, promptedMode.get());
        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "ASSIGNED / CRITICAL",
                SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText()));
    }

    @Test
    @DisplayName("issue detail assignment loads options off the event dispatch thread")
    void issueDetailAssignmentLoadsOptionsOffEdt() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        var recommendations = new RecordingAssignmentRecommendationRepository(pl, assignee, verifier);
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(issue(7L, 7L, "Login bug", Priority.CRITICAL, pl)),
                new SwingPromptSupport(
                        IssueStatusChangeDialogs::prompt,
                        (parent, mode, options) -> Optional.of(new IssueAssignmentRequest(
                                mode,
                                assignee.getLoginId(),
                                verifier.getLoginId())),
                        IssueCommentDialogs::prompt,
                        IssueDependencyDialogs::prompt),
                recommendations,
                pl,
                assignee,
                verifier);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_START_ASSIGNMENT");

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_START_ASSIGNMENT", JButton.class).doClick());

        assertTrue(recommendations.awaitCandidateLookup());
        assertFalse(recommendations.candidateLookupOnEdt());
        awaitIssueDetailState(panel, "ASSIGNED / CRITICAL");
    }

    @Test
    @DisplayName("issue detail comment actions add, update, cancel delete, and delete")
    void issueDetailCommentActionsAddUpdateCancelDeleteAndDelete() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User reporter = user("dev1", Role.DEV);
        Issue issue = issue(7L, 7L, "Login bug", Priority.CRITICAL, reporter);
        AtomicBoolean deleteConfirmed = new AtomicBoolean(false);
        IssueCommentPrompt commentPrompt = (parent, mode, selection) -> switch (mode) {
            case ADD -> Optional.of(IssueCommentRequest.add("new comment through swing"));
            case UPDATE -> Optional.of(IssueCommentRequest.update(selection.commentId(), "edited through swing"));
            case DELETE -> deleteConfirmed.get()
                    ? Optional.of(IssueCommentRequest.delete(selection.commentId()))
                    : Optional.empty();
        };
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 7)),
                List.of(issue),
                List.of(comment(100L, issue.id(), reporter, "original comment")),
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                commentPrompt,
                reporter);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");
        awaitRows(panel, "issueCommentTable", 1);

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_ADD_COMMENT", JButton.class).doClick());
        awaitRows(panel, "issueCommentTable", 2);
        awaitCommentContent(panel, 1, "new comment through swing");

        SwingComponentTestSupport.onEdt(() -> {
            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            comments.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "editCommentButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "editCommentButton", JButton.class).doClick());
        awaitCommentContent(panel, 0, "edited through swing");

        SwingComponentTestSupport.onEdt(() -> {
            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            comments.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "deleteCommentButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "deleteCommentButton", JButton.class).doClick());
        awaitRows(panel, "issueCommentTable", 2);

        deleteConfirmed.set(true);
        SwingComponentTestSupport.onEdt(() -> {
            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            comments.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "deleteCommentButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "deleteCommentButton", JButton.class).doClick());
        awaitRows(panel, "issueCommentTable", 1);
        awaitCommentContent(panel, 0, "new comment through swing");
    }

    @Test
    @DisplayName("issue detail dependency actions add, cancel remove, and remove")
    void issueDetailDependencyActionsAddCancelRemoveAndRemove() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        Issue blockedIssue = issue(7L, 7L, "Login bug", Priority.CRITICAL, pl);
        Issue blockingIssue = issue(8L, 7L, "Profile bug", Priority.MAJOR, pl);
        AtomicBoolean removeConfirmed = new AtomicBoolean(false);
        AtomicReference<IssueDependencyMode> promptedMode = new AtomicReference<>();
        IssueDependencyPrompt dependencyPrompt = (parent, mode, selection, defaultBlockedIssueId) -> {
            promptedMode.set(mode);
            return switch (mode) {
                case ADD -> {
                    assertEquals(blockedIssue.id(), defaultBlockedIssueId);
                    yield Optional.of(IssueDependencyRequest.add(blockingIssue.id(), blockedIssue.id()));
                }
                case REMOVE -> removeConfirmed.get()
                        ? Optional.of(IssueDependencyRequest.remove(
                                selection.blockingIssueId(),
                                selection.blockedIssueId()))
                        : Optional.empty();
            };
        };
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 2)),
                List.of(blockedIssue, blockingIssue),
                List.of(),
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                dependencyPrompt,
                pl);

        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 2);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Login bug");
        awaitRows(panel, "issueDependencyTable", 0);

        awaitButtonEnabled(panel, "addDependencyButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "addDependencyButton", JButton.class).doClick());
        awaitRows(panel, "issueDependencyTable", 1);
        assertEquals(IssueDependencyMode.ADD, promptedMode.get());

        SwingComponentTestSupport.onEdt(() -> {
            JTable dependencies = SwingComponentTestSupport.find(panel, "issueDependencyTable", JTable.class);
            dependencies.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "removeDependencyButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "removeDependencyButton", JButton.class).doClick());
        awaitRows(panel, "issueDependencyTable", 1);
        assertEquals(IssueDependencyMode.REMOVE, promptedMode.get());

        removeConfirmed.set(true);
        SwingComponentTestSupport.onEdt(() -> {
            JTable dependencies = SwingComponentTestSupport.find(panel, "issueDependencyTable", JTable.class);
            dependencies.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "removeDependencyButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "removeDependencyButton", JButton.class).doClick());
        awaitRows(panel, "issueDependencyTable", 0);
    }

    @Test
    @DisplayName("issue detail edit actions update issue and priority")
    void issueDetailEditActionsUpdateIssueAndPriority() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        Issue issue = issue(7L, 7L, "Login bug", Priority.CRITICAL, pl);
        AtomicReference<IssueEditMode> promptedMode = new AtomicReference<>();
        IssueEditPrompt editPrompt = (parent, mode, context) -> {
            promptedMode.set(mode);
            return switch (mode) {
                case UPDATE -> {
                    assertEquals("Login bug", context.title());
                    assertEquals(Priority.CRITICAL, context.priority());
                    yield Optional.of(IssueEditRequest.update("Edited bug", "Edited through Swing"));
                }
                case CHANGE_PRIORITY -> {
                    assertEquals("Edited bug", context.title());
                    assertEquals(Priority.CRITICAL, context.priority());
                    yield Optional.of(IssueEditRequest.changePriority(Priority.MINOR));
                }
                case SOFT_DELETE -> Optional.empty();
            };
        };
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 1)),
                List.of(issue),
                List.of(),
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                IssueDependencyDialogs::prompt,
                editPrompt,
                pl);

        openFirstIssueDetail(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_UPDATE_ISSUE");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_UPDATE_ISSUE", JButton.class).doClick());
        awaitIssueDetailTitle(panel, "[ISSUE-7] Edited bug");

        awaitButtonEnabled(panel, "issueActionButton_CHANGE_PRIORITY");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_CHANGE_PRIORITY", JButton.class).doClick());
        awaitIssueDetailState(panel, "NEW / MINOR");

        assertEquals(IssueEditMode.CHANGE_PRIORITY, promptedMode.get());
        SwingComponentTestSupport.onEdt(() -> {
            assertEquals(
                    "[ISSUE-7] Edited bug",
                    SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText());
            assertEquals(
                    "NEW / MINOR",
                    SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText());
        });
    }

    @Test
    @DisplayName("issue detail soft delete cancel stays on detail and confirm returns to issue list")
    void issueDetailSoftDeleteCancelStaysOnDetailAndConfirmReturnsToIssueList() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        User pl = user("pl1", Role.PL);
        Issue issue = issue(7L, 7L, "Login bug", Priority.CRITICAL, pl);
        AtomicBoolean deleteConfirmed = new AtomicBoolean(false);
        IssueEditPrompt editPrompt = (parent, mode, context) -> {
            if (mode != IssueEditMode.SOFT_DELETE) {
                return Optional.empty();
            }
            return deleteConfirmed.get()
                    ? Optional.of(IssueEditRequest.softDelete("remove duplicate"))
                    : Optional.empty();
        };
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(7L, "Alpha", 3, 1)),
                List.of(issue),
                List.of(),
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                IssueCommentDialogs::prompt,
                IssueDependencyDialogs::prompt,
                editPrompt,
                pl);

        openFirstIssueDetail(panel, "[ISSUE-7] Login bug");
        awaitButtonEnabled(panel, "issueActionButton_SOFT_DELETE");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_SOFT_DELETE", JButton.class).doClick());
        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "[ISSUE-7] Login bug",
                SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText()));

        deleteConfirmed.set(true);
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "issueActionButton_SOFT_DELETE", JButton.class).doClick());
        awaitIssueRows(panel, 0);
        SwingComponentTestSupport.onEdt(() -> assertEquals(
                "Alpha",
                SwingComponentTestSupport.find(panel, "issueListTitle", JLabel.class).getText()));
    }

    @Test
    @DisplayName("project list logout returns to login")
    void projectListLogoutReturnsToLogin() throws Exception {
        var passwordHashing = new RecordingPasswordHashing();
        var titles = new RecordingTitleUpdater();
        SwingAppPanel panel = loginProjectUser(
                passwordHashing,
                titles,
                List.of(projectSnapshot(1L, "Alpha", 3, 7)),
                user("pl1", Role.PL));

        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "projectListLogoutButton", JButton.class).doClick());

        SwingComponentTestSupport.onEdt(() -> {
            assertTrue(SwingComponentTestSupport.find(panel, "signInButton", JButton.class).isEnabled());
            assertEquals("", new String(SwingComponentTestSupport.find(
                    panel,
                    "passwordField",
                    JPasswordField.class).getPassword()));
        });
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            User user) throws Exception {
        return loginProjectUser(passwordHashing, titles, projects, List.of(), user);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            User user) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                IssueStatusChangeDialogs::prompt,
                IssueAssignmentDialogs::prompt,
                user);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            SwingPromptSupport prompts,
            AssignmentRecommendationRepository recommendationRepository,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                List.of(),
                prompts,
                recommendationRepository,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            IssueCommentPrompt commentPrompt,
            IssueDependencyPrompt dependencyPrompt,
            IssueEditPrompt editPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                new SwingPromptSupport(statusChangePrompt, assignmentPrompt, commentPrompt, dependencyPrompt),
                new InMemoryAssignmentRecommendationRepository(users),
                editPrompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            IssueStatusChangePrompt statusChangePrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                statusChangePrompt,
                IssueAssignmentDialogs::prompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                List.of(),
                new SwingPromptSupport(
                        statusChangePrompt,
                        assignmentPrompt,
                        IssueCommentDialogs::prompt,
                        IssueDependencyDialogs::prompt),
                new InMemoryAssignmentRecommendationRepository(users),
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            IssueCommentPrompt commentPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                statusChangePrompt,
                assignmentPrompt,
                commentPrompt,
                IssueDependencyDialogs::prompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            IssueCommentPrompt commentPrompt,
            IssueDependencyPrompt dependencyPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                new SwingPromptSupport(statusChangePrompt, assignmentPrompt, commentPrompt, dependencyPrompt),
                new InMemoryAssignmentRecommendationRepository(users),
                IssueEditDialogs::prompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            IssueCommentPrompt commentPrompt,
            IssueDependencyPrompt dependencyPrompt,
            DeletedIssuePrompt deletedIssuePrompt,
            IssueEditPrompt editPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                new SwingPromptSupport(statusChangePrompt, assignmentPrompt, commentPrompt, dependencyPrompt),
                new InMemoryAssignmentRecommendationRepository(users),
                deletedIssuePrompt,
                editPrompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            SwingPromptSupport prompts,
            AssignmentRecommendationRepository recommendationRepository,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                prompts,
                recommendationRepository,
                IssueEditDialogs::prompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            SwingPromptSupport prompts,
            AssignmentRecommendationRepository recommendationRepository,
            IssueEditPrompt editPrompt,
            User... users) throws Exception {
        return loginProjectUser(
                passwordHashing,
                titles,
                projects,
                issues,
                comments,
                prompts,
                recommendationRepository,
                new DeletedIssueDialogs.JOptionPaneDeletedIssuePrompt(),
                editPrompt,
                users);
    }

    private static SwingAppPanel loginProjectUser(
            RecordingPasswordHashing passwordHashing,
            RecordingTitleUpdater titles,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            SwingPromptSupport prompts,
            AssignmentRecommendationRepository recommendationRepository,
            DeletedIssuePrompt deletedIssuePrompt,
            IssueEditPrompt editPrompt,
            User... users) throws Exception {
        var panelRef = new AtomicReference<SwingAppPanel>();
        var workerDone = new CountDownLatch(1);
        SwingControllerFixture controllers = controllers(
                passwordHashing,
                projects,
                issues,
                comments,
                recommendationRepository,
                users);

        SwingComponentTestSupport.onEdt(() -> {
            SwingAppPanel panel = new SwingAppPanel(
                    controllers.swingControllers(),
                    new IssueActionSupport(
                            new IssueStatusChangeSupport(
                                    controllers.issueStateController(),
                                    prompts.statusChangePrompt()),
                            controllers.assignmentController(),
                            prompts.assignmentPrompt(),
                            prompts.commentPrompt(),
                            prompts.dependencyPrompt(),
                            controllers.deletedIssueController(),
                            editPrompt),
                    deletedIssuePrompt,
                    titles::update);
            panelRef.set(panel);
            User user = users[0];
            SwingComponentTestSupport.find(panel, "loginIdField", JTextField.class).setText(user.getLoginId());
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
        assertTrue(titles.awaitProjectList());
        assertTrue(workerDone.await(5, TimeUnit.SECONDS));
        SwingAppPanel panel = panelRef.get();
        assertNotNull(panel);
        awaitProjectListRows(panel, projects.size());
        return panel;
    }

    private static void openFirstIssueDetail(SwingAppPanel panel, String expectedTitle) throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            JTable projects = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            projects.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openProjectIssuesButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick());
        awaitIssueRows(panel, 1);

        SwingComponentTestSupport.onEdt(() -> {
            JTable issues = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            issues.setRowSelectionInterval(0, 0);
        });
        awaitButtonEnabled(panel, "openIssueDetailButton");
        SwingComponentTestSupport.onEdt(() ->
                SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick());
        awaitIssueDetailTitle(panel, expectedTitle);
    }

    private static SwingWorker<?, ?> loginWorker(SwingAppPanel panel) throws ReflectiveOperationException {
        Field worker = SwingAppPanel.class.getDeclaredField("loginWorker");
        worker.setAccessible(true);
        return (SwingWorker<?, ?>) worker.get(panel);
    }

    private static void awaitProjectRows(SwingAppPanel panel, int expectedRows) throws Exception {
        awaitRows(panel, "adminProjectTable", expectedRows);
    }

    private static void awaitAccountRows(SwingAppPanel panel, int expectedRows) throws Exception {
        awaitRows(panel, "accountUserTable", expectedRows);
    }

    private static void awaitManagedProjectRows(SwingAppPanel panel, int expectedRows) throws Exception {
        awaitRows(panel, "projectManagementTable", expectedRows);
    }

    private static void awaitProjectListRows(SwingAppPanel panel, int expectedRows) throws Exception {
        awaitRows(panel, "projectListTable", expectedRows);
    }

    private static void awaitIssueRows(SwingAppPanel panel, int expectedRows) throws Exception {
        awaitRows(panel, "issueListTable", expectedRows);
    }

    private static void awaitProjectDetailName(SwingAppPanel panel, String expectedName) throws Exception {
        CountDownLatch detailReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    String actual = SwingComponentTestSupport.find(panel, "projectDetailNameValue", JLabel.class)
                            .getText();
                    if (expectedName.equals(actual)) {
                        ((Timer) event.getSource()).stop();
                        detailReady.countDown();
                    }
                } catch (AssertionError ignored) {
                    // Detail panel is installed asynchronously after the selected project opens.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = detailReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertEquals(
                        expectedName,
                        SwingComponentTestSupport.find(panel, "projectDetailNameValue", JLabel.class).getText());
            }
        });
    }

    private static void awaitIssueDetailTitle(SwingAppPanel panel, String expectedTitle) throws Exception {
        CountDownLatch detailReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    String actual = SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText();
                    if (expectedTitle.equals(actual)) {
                        ((Timer) event.getSource()).stop();
                        detailReady.countDown();
                    }
                } catch (AssertionError ignored) {
                    // Detail panel is installed asynchronously after navigation.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = detailReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertEquals(
                        expectedTitle,
                        SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText());
            }
        });
    }

    private static void awaitIssueDetailState(SwingAppPanel panel, String expectedState) throws Exception {
        CountDownLatch detailReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    String actual = SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText();
                    if (expectedState.equals(actual)) {
                        ((Timer) event.getSource()).stop();
                        detailReady.countDown();
                    }
                } catch (AssertionError ignored) {
                    // Detail panel reloads asynchronously after status change.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = detailReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertEquals(
                        expectedState,
                        SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText());
            }
        });
    }

    private static void awaitLabelText(SwingAppPanel panel, String labelName, String expectedText) throws Exception {
        CountDownLatch labelReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    String actual = SwingComponentTestSupport.find(panel, labelName, JLabel.class).getText();
                    if (expectedText.equals(actual)) {
                        ((Timer) event.getSource()).stop();
                        labelReady.countDown();
                    }
                } catch (RuntimeException | AssertionError ignored) {
                    // The target panel may still be installing or refreshing asynchronously.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = labelReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertEquals(
                        expectedText,
                        SwingComponentTestSupport.find(panel, labelName, JLabel.class).getText());
            }
        });
    }

    private static void awaitCommentContent(SwingAppPanel panel, int row, String expectedContent) throws Exception {
        CountDownLatch contentReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
                    if (row < comments.getRowCount() && expectedContent.equals(comments.getValueAt(row, 3))) {
                        ((Timer) event.getSource()).stop();
                        contentReady.countDown();
                    }
                } catch (AssertionError ignored) {
                    // Comment table reloads asynchronously after comment actions.
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = contentReady.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
                assertEquals(expectedContent, comments.getValueAt(row, 3));
            }
        });
    }

    private static void awaitButtonEnabled(SwingAppPanel panel, String buttonName) throws Exception {
        CountDownLatch enabled = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                JButton button = SwingComponentTestSupport.find(panel, buttonName, JButton.class);
                if (button.isEnabled()) {
                    ((Timer) event.getSource()).stop();
                    enabled.countDown();
                }
            });
            timer.setRepeats(true);
            timerRef.set(timer);
            timer.start();
        });

        boolean ready = enabled.await(5, TimeUnit.SECONDS);
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = timerRef.get();
            if (timer != null) {
                timer.stop();
            }
            if (!ready) {
                assertTrue(SwingComponentTestSupport.find(panel, buttonName, JButton.class).isEnabled());
            }
        });
    }

    private static void awaitRows(SwingAppPanel panel, String tableName, int expectedRows) throws Exception {
        CountDownLatch rowsReady = new CountDownLatch(1);
        AtomicReference<Timer> timerRef = new AtomicReference<>();
        SwingComponentTestSupport.onEdt(() -> {
            Timer timer = new Timer(25, event -> {
                try {
                    int rowCount = SwingComponentTestSupport.find(panel, tableName, JTable.class).getRowCount();
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
                        SwingComponentTestSupport.find(panel, tableName, JTable.class).getRowCount());
            }
        });
    }

    private static SwingControllerFixture controllers(
            PasswordHashing passwordHashing,
            List<DashboardProjectSnapshot> projects,
            User... users) {
        return controllers(passwordHashing, projects, List.of(), users);
    }

    private static SwingControllerFixture controllers(
            PasswordHashing passwordHashing,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            User... users) {
        return controllers(
                passwordHashing,
                projects,
                issues,
                List.of(),
                new InMemoryAssignmentRecommendationRepository(users),
                users);
    }

    private static SwingControllerFixture controllers(
            PasswordHashing passwordHashing,
            List<DashboardProjectSnapshot> projects,
            List<Issue> issues,
            List<Comment> comments,
            AssignmentRecommendationRepository recommendationRepository,
            User... users) {
        var repository = new InMemoryUserRepository(users);
        var projectRepository = new InMemoryProjectRepository();
        projects.forEach(project -> {
            projectRepository.withProject(Project.fromPersistence(
                        project.projectId(),
                        project.projectName(),
                        project.projectDescription(),
                        "admin",
                        NOW,
                        NOW));
            for (User user : users) {
                if (user.getRole() != Role.ADMIN) {
                    projectRepository.withParticipant(project.projectId(), user.getLoginId());
                }
            }
        });
        var issueRepository = new InMemoryIssueRepository(issues.toArray(Issue[]::new));
        var deletedIssueRepository = new FakeDeletedIssueRepository(issueRepository);
        for (Issue issue : issues) {
            if (issue.status() == IssueStatus.DELETED) {
                deletedIssueRepository.addDeletedIssue(issue);
            }
        }
        var dependencyRepository = new FakeIssueDependencyRepository();
        var commentRepository = new FakeCommentRepository(comments);
        var historyRepository = new FakeIssueHistoryRepository();
        var statisticsRepository = new FakeStatisticsRepository(statisticsReport());
        var service = new AuthenticationService(repository, passwordHashing, new SessionStore());
        PermissionPolicy permissionPolicy = new PermissionPolicy();
        IssueStateController issueStateController = new IssueStateController(
                service,
                new IssueStateService(
                        issueRepository,
                        dependencyRepository,
                        repository,
                        permissionPolicy,
                        () -> NOW,
                        () -> "status-change-comment"));
        AssignmentController assignmentController = new AssignmentController(
                service,
                new AssignmentService(
                        issueRepository,
                        repository,
                        permissionPolicy,
                        new AssignmentRecommendationService(
                                recommendationRepository,
                                new KNNAssignmentRecommendation()),
                        () -> NOW));
        DeletedIssueController deletedIssueController = new DeletedIssueController(
                service,
                new DeletedIssueService(
                        issueRepository,
                        deletedIssueRepository,
                        repository,
                        permissionPolicy,
                        () -> NOW));
        return new SwingControllerFixture(
                new AuthenticationController(service),
                new DashboardController(
                        service,
                        new DashboardSummaryService(
                                new FakeDashboardSummaryRepository(projects),
                                repository,
                                permissionPolicy)),
                new AccountController(
                        service,
                        new AccountService(
                                permissionPolicy,
                                repository,
                                projectRepository,
                                new InMemoryIssueRepository(),
                                passwordHashing,
                                () -> NOW)),
                new ProjectController(
                        service,
                        new ProjectService(
                                projectRepository,
                                issueRepository,
                                repository,
                                permissionPolicy,
                                () -> NOW)),
                new IssueController(
                        service,
                        new IssueService(
                                projectRepository,
                                issueRepository,
                                dependencyRepository,
                                commentRepository,
                                historyRepository,
                                repository,
                                permissionPolicy,
                                () -> NOW),
                        new IssueWorkflowService(
                                issueRepository,
                                dependencyRepository,
                                commentRepository,
                                repository,
                                permissionPolicy)),
                new StatisticsController(
                        service,
                        new StatisticsService(permissionPolicy, statisticsRepository, repository)),
                issueStateController,
                assignmentController,
                deletedIssueController);
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

    private static Issue issue(long id, long projectId, String title, Priority priority, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(projectId, title, "Issue description", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .priority(priority)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Issue deletedIssue(long id, long projectId, String title, Priority priority, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(projectId, title, "Issue description", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .priority(priority)
                .status(IssueStatus.DELETED)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Issue assignedIssue(
            long id,
            long projectId,
            String title,
            Priority priority,
            User assignee,
            User verifier) {
        return Issue.fromPersistence(Issue.persistedState(projectId, title, "Issue description", assignee)
                .id(id)
                .issueId("ISSUE-" + id)
                .priority(priority)
                .status(IssueStatus.ASSIGNED)
                .assignee(assignee)
                .verifier(verifier)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Comment comment(long id, long issueId, User writer, String content) {
        return Comment.fromPersistence(id, issueId, writer.getLoginId(), content, CommentPurpose.GENERAL, NOW, NOW);
    }

    private static StatisticsReport statisticsReport() {
        return StatisticsReport.create(
                Map.of(IssueStatus.NEW, 2, IssueStatus.CLOSED, 1),
                Map.of(Priority.CRITICAL, 1, Priority.MAJOR, 2),
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 3)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 3)),
                Map.of(YearMonth.of(2026, 5), Map.of(IssueStatus.NEW, 2, IssueStatus.CLOSED, 1)),
                Map.of(YearMonth.of(2026, 5), Map.of(Priority.CRITICAL, 1, Priority.MAJOR, 2)),
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 2)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 2)),
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 31), 4)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 4)));
    }

    private record SwingControllerFixture(
            AuthenticationController authenticationController,
            DashboardController dashboardController,
            AccountController accountController,
            ProjectController projectController,
            IssueController issueController,
            StatisticsController statisticsController,
            IssueStateController issueStateController,
            AssignmentController assignmentController,
            DeletedIssueController deletedIssueController) {

        SwingControllers swingControllers() {
            return new SwingControllers(
                    authenticationController,
                    dashboardController,
                    accountController,
                    projectController,
                    issueController,
                    statisticsController);
        }
    }

    private record SwingPromptSupport(
            IssueStatusChangePrompt statusChangePrompt,
            IssueAssignmentPrompt assignmentPrompt,
            IssueCommentPrompt commentPrompt,
            IssueDependencyPrompt dependencyPrompt) {
    }

    private static final class FakeCommentRepository implements CommentRepository {

        private final Map<Long, Comment> comments = new java.util.LinkedHashMap<>();

        private FakeCommentRepository() {
        }

        private FakeCommentRepository(List<Comment> comments) {
            for (Comment comment : comments) {
                this.comments.put(comment.id(), comment);
            }
        }

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.ofNullable(comments.get(commentId));
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return comments.values().stream()
                    .filter(comment -> comment.issueId() == issueId)
                    .toList();
        }

        @Override
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
            Comment saved = comment;
            if (comment.id() == 0L) {
                long nextId = comments.keySet().stream().mapToLong(Long::longValue).max().orElse(99L) + 1L;
                saved = Comment.fromPersistence(
                        nextId,
                        comment.issueId(),
                        comment.writerId(),
                        comment.content(),
                        comment.purpose(),
                        comment.createdDate(),
                        comment.updatedDate());
            }
            comments.put(saved.id(), saved);
            return saved;
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            comments.remove(commentId);
        }
    }

    private record FakeDashboardSummaryRepository(
            List<DashboardProjectSnapshot> projects) implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return projects;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return projects;
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
        private final CountDownLatch projectListShown = new CountDownLatch(1);

        private void update(String title) {
            if ("Admin dashboard".equals(title)) {
                adminDashboardShown.countDown();
            }
            if ("Project list".equals(title)) {
                projectListShown.countDown();
            }
        }

        private boolean awaitAdminDashboard() throws InterruptedException {
            return adminDashboardShown.await(5, TimeUnit.SECONDS);
        }

        private boolean awaitProjectList() throws InterruptedException {
            return projectListShown.await(5, TimeUnit.SECONDS);
        }
    }

    private static final class RecordingAssignmentRecommendationRepository
            implements AssignmentRecommendationRepository {

        private final InMemoryAssignmentRecommendationRepository delegate;
        private final CountDownLatch candidateLookup = new CountDownLatch(1);
        private final AtomicBoolean candidateLookupOnEdt = new AtomicBoolean(true);

        private RecordingAssignmentRecommendationRepository(User... users) {
            this.delegate = new InMemoryAssignmentRecommendationRepository(users);
        }

        @Override
        public List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId) {
            return delegate.findResolvedIssuesForRecommendation(projectId);
        }

        @Override
        public List<User> findActiveDevCandidates(long projectId) {
            candidateLookupOnEdt.set(SwingUtilities.isEventDispatchThread());
            candidateLookup.countDown();
            return delegate.findActiveDevCandidates(projectId);
        }

        @Override
        public List<User> findActiveTesterCandidates(long projectId) {
            return delegate.findActiveTesterCandidates(projectId);
        }

        private boolean awaitCandidateLookup() throws InterruptedException {
            return candidateLookup.await(5, TimeUnit.SECONDS);
        }

        private boolean candidateLookupOnEdt() {
            return candidateLookupOnEdt.get();
        }
    }
}
