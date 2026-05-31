package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

final class IssueDependencyDialogs {

    private static final int FIELD_WIDTH = 220;
    private static final String ADD_DEPENDENCY_TITLE = "Add dependency";

    private IssueDependencyDialogs() {
    }

    static Optional<IssueDependencyRequest> prompt(
            Component parent,
            IssueDependencyMode mode,
            IssueDependencySelection selection,
            long defaultBlockedIssueId) {
        return switch (mode) {
            case ADD -> promptAdd(parent, defaultBlockedIssueId);
            case REMOVE -> confirmRemove(parent, requireSelection(selection));
        };
    }

    static DependencyForm form(long defaultBlockingIssueId, long defaultBlockedIssueId) {
        JTextField blockingIssueField = new JTextField(defaultValue(defaultBlockingIssueId));
        blockingIssueField.setName("dependencyBlockingIssueField");
        JTextField blockedIssueField = new JTextField(defaultValue(defaultBlockedIssueId));
        blockedIssueField.setName("dependencyBlockedIssueField");

        JLabel title = new JLabel(ADD_DEPENDENCY_TITLE);
        title.setName("dependencyDialogTitle");

        JPanel fields = SwingPanelSections.formPanel(
                FIELD_WIDTH,
                new JLabel("Blocking issue ID"),
                blockingIssueField,
                new JLabel("Blocked issue ID"),
                blockedIssueField);
        JPanel panel = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        panel.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP));
        panel.add(title, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        return new DependencyForm(panel, blockingIssueField, blockedIssueField);
    }

    private static Optional<IssueDependencyRequest> promptAdd(Component parent, long defaultBlockedIssueId) {
        DependencyForm form = form(0L, defaultBlockedIssueId);
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form.panel(),
                    ADD_DEPENDENCY_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            try {
                long blockingIssueId = parseIssueId(form.blockingIssueField().getText(), "blockingIssueId");
                long blockedIssueId = parseIssueId(form.blockedIssueField().getText(), "blockedIssueId");
                return Optional.of(IssueDependencyRequest.add(blockingIssueId, blockedIssueId));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        parent,
                        exception.getMessage(),
                        ADD_DEPENDENCY_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Optional<IssueDependencyRequest> confirmRemove(
            Component parent,
            IssueDependencySelection selection) {
        int result = JOptionPane.showConfirmDialog(
                parent,
                "Remove selected dependency?",
                "Remove dependency",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        return Optional.of(IssueDependencyRequest.remove(
                selection.blockingIssueId(),
                selection.blockedIssueId()));
    }

    private static IssueDependencySelection requireSelection(IssueDependencySelection selection) {
        if (selection == null) {
            throw new IllegalArgumentException("dependency selection is required");
        }
        return selection;
    }

    private static long parseIssueId(String text, String fieldName) {
        try {
            long value = Long.parseLong(text == null ? "" : text.trim());
            if (value <= 0L) {
                throw new IllegalArgumentException(fieldName + " must be a positive number");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a positive number", exception);
        }
    }

    private static String defaultValue(long value) {
        return value > 0L ? Long.toString(value) : "";
    }

    record DependencyForm(
            JPanel panel,
            JTextField blockingIssueField,
            JTextField blockedIssueField) {
    }
}
