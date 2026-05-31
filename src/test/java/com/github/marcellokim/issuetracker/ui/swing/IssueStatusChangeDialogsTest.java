package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue status change dialogs")
class IssueStatusChangeDialogsTest {

    @Test
    @DisplayName("renders status change dialog form snapshot")
    void rendersStatusChangeDialogFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            JPanel form = IssueStatusChangeDialogs.form("MARK_FIXED", IssueStatus.FIXED);
            assertEquals(
                    "Mark fixed -> FIXED",
                    SwingComponentTestSupport.find(form, "statusChangeDialogTitle", JLabel.class).getText());
            form.setSize(520, 240);
            layoutRecursively(form);

            BufferedImage image = render(form, Path.of("build/reports/ui/issue-status-change-dialog.png"));
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

    private static BufferedImage render(JPanel panel, Path output) throws java.io.IOException {
        BufferedImage image = new BufferedImage(520, 240, BufferedImage.TYPE_INT_RGB);
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
