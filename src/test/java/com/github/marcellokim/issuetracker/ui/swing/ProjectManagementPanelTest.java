package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project management panel")
class ProjectManagementPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders projects and selection-sensitive actions")
    void rendersProjectsAndSelectionActions() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectManagementPanel panel = panel(new FixedProjectDialogs());
            panel.showProjects(List.of(
                    projectSummary(1L, "Alpha", "Alpha project", 3, 7),
                    projectSummary(2L, "Beta", "Beta project", 1, 0)));

            JTable table = SwingComponentTestSupport.find(panel, "projectManagementTable", JTable.class);
            JButton create = SwingComponentTestSupport.find(panel, "createProjectButton", JButton.class);
            JButton open = SwingComponentTestSupport.find(panel, "openProjectDetailButton", JButton.class);
            JButton rename = SwingComponentTestSupport.find(panel, "renameProjectButton", JButton.class);
            JButton description = SwingComponentTestSupport.find(
                    panel,
                    "changeProjectDescriptionButton",
                    JButton.class);
            JButton delete = SwingComponentTestSupport.find(panel, "deleteProjectButton", JButton.class);

            assertEquals(2, table.getRowCount());
            assertEquals("Alpha", table.getValueAt(0, 1));
            assertEquals("Alpha project", table.getValueAt(0, 2));
            assertEquals(true, create.isEnabled());
            assertEquals(false, open.isEnabled());
            assertEquals(false, rename.isEnabled());
            assertEquals(false, description.isEnabled());
            assertEquals(false, delete.isEnabled());

            table.setRowSelectionInterval(1, 1);

            assertEquals(true, open.isEnabled());
            assertEquals(true, rename.isEnabled());
            assertEquals(true, description.isEnabled());
            assertEquals(true, delete.isEnabled());
        });
    }

    @Test
    @DisplayName("uses dialogs and selected project to publish project actions")
    void publishesProjectActions() throws Exception {
        var createRef = new AtomicReference<ProjectCreateRequest>();
        var openRef = new AtomicLong();
        var renameRef = new AtomicReference<String>();
        var descriptionRef = new AtomicReference<String>();
        var deleteRef = new AtomicReference<String>();
        var backClicks = new AtomicInteger();
        var logoutClicks = new AtomicInteger();
        FixedProjectDialogs dialogs = new FixedProjectDialogs();

        SwingComponentTestSupport.onEdt(() -> {
            ProjectManagementPanel panel = new ProjectManagementPanel(
                    userResult("admin", Role.ADMIN, true),
                    dialogs,
                    new ProjectManagementPanel.ProjectManagementActions(
                            (source, request) -> createRef.set(request),
                            (source, projectId) -> openRef.set(projectId),
                            (source, projectId, name) -> renameRef.set(projectId + ":" + name),
                            (source, projectId, description) ->
                                    descriptionRef.set(projectId + ":" + description),
                            (source, projectId, projectName) -> deleteRef.set(projectId + ":" + projectName),
                            backClicks::incrementAndGet,
                            logoutClicks::incrementAndGet));
            panel.showProjects(List.of(projectSummary(7L, "Alpha", "Alpha project", 3, 7)));
            JTable table = SwingComponentTestSupport.find(panel, "projectManagementTable", JTable.class);
            table.setRowSelectionInterval(0, 0);

            SwingComponentTestSupport.find(panel, "createProjectButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "openProjectDetailButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "renameProjectButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "changeProjectDescriptionButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "deleteProjectButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectManagementBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectManagementLogoutButton", JButton.class).doClick();
        });

        assertEquals(new ProjectCreateRequest("New Project", "New project description"), createRef.get());
        assertEquals(7L, openRef.get());
        assertEquals("7:Renamed Project", renameRef.get());
        assertEquals("7:Updated project description", descriptionRef.get());
        assertEquals("7:Alpha", deleteRef.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("shows fallback text for blank error messages")
    void showsFallbackTextForBlankErrors() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectManagementPanel panel = panel(new FixedProjectDialogs());
            panel.showMessage(" ", true);

            JLabel message = SwingComponentTestSupport.find(panel, "projectManagementMessage", JLabel.class);

            assertEquals("Project management failed. Please try again.", message.getText());
        });
    }

    @Test
    @DisplayName("rejects invalid dialog form component pairs")
    void rejectsInvalidDialogFormComponentPairs() {
        JLabel nameLabel = new JLabel("Name");

        assertThrows(
                IllegalArgumentException.class,
                () -> SwingPanelSections.formPanel(260, nameLabel));
    }

    @Test
    @DisplayName("renders project management screen snapshot")
    void rendersProjectManagementSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectManagementPanel panel = panel(new FixedProjectDialogs());
            panel.showProjects(List.of(
                    projectSummary(1L, "Alpha", "Alpha project", 3, 7),
                    projectSummary(2L, "Beta", "Beta project", 1, 0)));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage initial = render(panel, Path.of("build/reports/ui/project-management-panel.png"));
            assertTrue(hasRenderedContent(initial));

            JTable table = SwingComponentTestSupport.find(panel, "projectManagementTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            BufferedImage selected = render(panel, Path.of("build/reports/ui/project-management-panel-selected.png"));
            assertTrue(hasRenderedContent(selected));
        });
    }

    private static ProjectManagementPanel panel(FixedProjectDialogs dialogs) {
        return new ProjectManagementPanel(
                userResult("admin", Role.ADMIN, true),
                dialogs,
                new ProjectManagementPanel.ProjectManagementActions(
                        (source, ignored) -> {
                        },
                        (source, ignored) -> {
                        },
                        (source, projectId, name) -> {
                        },
                        (source, projectId, description) -> {
                        },
                        (source, projectId, projectName) -> {
                        },
                        () -> {
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

    private static BufferedImage render(ProjectManagementPanel panel, Path output) throws java.io.IOException {
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

    private static final class FixedProjectDialogs implements ProjectDialogs {

        @Override
        public Optional<ProjectCreateRequest> requestCreate(ProjectManagementPanel parent) {
            return Optional.of(new ProjectCreateRequest("New Project", "New project description"));
        }

        @Override
        public Optional<String> requestRename(ProjectManagementPanel parent, DashboardProjectSummary selectedProject) {
            return Optional.of("Renamed Project");
        }

        @Override
        public Optional<String> requestDescription(
                ProjectManagementPanel parent,
                DashboardProjectSummary selectedProject) {
            return Optional.of("Updated project description");
        }

        @Override
        public boolean confirmDelete(ProjectManagementPanel parent, DashboardProjectSummary selectedProject) {
            return true;
        }
    }
}
