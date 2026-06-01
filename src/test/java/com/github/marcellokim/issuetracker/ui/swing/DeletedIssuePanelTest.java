package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing deleted issue panel")
class DeletedIssuePanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders deleted issue rows and selection-sensitive actions")
    void rendersDeletedIssueRowsAndActions() throws Exception {
        AtomicLong restoreRef = new AtomicLong();
        AtomicLong purgeRef = new AtomicLong();
        AtomicInteger backClicks = new AtomicInteger();
        AtomicInteger logoutClicks = new AtomicInteger();

        SwingComponentTestSupport.onEdt(() -> {
            DeletedIssuePanel panel = panel(restoreRef, purgeRef, backClicks, logoutClicks);
            panel.showDeletedIssues(30, List.of(
                    issueSummary(7L, "ISSUE-7", "Removed login bug"),
                    issueSummary(8L, "ISSUE-8", "Removed profile typo")));

            JTable table = SwingComponentTestSupport.find(panel, "deletedIssueTable", JTable.class);
            JButton restore = SwingComponentTestSupport.find(panel, "restoreDeletedIssueButton", JButton.class);
            JButton purge = SwingComponentTestSupport.find(panel, "purgeDeletedIssueButton", JButton.class);

            assertEquals("Deleted issue management", SwingComponentTestSupport.find(panel, "deletedIssueTitle",
                    JLabel.class).getText());
            assertEquals("Deleted issues 2/30", SwingComponentTestSupport.find(panel, "deletedIssueCount",
                    JLabel.class).getText());
            assertEquals(2, table.getRowCount());
            assertEquals("ISSUE-7", table.getValueAt(0, 1));
            assertEquals("Removed login bug", table.getValueAt(0, 4));
            assertEquals(false, restore.isEnabled());
            assertEquals(false, purge.isEnabled());

            table.setRowSelectionInterval(1, 1);

            assertEquals(true, restore.isEnabled());
            assertEquals(true, purge.isEnabled());

            restore.doClick();
            purge.doClick();
            SwingComponentTestSupport.find(panel, "deletedIssueBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "deletedIssueLogoutButton", JButton.class).doClick();
        });

        assertEquals(8L, restoreRef.get());
        assertEquals(8L, purgeRef.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    @Test
    @DisplayName("shows fallback text for blank error messages")
    void showsFallbackTextForBlankErrors() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            DeletedIssuePanel panel = panel(new AtomicLong(), new AtomicLong(), new AtomicInteger(), new AtomicInteger());

            panel.showMessage(" ", true);

            assertEquals(
                    "Deleted issue management failed. Please try again.",
                    SwingComponentTestSupport.find(panel, "deletedIssueMessage", JLabel.class).getText());
        });
    }

    @Test
    @DisplayName("renders deleted issue screen snapshot")
    void rendersDeletedIssueSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            DeletedIssuePanel panel = panel(new AtomicLong(), new AtomicLong(), new AtomicInteger(), new AtomicInteger());
            panel.showDeletedIssues(30, List.of(
                    issueSummary(7L, "ISSUE-7", "Removed login bug"),
                    issueSummary(8L, "ISSUE-8", "Removed profile typo")));
            panel.setSize(SwingStyles.WINDOW_SIZE);
            layoutRecursively(panel);

            BufferedImage image = render(panel, Path.of("build/reports/ui/deleted-issue-panel.png"));

            assertTrue(hasRenderedContent(image));

            JTable table = SwingComponentTestSupport.find(panel, "deletedIssueTable", JTable.class);
            table.setRowSelectionInterval(0, 0);
            BufferedImage selected = render(panel, Path.of("build/reports/ui/deleted-issue-panel-selected.png"));

            assertTrue(hasRenderedContent(selected));

            panel.showMessage("Only PL can manage deleted issues.", true);
            BufferedImage error = render(panel, Path.of("build/reports/ui/deleted-issue-panel-error.png"));

            assertTrue(hasRenderedContent(error));
        });
    }

    private static DeletedIssuePanel panel(
            AtomicLong restoreRef,
            AtomicLong purgeRef,
            AtomicInteger backClicks,
            AtomicInteger logoutClicks) {
        return new DeletedIssuePanel(
                userResult("pl1", Role.PL),
                new DeletedIssuePanel.DeletedIssueActions(
                        (source, issue) -> restoreRef.set(issue.id()),
                        (source, issue) -> purgeRef.set(issue.id()),
                        backClicks::incrementAndGet,
                        logoutClicks::incrementAndGet));
    }

    private static IssueSummary issueSummary(long id, String issueId, String title) {
        return new IssueSummary(
                id,
                issueId,
                1L,
                IssueStatus.DELETED,
                Priority.CRITICAL,
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

    private static BufferedImage render(DeletedIssuePanel panel, Path output) throws java.io.IOException {
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
