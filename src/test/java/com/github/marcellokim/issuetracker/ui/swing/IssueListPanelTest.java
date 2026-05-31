package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.ProjectResult;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue list panel")
class IssueListPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders project, issues, filters, and selection-sensitive actions")
    void rendersProjectIssuesAndActions() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueListPanel panel = panel(new FixedIssueDialogs());
            panel.showProject(project());
            panel.showIssues(List.of(
                    issueSummary(1L, "ISSUE-1", "Login bug", IssueStatus.NEW, Priority.CRITICAL),
                    issueSummary(2L, "ISSUE-2", "Profile typo", IssueStatus.ASSIGNED, Priority.MINOR)));
            panel.setRegisterEnabled(true);

            JTable table = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            JButton open = SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class);
            JButton register = SwingComponentTestSupport.find(panel, "registerIssueButton", JButton.class);

            assertEquals("Alpha", SwingComponentTestSupport.find(panel, "issueListTitle", JLabel.class).getText());
            assertEquals(2, table.getRowCount());
            assertEquals("ISSUE-1", table.getValueAt(0, 1));
            assertEquals("Login bug", table.getValueAt(0, 4));
            assertEquals(true, register.isEnabled());
            assertEquals(false, open.isEnabled());

            table.setRowSelectionInterval(1, 1);

            assertEquals(true, open.isEnabled());
        });
    }

    @Test
    @DisplayName("publishes search, register, open, deleted issue, statistics, back, and logout actions")
    void publishesActions() throws Exception {
        var searchRef = new AtomicReference<IssueSearchRequest>();
        var registerRef = new AtomicReference<IssueRegisterRequest>();
        var openRef = new AtomicLong();
        var deletedIssueClicks = new AtomicInteger();
        var statisticsClicks = new AtomicInteger();
        var backClicks = new AtomicInteger();
        var logoutClicks = new AtomicInteger();
        FixedIssueDialogs dialogs = new FixedIssueDialogs();

        SwingComponentTestSupport.onEdt(() -> {
            IssueListPanel panel = new IssueListPanel(
                    userResult("pl1", Role.PL),
                    dialogs,
                    new IssueListPanel.IssueListActions(
                            (source, request) -> searchRef.set(request),
                            (source, request) -> registerRef.set(request),
                            openRef::set,
                            deletedIssueClicks::incrementAndGet,
                            statisticsClicks::incrementAndGet,
                            backClicks::incrementAndGet,
                            logoutClicks::incrementAndGet));
            panel.showProject(project());
            panel.showIssues(List.of(issueSummary(7L, "ISSUE-7", "Login bug", IssueStatus.NEW, Priority.CRITICAL)));
            panel.setRegisterEnabled(true);

            SwingComponentTestSupport.find(panel, "issueSearchField", JTextField.class).setText("login");
            SwingComponentTestSupport.find(panel, "issueStatusFilter", JComboBox.class)
                    .setSelectedItem(IssueStatus.NEW);
            SwingComponentTestSupport.find(panel, "issuePriorityFilter", JComboBox.class)
                    .setSelectedItem(Priority.CRITICAL);
            SwingComponentTestSupport.find(panel, "searchIssuesButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "registerIssueButton", JButton.class).doClick();
            JTable table = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            SwingComponentTestSupport.find(panel, "openIssueDetailButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "deletedIssuesButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "statisticsButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "issueListBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "issueListLogoutButton", JButton.class).doClick();
        });

        assertEquals(new IssueSearchRequest("login", IssueStatus.NEW, Priority.CRITICAL), searchRef.get());
        assertEquals(new IssueRegisterRequest("New issue", "New issue description", Priority.MINOR), registerRef.get());
        assertEquals(7L, openRef.get());
        assertEquals(1, deletedIssueClicks.get());
        assertEquals(1, statisticsClicks.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("opens selected issue on double click")
    void opensSelectedIssueOnDoubleClick() throws Exception {
        var openRef = new AtomicLong();

        SwingComponentTestSupport.onEdt(() -> {
            IssueListPanel panel = new IssueListPanel(
                    userResult("tester1", Role.TESTER),
                    new FixedIssueDialogs(),
                    new IssueListPanel.IssueListActions(
                            (source, request) -> {
                            },
                            (source, request) -> {
                            },
                            openRef::set,
                            () -> {
                            },
                            () -> {
                            },
                            () -> {
                            },
                            () -> {
                            }));
            panel.showProject(project());
            panel.showIssues(List.of(issueSummary(9L, "ISSUE-9", "Login bug", IssueStatus.NEW, Priority.CRITICAL)));
            JTable table = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
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

        assertEquals(9L, openRef.get());
    }

    @Test
    @DisplayName("shows fallback text for blank error messages")
    void showsFallbackTextForBlankErrors() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueListPanel panel = panel(new FixedIssueDialogs());
            panel.showMessage(" ", true);

            JLabel message = SwingComponentTestSupport.find(panel, "issueListMessage", JLabel.class);

            assertEquals("Issue list failed. Please try again.", message.getText());
        });
    }

    @Test
    @DisplayName("renders issue list screen snapshot")
    void rendersIssueListSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueListPanel panel = panel(new FixedIssueDialogs());
            panel.showProject(project());
            panel.showIssues(List.of(
                    issueSummary(1L, "ISSUE-1", "Login bug", IssueStatus.NEW, Priority.CRITICAL),
                    issueSummary(2L, "ISSUE-2", "Profile typo", IssueStatus.ASSIGNED, Priority.MINOR)));
            panel.setRegisterEnabled(true);
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage initial = render(panel, Path.of("build/reports/ui/issue-list-panel.png"));
            assertTrue(hasRenderedContent(initial));

            JTable table = SwingComponentTestSupport.find(panel, "issueListTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            BufferedImage selected = render(panel, Path.of("build/reports/ui/issue-list-panel-selected.png"));
            assertTrue(hasRenderedContent(selected));

            IssueListPanel plPanel = panel(Role.PL, new FixedIssueDialogs());
            plPanel.showProject(project());
            plPanel.showIssues(List.of(
                    issueSummary(1L, "ISSUE-1", "Login bug", IssueStatus.NEW, Priority.CRITICAL),
                    issueSummary(2L, "ISSUE-2", "Profile typo", IssueStatus.ASSIGNED, Priority.MINOR)));
            plPanel.setRegisterEnabled(true);
            plPanel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(plPanel);

            assertEquals(
                    true,
                    SwingComponentTestSupport.find(plPanel, "deletedIssuesButton", JButton.class).isVisible());
            BufferedImage pl = render(plPanel, Path.of("build/reports/ui/issue-list-panel-pl.png"));
            assertTrue(hasRenderedContent(pl));
        });
    }

    private static IssueListPanel panel(FixedIssueDialogs dialogs) {
        return panel(Role.DEV, dialogs);
    }

    private static IssueListPanel panel(Role role, FixedIssueDialogs dialogs) {
        return new IssueListPanel(
                userResult(role == Role.PL ? "pl1" : "dev1", role),
                dialogs,
                new IssueListPanel.IssueListActions(
                        (source, request) -> {
                        },
                        (source, request) -> {
                        },
                        issueId -> {
                        },
                        () -> {
                        },
                        () -> {
                        },
                        () -> {
                        },
                        () -> {
                        }));
    }

    private static ProjectResult project() {
        return new ProjectResult(1L, "Alpha", "Alpha project", "admin", NOW, NOW);
    }

    private static IssueSummary issueSummary(
            long id,
            String issueId,
            String title,
            IssueStatus status,
            Priority priority) {
        return new IssueSummary(
                id,
                issueId,
                1L,
                status,
                priority,
                title,
                "dev1",
                null,
                null,
                NOW,
                NOW);
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

    private static BufferedImage render(IssueListPanel panel, Path output) throws java.io.IOException {
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

    private static final class FixedIssueDialogs implements IssueDialogs {

        @Override
        public Optional<IssueRegisterRequest> requestRegister(IssueListPanel parent) {
            return Optional.of(new IssueRegisterRequest("New issue", "New issue description", Priority.MINOR));
        }
    }
}
