package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing deleted issue dialogs")
class DeletedIssueDialogsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("builds restore comment form")
    void buildsRestoreCommentForm() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            JPanel form = DeletedIssueDialogs.restoreCommentForm(issueSummary());

            assertEquals(
                    "Restore ISSUE-7",
                    SwingComponentTestSupport.find(form, "deletedIssueRestoreTitle", JLabel.class).getText());
            assertEquals(
                    "Restore reason",
                    SwingComponentTestSupport.find(form, "deletedIssueRestoreCommentLabel", JLabel.class).getText());
            assertEquals(
                    "",
                    SwingComponentTestSupport.find(form, "deletedIssueRestoreCommentArea", JTextArea.class)
                            .getText());
        });
    }

    @Test
    @DisplayName("renders restore dialog snapshot")
    void rendersRestoreDialogSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            JPanel form = DeletedIssueDialogs.restoreCommentForm(issueSummary());
            form.setSize(420, 180);
            layoutRecursively(form);

            BufferedImage image = render(form, Path.of("build/reports/ui/deleted-issue-restore-dialog.png"));

            assertTrue(hasRenderedContent(image));
        });
    }

    private static IssueSummary issueSummary() {
        return new IssueSummary(
                7L,
                "ISSUE-7",
                1L,
                IssueStatus.DELETED,
                Priority.CRITICAL,
                "Removed login bug",
                "dev1",
                null,
                null,
                NOW,
                NOW);
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static BufferedImage render(JPanel panel, Path output) throws java.io.IOException {
        BufferedImage image = new BufferedImage(420, 180, BufferedImage.TYPE_INT_RGB);
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
        int white = Color.WHITE.getRGB();
        int contentPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != white) {
                    contentPixels++;
                }
            }
        }
        return contentPixels > 1_000;
    }
}
