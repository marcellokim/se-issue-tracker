package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Priority;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue edit dialogs")
class IssueEditDialogsTest {

    @Test
    @DisplayName("renders issue update form snapshot")
    void rendersIssueUpdateFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueEditDialogs.UpdateForm form =
                    IssueEditDialogs.updateForm(new IssueEditContext(
                            "Login bug",
                            "Login fails with valid credentials.",
                            Priority.CRITICAL));
            assertEquals(
                    "Edit issue",
                    SwingComponentTestSupport.find(form.panel(), "issueEditDialogTitle", JLabel.class).getText());
            assertEquals(
                    "Login bug",
                    SwingComponentTestSupport.find(form.panel(), "issueEditTitleField", JTextField.class).getText());
            form.panel().setSize(560, 300);
            layoutRecursively(form.panel());

            BufferedImage image = render(form.panel(), Path.of("build/reports/ui/issue-edit-dialog.png"), 560, 300);
            assertTrue(hasRenderedContent(image));
        });
    }

    @Test
    @DisplayName("renders priority change form snapshot")
    void rendersPriorityChangeFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueEditDialogs.PriorityForm form = IssueEditDialogs.priorityForm(Priority.CRITICAL);
            assertEquals(
                    "Change priority",
                    SwingComponentTestSupport.find(form.panel(), "priorityDialogTitle", JLabel.class).getText());
            assertEquals(
                    Priority.CRITICAL,
                    SwingComponentTestSupport.find(form.panel(), "priorityComboBox", JComboBox.class)
                            .getSelectedItem());
            form.panel().setSize(420, 160);
            layoutRecursively(form.panel());

            BufferedImage image = render(
                    form.panel(),
                    Path.of("build/reports/ui/issue-priority-dialog.png"),
                    420,
                    160);
            assertTrue(hasRenderedContent(image));
        });
    }

    @Test
    @DisplayName("renders soft delete form snapshot")
    void rendersSoftDeleteFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueEditDialogs.DeleteForm form = IssueEditDialogs.deleteForm();
            assertEquals(
                    "Delete issue",
                    SwingComponentTestSupport.find(form.panel(), "issueDeleteDialogTitle", JLabel.class).getText());
            assertEquals(
                    "",
                    SwingComponentTestSupport.find(form.panel(), "issueDeleteCommentArea", JTextArea.class).getText());
            form.panel().setSize(560, 260);
            layoutRecursively(form.panel());

            BufferedImage image = render(form.panel(), Path.of("build/reports/ui/issue-delete-dialog.png"), 560, 260);
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

    private static BufferedImage render(Container panel, Path output, int width, int height)
            throws java.io.IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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
