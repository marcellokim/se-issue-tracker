package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.AssignmentCandidateResult;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue assignment dialogs")
class IssueAssignmentDialogsTest {

    @Test
    @DisplayName("renders assignment dialog form snapshot")
    void rendersAssignmentDialogFormSnapshot() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            IssueAssignmentDialogs.AssignmentForm form = IssueAssignmentDialogs.form(
                    IssueAssignmentMode.ASSIGN,
                    options());
            assertEquals(
                    "Assign issue",
                    SwingComponentTestSupport.find(form.panel(), "assignmentDialogTitle", JLabel.class).getText());
            assertEquals(1, SwingComponentTestSupport.find(
                    form.panel(),
                    "assignmentAssigneeBox",
                    JComboBox.class).getItemCount());
            assertEquals(1, SwingComponentTestSupport.find(
                    form.panel(),
                    "assignmentVerifierBox",
                    JComboBox.class).getItemCount());
            form.panel().setSize(560, 220);
            layoutRecursively(form.panel());

            BufferedImage image = render(form.panel(), Path.of("build/reports/ui/issue-assignment-dialog.png"));
            assertTrue(hasRenderedContent(image));
        });
    }

    @Test
    @DisplayName("change-only forms exclude current owners")
    void changeOnlyFormsExcludeCurrentOwners() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            AssignmentOptionsResult options = optionsWithCurrentOwners();

            IssueAssignmentDialogs.AssignmentForm reassignForm = IssueAssignmentDialogs.form(
                    IssueAssignmentMode.REASSIGN_DEV,
                    options);
            JComboBox<?> assigneeBox = SwingComponentTestSupport.find(
                    reassignForm.panel(),
                    "assignmentAssigneeBox",
                    JComboBox.class);
            assertEquals(1, assigneeBox.getItemCount());
            assertEquals("dev2", ((AssignmentCandidateResult) assigneeBox.getItemAt(0)).loginId());

            IssueAssignmentDialogs.AssignmentForm verifierForm = IssueAssignmentDialogs.form(
                    IssueAssignmentMode.CHANGE_TESTER,
                    options);
            JComboBox<?> verifierBox = SwingComponentTestSupport.find(
                    verifierForm.panel(),
                    "assignmentVerifierBox",
                    JComboBox.class);
            assertEquals(1, verifierBox.getItemCount());
            assertEquals("tester2", ((AssignmentCandidateResult) verifierBox.getItemAt(0)).loginId());
        });
    }

    private static AssignmentOptionsResult options() {
        AssignmentCandidateResult dev = new AssignmentCandidateResult(
                "dev1",
                "Dev One",
                Role.DEV,
                3,
                "recent resolver match");
        AssignmentCandidateResult tester = new AssignmentCandidateResult(
                "tester1",
                "Tester One",
                Role.TESTER,
                2,
                "recent verifier match");
        return new AssignmentOptionsResult(List.of(dev), List.of(tester), List.of(dev), List.of(tester));
    }

    private static AssignmentOptionsResult optionsWithCurrentOwners() {
        AssignmentCandidateResult currentDev = new AssignmentCandidateResult(
                "dev1",
                "Dev One",
                Role.DEV,
                0,
                "current assignee");
        AssignmentCandidateResult nextDev = new AssignmentCandidateResult(
                "dev2",
                "Dev Two",
                Role.DEV,
                2,
                "recommended by similarity");
        AssignmentCandidateResult currentTester = new AssignmentCandidateResult(
                "tester1",
                "Tester One",
                Role.TESTER,
                0,
                "current verifier");
        AssignmentCandidateResult nextTester = new AssignmentCandidateResult(
                "tester2",
                "Tester Two",
                Role.TESTER,
                2,
                "recommended by similarity");
        return new AssignmentOptionsResult(
                List.of(currentDev, nextDev),
                List.of(currentTester, nextTester),
                List.of(currentDev, nextDev),
                List.of(currentTester, nextTester));
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
