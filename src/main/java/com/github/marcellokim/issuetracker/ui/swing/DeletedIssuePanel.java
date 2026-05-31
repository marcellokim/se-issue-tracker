package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

final class DeletedIssuePanel extends JPanel implements DeletedIssueView {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DEFAULT_ERROR_MESSAGE = "Deleted issue management failed. Please try again.";
    private static final String[] ISSUE_COLUMNS = {"ID", "Issue", "Status", "Priority", "Title", "Reporter", "Updated"};
    private static final int[] ISSUE_COLUMN_WIDTHS = {64, 96, 100, 92, 320, 112, 144};
    private static final Color SELECTION_BACKGROUND = new Color(219, 234, 254);

    private final transient DeletedIssueActions actions;
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel countLabel = new JLabel("Deleted issues 0/0");
    private final DefaultTableModel issueTableModel = SwingPanelSections.readOnlyTableModel(ISSUE_COLUMNS);
    private final JTable issueTable = table();
    private final JButton restoreButton = new JButton("Restore selected");
    private final JButton purgeButton = new JButton("Purge selected");
    private final List<IssueSummary> issues = new ArrayList<>();
    private boolean busy;

    DeletedIssuePanel(UserResult user, DeletedIssueActions actions) {
        Objects.requireNonNull(user, "user");
        this.actions = Objects.requireNonNull(actions, "actions");

        setName("deletedIssuePanel");
        setLayout(new BorderLayout(SwingStyles.SECTION_GAP, SwingStyles.SECTION_GAP));
        setBackground(SwingStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING,
                SwingStyles.OUTER_PADDING));

        add(topSection(user), BorderLayout.NORTH);
        add(tableSection(), BorderLayout.CENTER);
        add(actionBar(), BorderLayout.SOUTH);
        updateActionState();
    }

    @Override
    public void showDeletedIssues(int maxRetentionLimit, List<IssueSummary> issues) {
        Objects.requireNonNull(issues, "issues");
        List<IssueSummary> snapshot = List.copyOf(issues);
        List<Object[]> rows = issueRows(snapshot);
        SwingPanelSections.runOnEdt(() -> {
            Long selectedIssueId = selectedIssue().map(IssueSummary::id).orElse(null);
            this.issues.clear();
            this.issues.addAll(snapshot);
            countLabel.setText("Deleted issues " + snapshot.size() + "/" + maxRetentionLimit);
            replaceRows(rows);
            restoreSelection(selectedIssueId);
            updateActionState();
        });
    }

    @Override
    public void showMessage(String message, boolean error) {
        SwingPanelSections.runOnEdt(() -> SwingPanelSections.updateMessage(
                messageLabel,
                message,
                error,
                DEFAULT_ERROR_MESSAGE));
    }

    void setBusy(boolean busy) {
        SwingPanelSections.runOnEdt(() -> {
            this.busy = busy;
            issueTable.setEnabled(!busy);
            updateActionState();
        });
    }

    private JPanel topSection(UserResult user) {
        JPanel panel = new JPanel(new BorderLayout(0, SwingStyles.SECTION_GAP));
        panel.setOpaque(false);
        panel.add(SwingPanelSections.managementHeader(
                new SwingPanelSections.HeaderLabels(
                        "Deleted issue management",
                        "deletedIssueTitle",
                        "deletedIssueUser",
                        "deletedIssueMessage",
                        "deletedIssueBackButton",
                        "deletedIssueLogoutButton"),
                user,
                messageLabel,
                new SwingPanelSections.NavigationActions(actions.onBack(), actions.onLogout())), BorderLayout.NORTH);
        return panel;
    }

    private JPanel tableSection() {
        JPanel section = new JPanel(new BorderLayout(0, SwingStyles.ROW_GAP));
        section.setBackground(SwingStyles.SURFACE);
        section.setBorder(SwingStyles.surfaceBorder());

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Deleted issues");
        SwingStyles.applySectionTitle(title);
        countLabel.setName("deletedIssueCount");
        SwingStyles.applyMuted(countLabel);
        heading.add(title, BorderLayout.WEST);
        heading.add(countLabel, BorderLayout.EAST);
        section.add(heading, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(issueTable);
        scrollPane.setColumnHeaderView(issueTable.getTableHeader());
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel actionBar() {
        JPanel panel = new JPanel();
        panel.setBackground(SwingStyles.SURFACE);
        panel.setBorder(SwingStyles.surfaceBorder());

        restoreButton.setName("restoreDeletedIssueButton");
        restoreButton.addActionListener(event -> selectedIssue()
                .ifPresent(issue -> actions.onRestore().accept(this, issue)));
        panel.add(restoreButton);

        purgeButton.setName("purgeDeletedIssueButton");
        purgeButton.addActionListener(event -> selectedIssue()
                .ifPresent(issue -> actions.onPurge().accept(this, issue)));
        panel.add(purgeButton);
        return panel;
    }

    private JTable table() {
        JTable table = new JTable(issueTableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                return SwingPanelSections.stripedTableCell(
                        this,
                        super.prepareRenderer(renderer, row, column),
                        row,
                        SELECTION_BACKGROUND);
            }
        };
        SwingPanelSections.configureReadOnlyTable(table, "deletedIssueTable", SELECTION_BACKGROUND);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionState();
            }
        });
        SwingPanelSections.applyColumnWidths(table, ISSUE_COLUMN_WIDTHS);
        return table;
    }

    private Optional<IssueSummary> selectedIssue() {
        int selectedRow = issueTable.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }
        int modelRow = issueTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= issues.size()) {
            return Optional.empty();
        }
        return Optional.of(issues.get(modelRow));
    }

    private void replaceRows(List<Object[]> rows) {
        issueTableModel.setRowCount(0);
        for (Object[] row : rows) {
            issueTableModel.addRow(row);
        }
    }

    private void restoreSelection(Long selectedIssueId) {
        if (selectedIssueId == null) {
            return;
        }
        for (int row = 0; row < issues.size(); row++) {
            if (selectedIssueId == issues.get(row).id()) {
                int viewRow = issueTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    issueTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }

    private void updateActionState() {
        boolean enabled = !busy && selectedIssue().isPresent();
        restoreButton.setEnabled(enabled);
        purgeButton.setEnabled(enabled);
    }

    private static List<Object[]> issueRows(List<IssueSummary> issues) {
        return issues.stream()
                .map(issue -> new Object[]{
                        issue.id(),
                        issue.issueId(),
                        issue.status(),
                        issue.priority(),
                        issue.title(),
                        issue.reporterId(),
                        DATE_TIME_FORMATTER.format(issue.updatedAt())
                })
                .toList();
    }

    @FunctionalInterface
    interface PanelIssueConsumer {

        void accept(DeletedIssuePanel panel, IssueSummary issue);
    }

    record DeletedIssueActions(
            PanelIssueConsumer onRestore,
            PanelIssueConsumer onPurge,
            Runnable onBack,
            Runnable onLogout) {

        DeletedIssueActions {
            Objects.requireNonNull(onRestore, "onRestore");
            Objects.requireNonNull(onPurge, "onPurge");
            Objects.requireNonNull(onBack, "onBack");
            Objects.requireNonNull(onLogout, "onLogout");
        }
    }
}
