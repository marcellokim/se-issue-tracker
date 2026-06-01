package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project detail panel")
class ProjectDetailPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders project detail and selection-sensitive participant actions")
    void rendersProjectDetail() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectDetailPanel panel = panel(new FixedProjectDetailDialogs());
            panel.showDetail(detail(List.of(member("dev1", "Developer", Role.DEV))));

            assertEquals("Alpha", SwingComponentTestSupport.find(panel, "projectDetailNameValue", JLabel.class).getText());
            assertEquals(
                    "Alpha project",
                    SwingComponentTestSupport.find(panel, "projectDetailDescriptionValue", JLabel.class).getText());
            JTable table = SwingComponentTestSupport.find(panel, "projectParticipantTable", JTable.class);
            JButton rename = SwingComponentTestSupport.find(panel, "renameProjectDetailButton", JButton.class);
            JButton description = SwingComponentTestSupport.find(
                    panel,
                    "changeProjectDetailDescriptionButton",
                    JButton.class);
            JButton add = SwingComponentTestSupport.find(panel, "addProjectParticipantButton", JButton.class);
            JButton remove = SwingComponentTestSupport.find(panel, "removeProjectParticipantButton", JButton.class);

            assertEquals(1, table.getRowCount());
            assertEquals("dev1", table.getValueAt(0, 0));
            assertEquals("Developer", table.getValueAt(0, 1));
            assertEquals("Active", table.getValueAt(0, 3));
            assertEquals(true, rename.isEnabled());
            assertEquals(true, description.isEnabled());
            assertEquals(true, add.isEnabled());
            assertEquals(false, remove.isEnabled());

            table.setRowSelectionInterval(0, 0);

            assertEquals(true, remove.isEnabled());
        });
    }

    @Test
    @DisplayName("uses dialogs and selected participant to publish actions")
    void publishesProjectDetailActions() throws Exception {
        var renameRef = new AtomicReference<String>();
        var descriptionRef = new AtomicReference<String>();
        var addRef = new AtomicReference<String>();
        var removeRef = new AtomicReference<String>();
        var backClicks = new AtomicInteger();
        var logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            ProjectDetailPanel panel = new ProjectDetailPanel(
                    userResult("admin", Role.ADMIN, true),
                    1L,
                    new FixedProjectDetailDialogs(),
                    new ProjectDetailPanel.ProjectDetailActions(
                            (source, projectId, name) -> renameRef.set(projectId + ":" + name),
                            (source, projectId, description) -> descriptionRef.set(projectId + ":" + description),
                            (source, projectId, loginId) -> addRef.set(projectId + ":" + loginId),
                            (source, projectId, loginId) -> removeRef.set(projectId + ":" + loginId),
                            backClicks::incrementAndGet,
                            logoutClicks::incrementAndGet));
            panel.showDetail(detail(List.of(member("dev1", "Developer", Role.DEV))));
            JTable table = SwingComponentTestSupport.find(panel, "projectParticipantTable", JTable.class);
            table.setRowSelectionInterval(0, 0);

            SwingComponentTestSupport.find(panel, "renameProjectDetailButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "changeProjectDetailDescriptionButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "addProjectParticipantButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "removeProjectParticipantButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectDetailBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "projectDetailLogoutButton", JButton.class).doClick();
        });

        assertEquals("1:Renamed Project", renameRef.get());
        assertEquals("1:Updated project description", descriptionRef.get());
        assertEquals("1:tester1", addRef.get());
        assertEquals("1:dev1", removeRef.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("shows fallback text for blank error messages")
    void showsFallbackTextForBlankErrors() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectDetailPanel panel = panel(new FixedProjectDetailDialogs());
            panel.showMessage(" ", true);

            JLabel message = SwingComponentTestSupport.find(panel, "projectDetailMessage", JLabel.class);

            assertEquals("Project detail failed. Please try again.", message.getText());
        });
    }

    @Test
    @DisplayName("renders project detail screen snapshot")
    void rendersProjectDetailSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            ProjectDetailPanel panel = panel(new FixedProjectDetailDialogs());
            panel.showDetail(detail(List.of(
                    member("pl1", "Project Lead", Role.PL),
                    member("dev1", "Developer", Role.DEV))));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage initial = render(panel, Path.of("build/reports/ui/project-detail-panel.png"));
            assertTrue(hasRenderedContent(initial));

            JTable table = SwingComponentTestSupport.find(panel, "projectParticipantTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            BufferedImage selected = render(panel, Path.of("build/reports/ui/project-detail-panel-selected.png"));
            assertTrue(hasRenderedContent(selected));
        });
    }

    private static ProjectDetailPanel panel(FixedProjectDetailDialogs dialogs) {
        return new ProjectDetailPanel(
                userResult("admin", Role.ADMIN, true),
                1L,
                dialogs,
                new ProjectDetailPanel.ProjectDetailActions(
                        (source, projectId, name) -> {
                        },
                        (source, projectId, description) -> {
                        },
                        (source, projectId, loginId) -> {
                        },
                        (source, projectId, loginId) -> {
                        },
                        () -> {
                        },
                        () -> {
                        }));
    }

    private static ProjectAdminDetail detail(List<ProjectMemberResult> participants) {
        return ProjectAdminDetail.create(project(), participants);
    }

    private static ProjectResult project() {
        return new ProjectResult(1L, "Alpha", "Alpha project", "admin", NOW, NOW);
    }

    private static ProjectMemberResult member(String loginId, String name, Role role) {
        return new ProjectMemberResult(1L, loginId, name, role, true, NOW);
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

    private static BufferedImage render(ProjectDetailPanel panel, Path output) throws java.io.IOException {
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

    private static final class FixedProjectDetailDialogs implements ProjectDetailDialogs {

        @Override
        public Optional<String> requestRename(ProjectDetailPanel parent, ProjectResult project) {
            return Optional.of("Renamed Project");
        }

        @Override
        public Optional<String> requestDescription(ProjectDetailPanel parent, ProjectResult project) {
            return Optional.of("Updated project description");
        }

        @Override
        public Optional<String> requestParticipantLoginId(ProjectDetailPanel parent) {
            return Optional.of("tester1");
        }

        @Override
        public boolean confirmRemove(ProjectDetailPanel parent, ProjectMemberResult selectedParticipant) {
            return true;
        }
    }
}
