package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;

interface AdminDashboardView {

    void showDashboard(List<DashboardProjectSummary> projects, List<UserResult> users);

    void showError(String message);
}
