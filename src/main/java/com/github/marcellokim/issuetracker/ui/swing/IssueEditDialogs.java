package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Priority;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

final class IssueEditDialogs {

    private static final int TEXT_ROWS = 5;
    private static final int TEXT_COLUMNS = 36;
    private static final String EDIT_ISSUE_TITLE = "Edit issue";
    private static final String DELETE_ISSUE_TITLE = "Delete issue";

    private IssueEditDialogs() {
    }

    static Optional<IssueEditRequest> prompt(Component parent, IssueEditMode mode, IssueEditContext context) {
        return switch (mode) {
            case UPDATE -> promptUpdate(parent, context);
            case CHANGE_PRIORITY -> promptPriority(parent, context.priority());
            case SOFT_DELETE -> promptDelete(parent);
        };
    }

    static UpdateForm updateForm(IssueEditContext context) {
        JTextField titleField = new JTextField(context.title(), 32);
        titleField.setName("issueEditTitleField");
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, SwingStyles.FIELD_HEIGHT));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea descriptionArea = new JTextArea(context.description(), TEXT_ROWS, TEXT_COLUMNS);
        descriptionArea.setName("issueEditDescriptionArea");
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionPane = aligned(new JScrollPane(descriptionArea));

        JPanel fields = verticalFields();
        fields.add(fieldLabel("Title"));
        fields.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        fields.add(titleField);
        fields.add(Box.createVerticalStrut(SwingStyles.SECTION_GAP));
        fields.add(fieldLabel("Description"));
        fields.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        fields.add(descriptionPane);

        JPanel panel = formPanel(EDIT_ISSUE_TITLE, "issueEditDialogTitle");
        panel.add(fields, BorderLayout.CENTER);
        return new UpdateForm(panel, titleField, descriptionArea);
    }

    static PriorityForm priorityForm(Priority currentPriority) {
        JComboBox<Priority> priorityBox = new JComboBox<>(Priority.values());
        priorityBox.setName("priorityComboBox");
        priorityBox.setSelectedItem(currentPriority);
        priorityBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, SwingStyles.FIELD_HEIGHT));
        priorityBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel fields = verticalFields();
        fields.add(fieldLabel("Priority"));
        fields.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        fields.add(priorityBox);

        JPanel panel = formPanel("Change priority", "priorityDialogTitle");
        panel.add(fields, BorderLayout.CENTER);
        return new PriorityForm(panel, priorityBox);
    }

    static DeleteForm deleteForm() {
        JTextArea commentArea = new JTextArea(TEXT_ROWS, TEXT_COLUMNS);
        commentArea.setName("issueDeleteCommentArea");
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        JScrollPane commentPane = aligned(new JScrollPane(commentArea));

        JPanel panel = formPanel(DELETE_ISSUE_TITLE, "issueDeleteDialogTitle");
        JPanel fields = verticalFields();
        fields.add(fieldLabel("Comment"));
        fields.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        fields.add(commentPane);
        panel.add(fields, BorderLayout.CENTER);
        return new DeleteForm(panel, commentArea);
    }

    private static Optional<IssueEditRequest> promptUpdate(Component parent, IssueEditContext context) {
        UpdateForm form = updateForm(context);
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form.panel(),
                    EDIT_ISSUE_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            try {
                return Optional.of(IssueEditRequest.update(
                        form.titleField().getText(),
                        form.descriptionArea().getText()));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(parent, exception.getMessage(), EDIT_ISSUE_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Optional<IssueEditRequest> promptPriority(Component parent, Priority currentPriority) {
        PriorityForm form = priorityForm(currentPriority);
        int result = JOptionPane.showConfirmDialog(
                parent,
                form.panel(),
                "Change priority",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        return Optional.of(IssueEditRequest.changePriority((Priority) form.priorityBox().getSelectedItem()));
    }

    private static Optional<IssueEditRequest> promptDelete(Component parent) {
        DeleteForm form = deleteForm();
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form.panel(),
                    DELETE_ISSUE_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            try {
                return Optional.of(IssueEditRequest.softDelete(form.commentArea().getText()));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        parent,
                        exception.getMessage(),
                        DELETE_ISSUE_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JPanel formPanel(String titleText, String titleName) {
        JPanel panel = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        panel.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP));
        JLabel title = new JLabel(titleText);
        title.setName(titleName);
        SwingStyles.applySectionTitle(title);
        panel.add(title, BorderLayout.NORTH);
        return panel;
    }

    private static JPanel verticalFields() {
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        return fields;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static <T extends JComponent> T aligned(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    record UpdateForm(JPanel panel, JTextField titleField, JTextArea descriptionArea) {
    }

    record PriorityForm(JPanel panel, JComboBox<Priority> priorityBox) {
    }

    record DeleteForm(JPanel panel, JTextArea commentArea) {
    }
}
