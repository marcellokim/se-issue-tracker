package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue comment dialogs")
class IssueCommentDialogsTest {

    @Test
    @DisplayName("renders comment content dialog form snapshot")
    void rendersCommentDialogFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueCommentDialogs.CommentForm form =
                    IssueCommentDialogs.form("Edit comment", "Existing comment body");
            assertEquals(
                    "Edit comment",
                    SwingComponentTestSupport.find(form.panel(), "commentDialogTitle", JLabel.class).getText());
            assertEquals(
                    "Existing comment body",
                    SwingComponentTestSupport.find(form.panel(), "commentContentArea", JTextArea.class).getText());
            form.panel().setSize(560, 260);
            layoutRecursively(form.panel());

            BufferedImage image = render(form.panel(), Path.of("build/reports/ui/issue-comment-dialog.png"));
            assertTrue(hasRenderedContent(image));
        });
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutRecursively(nested);
            }
        }
    }

    private static BufferedImage render(Container panel, Path output) throws java.io.IOException {
        BufferedImage image = new BufferedImage(560, 260, BufferedImage.TYPE_INT_RGB);
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
