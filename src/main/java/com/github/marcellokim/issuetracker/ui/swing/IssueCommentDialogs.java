package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class IssueCommentDialogs {

    private static final int TEXT_ROWS = 5;
    private static final int TEXT_COLUMNS = 36;

    private IssueCommentDialogs() {
    }

    static Optional<IssueCommentRequest> prompt(
            Component parent,
            IssueCommentMode mode,
            IssueCommentSelection selection) {
        return switch (mode) {
            case ADD -> promptContent(parent, mode, null);
            case UPDATE -> promptContent(parent, mode, requireSelection(selection));
            case DELETE -> confirmDelete(parent, requireSelection(selection));
        };
    }

    static CommentForm form(String title, String initialContent) {
        JTextArea contentArea = new JTextArea(initialContent == null ? "" : initialContent, TEXT_ROWS, TEXT_COLUMNS);
        contentArea.setName("commentContentArea");
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        panel.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP));
        JLabel label = new JLabel(title);
        label.setName("commentDialogTitle");
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        return new CommentForm(panel, contentArea);
    }

    private static Optional<IssueCommentRequest> promptContent(
            Component parent,
            IssueCommentMode mode,
            IssueCommentSelection selection) {
        CommentForm form = form(label(mode), selection == null ? "" : selection.content());
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form.panel(),
                    label(mode),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            try {
                if (mode == IssueCommentMode.ADD) {
                    return Optional.of(IssueCommentRequest.add(form.contentArea().getText()));
                }
                return Optional.of(IssueCommentRequest.update(selection.commentId(), form.contentArea().getText()));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(parent, exception.getMessage(), label(mode), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Optional<IssueCommentRequest> confirmDelete(Component parent, IssueCommentSelection selection) {
        int result = JOptionPane.showConfirmDialog(
                parent,
                "Delete selected comment?",
                label(IssueCommentMode.DELETE),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        return Optional.of(IssueCommentRequest.delete(selection.commentId()));
    }

    private static IssueCommentSelection requireSelection(IssueCommentSelection selection) {
        if (selection == null) {
            throw new IllegalArgumentException("comment selection is required");
        }
        return selection;
    }

    private static String label(IssueCommentMode mode) {
        return switch (mode) {
            case ADD -> "Add comment";
            case UPDATE -> "Edit comment";
            case DELETE -> "Delete comment";
        };
    }

    record CommentForm(JPanel panel, JTextArea contentArea) {
    }
}
