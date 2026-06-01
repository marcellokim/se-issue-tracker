package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class ProjectTableRows {

    private final DefaultTableModel model;
    private final List<DashboardProjectSummary> projects = new ArrayList<>();

    ProjectTableRows(DefaultTableModel model) {
        this.model = model;
    }

    void replaceKeepingSelection(JTable table, List<DashboardProjectSummary> newProjects) {
        Optional<Long> selectedProjectId = selectedProject(table).map(DashboardProjectSummary::projectId);
        projects.clear();
        projects.addAll(newProjects);
        model.setRowCount(0);
        newProjects.forEach(project -> model.addRow(new Object[]{
                project.projectId(),
                project.projectName(),
                project.projectDescription(),
                project.memberCount(),
                project.visibleIssueCount()
        }));
        selectedProjectId.ifPresent(projectId -> restoreSelection(table, projectId));
    }

    Optional<DashboardProjectSummary> selectedProject(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return Optional.empty();
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= projects.size()) {
            return Optional.empty();
        }
        return Optional.of(projects.get(modelRow));
    }

    private void restoreSelection(JTable table, long selectedProjectId) {
        for (int row = 0; row < projects.size(); row++) {
            if (projects.get(row).projectId() == selectedProjectId) {
                int viewRow = table.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }
}
