package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class IssueTableRows {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DefaultTableModel model;
    private final List<IssueSummary> issues = new ArrayList<>();

    IssueTableRows(DefaultTableModel model) {
        this.model = model;
    }

    void replaceKeepingSelection(JTable table, List<IssueSummary> newIssues) {
        Optional<Long> selectedIssueId = selectedIssue(table).map(IssueSummary::id);
        issues.clear();
        issues.addAll(newIssues);
        model.setRowCount(0);
        newIssues.forEach(issue -> model.addRow(new Object[]{
                issue.id(),
                issue.issueId(),
                issue.status(),
                issue.priority(),
                issue.title(),
                issue.reporterId(),
                valueOrDash(issue.assigneeId()),
                valueOrDash(issue.verifierId()),
                DATE_TIME_FORMATTER.format(issue.updatedAt())
        }));
        selectedIssueId.ifPresent(issueId -> restoreSelection(table, issueId));
    }

    Optional<IssueSummary> selectedIssue(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= issues.size()) {
            return Optional.empty();
        }
        return Optional.of(issues.get(modelRow));
    }

    private void restoreSelection(JTable table, long selectedIssueId) {
        for (int row = 0; row < issues.size(); row++) {
            if (issues.get(row).id() == selectedIssueId) {
                int viewRow = table.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
