package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

final class SwingPanelSections {

    private static final String FORM_PAIR_ERROR = "Form components must be label-field pairs.";
    private static final Color EVEN_ROW_BACKGROUND = Color.WHITE;
    private static final Color ODD_ROW_BACKGROUND = new Color(248, 250, 252);

    private SwingPanelSections() {
    }

    static JPanel managementHeader(
            HeaderLabels labels,
            UserResult user,
            JLabel messageLabel,
            NavigationActions navigation) {
        Objects.requireNonNull(labels, "labels");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(messageLabel, "messageLabel");
        Objects.requireNonNull(navigation, "navigation");

        JPanel header = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        header.setBackground(SwingStyles.SURFACE);
        header.setBorder(SwingStyles.surfaceBorder());

        JPanel topRow = new JPanel(new BorderLayout(SwingStyles.SECTION_GAP, 0));
        topRow.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(labels.title());
        title.setName(labels.titleName());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyTitle(title);
        titles.add(title);

        JLabel userLabel = new JLabel(user.name() + " (" + user.role() + ")");
        userLabel.setName(labels.userName());
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(userLabel);
        titles.add(Box.createVerticalStrut(SwingStyles.ROW_GAP));
        titles.add(userLabel);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        JButton backButton = new JButton("Back");
        backButton.setName(labels.backButtonName());
        backButton.addActionListener(event -> navigation.onBack().run());
        nav.add(backButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setName(labels.logoutButtonName());
        logoutButton.addActionListener(event -> navigation.onLogout().run());
        nav.add(logoutButton);

        messageLabel.setName(labels.messageName());
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        SwingStyles.applyMuted(messageLabel);

        topRow.add(titles, BorderLayout.CENTER);
        topRow.add(nav, BorderLayout.EAST);
        header.add(topRow, BorderLayout.CENTER);
        header.add(messageLabel, BorderLayout.SOUTH);
        return header;
    }

    static JPanel formPanel(int fieldWidth, Component... components) {
        if (components.length % 2 != 0) {
            throw new IllegalArgumentException(FORM_PAIR_ERROR);
        }
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        for (int index = 0; index < components.length; index += 2) {
            constraints.gridy = index / 2;
            constraints.gridx = 0;
            constraints.weightx = 0.0;
            panel.add(components[index], constraints);
            constraints.gridx = 1;
            constraints.weightx = 1.0;
            Component field = components[index + 1];
            field.setPreferredSize(new Dimension(fieldWidth, SwingStyles.FIELD_HEIGHT));
            panel.add(field, constraints);
        }
        return panel;
    }

    static void updateMessage(
            JLabel messageLabel,
            String message,
            boolean error,
            String defaultErrorMessage) {
        String text = message;
        if (text == null || text.isBlank()) {
            text = error ? defaultErrorMessage : " ";
        }
        messageLabel.setText(text);
        messageLabel.setForeground(error ? SwingStyles.ERROR_TEXT : SwingStyles.MUTED_TEXT);
    }

    static Component stripedTableCell(
            JTable table,
            Component component,
            int row,
            Color selectionBackground) {
        component.setForeground(SwingStyles.BODY_TEXT);
        if (table.isRowSelected(row)) {
            component.setBackground(selectionBackground);
        } else {
            component.setBackground(row % 2 == 0 ? EVEN_ROW_BACKGROUND : ODD_ROW_BACKGROUND);
        }
        return component;
    }

    static void configureReadOnlyTable(JTable table, String name, Color selectionBackground) {
        table.setName(name);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(selectionBackground);
        table.setSelectionForeground(SwingStyles.BODY_TEXT);
        table.getTableHeader().setReorderingAllowed(false);
    }

    static JPanel tableSection(String title, JTable table) {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JLabel label = new JLabel(title);
        SwingStyles.applySectionTitle(label);
        section.add(label, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    static void applyColumnWidths(JTable table, int[] widths) {
        if (table.getColumnCount() != widths.length) {
            return;
        }
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
    }

    static DefaultTableModel readOnlyTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    record HeaderLabels(
            String title,
            String titleName,
            String userName,
            String messageName,
            String backButtonName,
            String logoutButtonName) {

        HeaderLabels {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(titleName, "titleName");
            Objects.requireNonNull(userName, "userName");
            Objects.requireNonNull(messageName, "messageName");
            Objects.requireNonNull(backButtonName, "backButtonName");
            Objects.requireNonNull(logoutButtonName, "logoutButtonName");
        }
    }

    record NavigationActions(Runnable onBack, Runnable onLogout) {

        NavigationActions {
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }
}
