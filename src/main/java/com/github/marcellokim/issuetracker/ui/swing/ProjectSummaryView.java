package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.List;

interface ProjectSummaryView {

    void showProjects(List<DashboardProjectSummary> projects);

    void showMessage(String message, boolean error);
}
