package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing admin dashboard panel")
class AdminDashboardPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders project and user tables")
    void rendersProjectAndUserTables() throws Exception {
        AtomicInteger accountClicks = new AtomicInteger();
        AtomicInteger projectClicks = new AtomicInteger();
        AtomicInteger logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            AdminDashboardPanel panel = new AdminDashboardPanel(
                    userResult("admin", Role.ADMIN, true),
                    accountClicks::incrementAndGet,
                    projectClicks::incrementAndGet,
                    logoutClicks::incrementAndGet);
            panel.showDashboard(
                    List.of(projectSummary(1L, "Alpha", 3, 7)),
                    List.of(userResult("dev1", Role.DEV, true)));

            JTable projects = SwingComponentTestSupport.find(panel, "adminProjectTable", JTable.class);
            JTable users = SwingComponentTestSupport.find(panel, "adminUserTable", JTable.class);

            assertEquals(1, projects.getRowCount());
            assertEquals("Alpha", projects.getValueAt(0, 1));
            assertEquals(7, projects.getValueAt(0, 3));
            assertEquals(1, users.getRowCount());
            assertEquals("dev1", users.getValueAt(0, 0));
            assertEquals("DEV", users.getValueAt(0, 2));
        });
    }

    @Test
    @DisplayName("exposes navigation callbacks and dashboard error")
    void exposesNavigationCallbacksAndError() throws Exception {
        AtomicInteger accountClicks = new AtomicInteger();
        AtomicInteger projectClicks = new AtomicInteger();
        AtomicInteger logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            AdminDashboardPanel panel = new AdminDashboardPanel(
                    userResult("admin", Role.ADMIN, true),
                    accountClicks::incrementAndGet,
                    projectClicks::incrementAndGet,
                    logoutClicks::incrementAndGet);

            SwingComponentTestSupport.find(panel, "accountManagementButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectManagementButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "adminLogoutButton", JButton.class).doClick();
            panel.showError("Login is required.");

            assertEquals(1, accountClicks.get());
            assertEquals(1, projectClicks.get());
            assertEquals(1, logoutClicks.get());
            assertEquals(
                    "Login is required.",
                    SwingComponentTestSupport.find(panel, "adminDashboardMessage", JLabel.class).getText());
        });
    }

    private static UserResult userResult(String loginId, Role role, boolean active) {
        return UserResult.from(User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW));
    }

    private static DashboardProjectSummary projectSummary(
            long projectId,
            String projectName,
            int memberCount,
            int visibleIssueCount) {
        return new DashboardProjectSummary(
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
}
