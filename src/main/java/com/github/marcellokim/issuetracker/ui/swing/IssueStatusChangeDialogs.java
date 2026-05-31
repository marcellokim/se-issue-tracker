package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class IssueStatusChangeDialogs {

    private static final int COMMENT_ROWS = 5;
    private static final int COMMENT_COLUMNS = 36;

    private IssueStatusChangeDialogs() {
    }

    static Optional<IssueStatusChangeRequest> prompt(Component parent, String action, IssueStatus targetStatus) {
        JTextArea commentArea = new JTextArea(COMMENT_ROWS, COMMENT_COLUMNS);
        JPanel form = form(action, targetStatus, commentArea);
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form,
                    "Change issue status",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            String comment = commentArea.getText().trim();
            if (!comment.isBlank()) {
                return Optional.of(new IssueStatusChangeRequest(targetStatus, comment));
            }
            JOptionPane.showMessageDialog(
                    parent,
                    "Comment is required.",
                    "Change issue status",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static JPanel form(String action, IssueStatus targetStatus) {
        return form(action, targetStatus, new JTextArea(COMMENT_ROWS, COMMENT_COLUMNS));
    }

    private static JPanel form(String action, IssueStatus targetStatus, JTextArea commentArea) {
        commentArea.setName("statusChangeCommentArea");
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JLabel title = new JLabel(IssueStatusChangeActions.label(action) + " -> " + targetStatus);
        title.setName("statusChangeDialogTitle");
        JPanel form = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        form.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP));
        form.add(title, BorderLayout.NORTH);
        form.add(new JScrollPane(commentArea), BorderLayout.CENTER);
        return form;
    }
}
