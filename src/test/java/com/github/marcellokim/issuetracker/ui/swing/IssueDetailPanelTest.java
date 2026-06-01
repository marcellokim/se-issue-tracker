package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.HistoryResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue detail panel")
class IssueDetailPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders issue detail, summaries, comment permissions, and action button states")
    void rendersIssueDetailAndActions() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueDetailPanel panel = panel();
            panel.showDetail(
                    detail(List.of("UPDATE_ISSUE", "ADD_DEPENDENCY", "REMOVE_DEPENDENCY", "ADD_COMMENT")),
                    List.of(new IssueCommentActionState("100", 100L, "Confirmed in local run.", true, true)),
                    List.of(dependency()));

            assertEquals(
                    "[ISSUE-7] Login bug",
                    SwingComponentTestSupport.find(panel, "issueDetailTitle", JLabel.class).getText());
            assertEquals(
                    "NEW / CRITICAL",
                    SwingComponentTestSupport.find(panel, "issueDetailState", JLabel.class).getText());
            assertTrue(SwingComponentTestSupport.find(panel, "issueActionButton_UPDATE_ISSUE", JButton.class)
                    .isEnabled());
            assertTrue(SwingComponentTestSupport.find(panel, "issueActionButton_ADD_COMMENT", JButton.class)
                    .isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "issueActionButton_MARK_FIXED", JButton.class)
                    .isEnabled());
            assertTrue(SwingComponentTestSupport.find(panel, "addDependencyButton", JButton.class).isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "removeDependencyButton", JButton.class).isEnabled());

            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            assertEquals(1, comments.getRowCount());
            assertEquals("100", comments.getValueAt(0, 0));
            assertEquals("Y", comments.getValueAt(0, 5));
            assertEquals("Y", comments.getValueAt(0, 6));
            assertTrue(SwingComponentTestSupport.find(panel, "addCommentButton", JButton.class).isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "editCommentButton", JButton.class).isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "deleteCommentButton", JButton.class).isEnabled());

            comments.setRowSelectionInterval(0, 0);
            assertTrue(SwingComponentTestSupport.find(panel, "editCommentButton", JButton.class).isEnabled());
            assertTrue(SwingComponentTestSupport.find(panel, "deleteCommentButton", JButton.class).isEnabled());

            JTable histories = SwingComponentTestSupport.find(panel, "issueHistoryTable", JTable.class);
            assertEquals("CREATED", histories.getValueAt(0, 1));

            JTable dependencies = SwingComponentTestSupport.find(panel, "issueDependencyTable", JTable.class);
            assertEquals("ISSUE-3", dependencies.getValueAt(0, 2));
            dependencies.setRowSelectionInterval(0, 0);
            assertTrue(SwingComponentTestSupport.find(panel, "removeDependencyButton", JButton.class).isEnabled());

            IssueEditContext editContext = panel.currentIssueEditContext();
            assertEquals("Login bug", editContext.title());
            assertEquals("Login fails with valid credentials.", editContext.description());
            assertEquals(Priority.CRITICAL, editContext.priority());
        });
    }

    @Test
    @DisplayName("publishes available action, back, and logout callbacks")
    void publishesActions() throws Exception {
        AtomicReference<String> actionRef = new AtomicReference<>();
        AtomicReference<IssueCommentMode> commentModeRef = new AtomicReference<>();
        AtomicReference<IssueCommentSelection> commentSelectionRef = new AtomicReference<>();
        AtomicReference<IssueDependencyMode> dependencyModeRef = new AtomicReference<>();
        AtomicReference<IssueDependencySelection> dependencySelectionRef = new AtomicReference<>();
        AtomicInteger backClicks = new AtomicInteger();
        AtomicInteger logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            IssueDetailPanel panel = new IssueDetailPanel(
                    userResult("dev1", Role.DEV),
                    new IssueDetailPanel.IssueDetailActions(
                            (source, action) -> actionRef.set(action),
                            (source, mode, selection) -> {
                                commentModeRef.set(mode);
                                commentSelectionRef.set(selection);
                            },
                            (source, mode, selection) -> {
                                dependencyModeRef.set(mode);
                                dependencySelectionRef.set(selection);
                            },
                            backClicks::incrementAndGet,
                            logoutClicks::incrementAndGet));
            panel.showDetail(
                    detail(List.of("ADD_COMMENT", "ADD_DEPENDENCY", "REMOVE_DEPENDENCY")),
                    List.of(new IssueCommentActionState("100", 100L, "Confirmed in local run.", true, true)),
                    List.of(dependency()));

            SwingComponentTestSupport.find(panel, "issueActionButton_ADD_COMMENT", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "addCommentButton", JButton.class).doClick();
            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            comments.setRowSelectionInterval(0, 0);
            SwingComponentTestSupport.find(panel, "editCommentButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "addDependencyButton", JButton.class).doClick();
            JTable dependencies = SwingComponentTestSupport.find(panel, "issueDependencyTable", JTable.class);
            dependencies.setRowSelectionInterval(0, 0);
            SwingComponentTestSupport.find(panel, "removeDependencyButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "issueDetailBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "issueDetailLogoutButton", JButton.class).doClick();
        });

        assertEquals("ADD_COMMENT", actionRef.get());
        assertEquals(IssueCommentMode.UPDATE, commentModeRef.get());
        assertEquals(100L, commentSelectionRef.get().commentId());
        assertEquals("Confirmed in local run.", commentSelectionRef.get().content());
        assertEquals(IssueDependencyMode.REMOVE, dependencyModeRef.get());
        assertEquals(3L, dependencySelectionRef.get().blockingIssueId());
        assertEquals(7L, dependencySelectionRef.get().blockedIssueId());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("disables edit and delete for non numeric comments")
    void disablesEditAndDeleteForNonNumericComments() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueDetailPanel panel = panel();
            panel.showDetail(
                    detail(List.of("ADD_COMMENT")),
                    List.of(new IssueCommentActionState("legacy-id", null, "Legacy comment", false, false)),
                    List.of());

            JTable comments = SwingComponentTestSupport.find(panel, "issueCommentTable", JTable.class);
            comments.setRowSelectionInterval(0, 0);

            assertTrue(SwingComponentTestSupport.find(panel, "addCommentButton", JButton.class).isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "editCommentButton", JButton.class).isEnabled());
            assertFalse(SwingComponentTestSupport.find(panel, "deleteCommentButton", JButton.class).isEnabled());
        });
    }

    @Test
    @DisplayName("renders issue detail screen snapshot")
    void rendersIssueDetailSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueDetailPanel panel = panel();
            panel.showDetail(
                    detail(List.of("UPDATE_ISSUE", "ADD_COMMENT", "ADD_DEPENDENCY", "REMOVE_DEPENDENCY")),
                    List.of(new IssueCommentActionState("100", 100L, "Confirmed in local run.", true, true)),
                    List.of(dependency()));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage image = render(panel, Path.of("build/reports/ui/issue-detail-panel.png"));
            assertTrue(hasRenderedContent(image));

            scrollToDependencies(panel);
            BufferedImage dependencyImage = render(
                    panel,
                    Path.of("build/reports/ui/issue-detail-dependency-section.png"));
            assertTrue(hasRenderedContent(dependencyImage));
        });
    }

    private static IssueDetailPanel panel() {
        return new IssueDetailPanel(
                userResult("dev1", Role.DEV),
                new IssueDetailPanel.IssueDetailActions(
                        (source, action) -> {
                        },
                        (source, mode, selection) -> {
                        },
                        (source, mode, selection) -> {
                        },
                        () -> {
                        },
                        () -> {
                        }));
    }

    private static IssueDetailResult detail(List<String> actions) {
        return new IssueDetailResult(
                7L,
                1L,
                "ISSUE-7",
                IssueStatus.NEW,
                Priority.CRITICAL,
                "Login bug",
                "Login fails with valid credentials.",
                userResult("dev1", Role.DEV),
                null,
                null,
                null,
                null,
                NOW,
                NOW,
                List.of(comment()),
                List.of(history()),
                List.of(dependency()),
                actions);
    }

    private static CommentResult comment() {
        return new CommentResult(
                "100",
                "Confirmed in local run.",
                CommentPurpose.GENERAL,
                "dev1",
                userResult("dev1", Role.DEV),
                NOW,
                NOW);
    }

    private static HistoryResult history() {
        return new HistoryResult(1L, 7L, "dev1", ActionType.CREATED, null, "NEW", "Created", NOW);
    }

    private static DependencyResult dependency() {
        return new DependencyResult(1L, "dep-1", 3L, "ISSUE-3", 7L, "ISSUE-7", NOW);
    }

    private static UserResult userResult(String loginId, Role role) {
        return UserResult.from(User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW));
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static BufferedImage render(IssueDetailPanel panel, Path output) throws java.io.IOException {
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

    private static void scrollToDependencies(IssueDetailPanel panel) {
        JTable dependencies = SwingComponentTestSupport.find(panel, "issueDependencyTable", JTable.class);
        dependencies.scrollRectToVisible(new Rectangle(0, 0, dependencies.getWidth(), dependencies.getHeight()));
        Container ancestor = dependencies.getParent();
        while (ancestor != null) {
            if (ancestor instanceof JViewport viewport && viewport.getView() != null) {
                Point position = SwingUtilities.convertPoint(dependencies, 0, 0, viewport.getView());
                viewport.setViewPosition(new Point(0, Math.max(0, position.y - SwingStyles.ROW_GAP)));
            }
            ancestor = ancestor.getParent();
        }
        layoutRecursively(panel);
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
