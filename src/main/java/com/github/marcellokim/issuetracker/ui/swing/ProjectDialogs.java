package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import java.util.Optional;

interface ProjectDialogs {

    Optional<ProjectCreateRequest> requestCreate(ProjectManagementPanel parent);

    Optional<String> requestRename(ProjectManagementPanel parent, DashboardProjectSummary selectedProject);

    Optional<String> requestDescription(ProjectManagementPanel parent, DashboardProjectSummary selectedProject);

    boolean confirmDelete(ProjectManagementPanel parent, DashboardProjectSummary selectedProject);
}
