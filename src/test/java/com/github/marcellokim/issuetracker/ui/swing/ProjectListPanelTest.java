package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project list panel")
class ProjectListPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders projects and selection-sensitive open action")
    void rendersProjectsAndSelectionSensitiveOpenAction() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectListPanel panel = panel();
            panel.showProjects(List.of(
                    projectSummary(1L, "Alpha", "Alpha project", 3, 7),
                    projectSummary(2L, "Beta", "Beta project", 1, 0)));

            JTable table = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            JButton open = SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class);

            assertEquals(2, table.getRowCount());
            assertEquals("Alpha", table.getValueAt(0, 1));
            assertEquals("Alpha project", table.getValueAt(0, 2));
            assertEquals(false, open.isEnabled());

            table.setRowSelectionInterval(1, 1);

            assertEquals(true, open.isEnabled());
        });
    }

    @Test
    @DisplayName("publishes selected project and logout actions")
    void publishesSelectedProjectAndLogoutActions() throws Exception {
        var openedProjectId = new AtomicLong();
        var logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            ProjectListPanel panel = new ProjectListPanel(
                    userResult("dev1", Role.DEV, true),
                    new ProjectListPanel.ProjectListActions(
                            openedProjectId::set,
                            logoutClicks::incrementAndGet));
            panel.showProjects(List.of(projectSummary(7L, "Alpha", "Alpha project", 3, 7)));
            JTable table = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            table.setRowSelectionInterval(0, 0);

            SwingComponentTestSupport.find(panel, "openProjectIssuesButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectListLogoutButton", JButton.class).doClick();
        });

        assertEquals(7L, openedProjectId.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("opens selected project on double click")
    void opensSelectedProjectOnDoubleClick() throws Exception {
        var openedProjectId = new AtomicLong();

        SwingComponentTestSupport.onEdt(() -> {
            ProjectListPanel panel = new ProjectListPanel(
                    userResult("tester1", Role.TESTER, true),
                    new ProjectListPanel.ProjectListActions(
                            openedProjectId::set,
                            () -> {
                            }));
            panel.showProjects(List.of(projectSummary(9L, "Gamma", "Gamma project", 2, 4)));
            JTable table = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            table.setRowSelectionInterval(0, 0);

            MouseEvent doubleClick = new MouseEvent(
                    table,
                    MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(),
                    0,
                    8,
                    8,
                    2,
                    false);
            table.dispatchEvent(doubleClick);
        });

        assertEquals(9L, openedProjectId.get());
    }

    @Test
    @DisplayName("shows fallback text for blank error messages")
    void showsFallbackTextForBlankErrors() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectListPanel panel = panel();
            panel.showMessage(" ", true);

            JLabel message = SwingComponentTestSupport.find(panel, "projectListMessage", JLabel.class);

            assertEquals("Project list failed. Please try again.", message.getText());
        });
    }

    @Test
    @DisplayName("renders project list screen snapshot")
    void rendersProjectListSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectListPanel panel = panel();
            panel.showProjects(List.of(
                    projectSummary(1L, "Alpha", "Alpha project", 3, 7),
                    projectSummary(2L, "Beta", "Beta project", 1, 0)));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage initial = render(panel, Path.of("build/reports/ui/project-list-panel.png"));
            assertTrue(hasRenderedContent(initial));

            JTable table = SwingComponentTestSupport.find(panel, "projectListTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            BufferedImage selected = render(panel, Path.of("build/reports/ui/project-list-panel-selected.png"));
            assertTrue(hasRenderedContent(selected));
        });
    }

    private static ProjectListPanel panel() {
        return new ProjectListPanel(
                userResult("dev1", Role.DEV, true),
                new ProjectListPanel.ProjectListActions(
                        projectId -> {
                        },
                        () -> {
                        }));
    }

    private static DashboardProjectSummary projectSummary(
            long projectId,
            String name,
            String description,
            int memberCount,
            int issueCount) {
        return new DashboardProjectSummary(
                projectId,
                name,
                description,
                memberCount,
                1,
                1,
                1,
                issueCount,
                Map.of(IssueStatus.NEW, issueCount));
    }

    private static UserResult userResult(String loginId, Role role, boolean active) {
        return UserResult.from(User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW));
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static BufferedImage render(ProjectListPanel panel, Path output) throws java.io.IOException {
        BufferedImage image = new BufferedImage(
                SwingStyles.WINDOW_SIZE.width,
                SwingStyles.WINDOW_SIZE.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        return image;
    }

    private static boolean hasRenderedContent(BufferedImage image) {
        int background = SwingStyles.BACKGROUND.getRGB();
        int white = Color.WHITE.getRGB();
        int contentPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (pixel != background && pixel != white) {
                    contentPixels++;
                }
            }
        }
        return contentPixels > 10_000;
    }
}
