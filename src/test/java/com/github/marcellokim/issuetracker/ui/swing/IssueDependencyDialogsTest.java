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
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue dependency dialogs")
class IssueDependencyDialogsTest {

    @Test
    @DisplayName("renders dependency add form snapshot")
    void rendersDependencyAddFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueDependencyDialogs.DependencyForm form =
                    IssueDependencyDialogs.form(12L, 7L);
            assertEquals(
                    "Add dependency",
                    SwingComponentTestSupport.find(form.panel(), "dependencyDialogTitle", JLabel.class).getText());
            assertEquals(
                    "12",
                    SwingComponentTestSupport.find(
                            form.panel(),
                            "dependencyBlockingIssueField",
                            JTextField.class).getText());
            assertEquals(
                    "7",
                    SwingComponentTestSupport.find(
                            form.panel(),
                            "dependencyBlockedIssueField",
                            JTextField.class).getText());
            form.panel().setSize(560, 220);
            layoutRecursively(form.panel());

            BufferedImage image = render(form.panel(), Path.of("build/reports/ui/issue-dependency-dialog.png"));
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
        BufferedImage image = new BufferedImage(560, 220, BufferedImage.TYPE_INT_RGB);
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
