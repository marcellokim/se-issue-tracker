package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Objects;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class DeletedIssueDialogs {

    private static final int COMMENT_ROWS = 5;
    private static final int COMMENT_COLUMNS = 36;
    private static final String RESTORE_DIALOG_TITLE = "Restore deleted issue";
    private static final String PURGE_DIALOG_TITLE = "Purge deleted issue";

    private DeletedIssueDialogs() {
    }

    static JPanel restoreCommentForm(IssueSummary issue) {
        return restoreForm(issue).panel();
    }

    private static RestoreForm restoreForm(IssueSummary issue) {
        Objects.requireNonNull(issue, "issue");
        JPanel panel = SwingPanelSections.dialogFormPanel("Restore " + issue.issueId(), "deletedIssueRestoreTitle");
        JPanel fields = SwingPanelSections.verticalFieldPanel();

        JLabel commentLabel = SwingPanelSections.fieldLabel("Restore reason");
        commentLabel.setName("deletedIssueRestoreCommentLabel");
        JTextArea commentArea = new JTextArea(COMMENT_ROWS, COMMENT_COLUMNS);
        commentArea.setName("deletedIssueRestoreCommentArea");
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        fields.add(commentLabel);
        fields.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        fields.add(new JScrollPane(commentArea));
        panel.add(fields, BorderLayout.CENTER);
        return new RestoreForm(panel, commentArea);
    }

    static final class JOptionPaneDeletedIssuePrompt implements DeletedIssuePrompt {

        @Override
        public Optional<String> requestRestoreComment(Component parent, IssueSummary issue) {
            return promptRestore(parent, issue);
        }

        @Override
        public boolean confirmPurge(Component parent, IssueSummary issue) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    "Permanently purge " + issue.issueId() + "?",
                    PURGE_DIALOG_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return result == JOptionPane.OK_OPTION;
        }

        private static Optional<String> promptRestore(Component parent, IssueSummary issue) {
            RestoreForm form = restoreForm(issue);
            while (true) {
                int result = JOptionPane.showConfirmDialog(
                        parent,
                        form.panel(),
                        RESTORE_DIALOG_TITLE,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (result != JOptionPane.OK_OPTION) {
                    return Optional.empty();
                }
                String comment = form.commentArea().getText();
                if (comment != null && !comment.isBlank()) {
                    return Optional.of(comment.trim());
                }
                JOptionPane.showMessageDialog(
                        parent,
                        "Restore reason must not be blank.",
                        RESTORE_DIALOG_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private record RestoreForm(JPanel panel, JTextArea commentArea) {
    }
}
