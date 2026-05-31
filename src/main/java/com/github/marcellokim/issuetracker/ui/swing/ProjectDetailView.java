package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import java.util.List;

interface ProjectDetailView {

    void showDetail(ProjectAdminDetail detail);

    void showParticipants(List<ProjectMemberResult> participants);

    void showMessage(String message, boolean error);
}
