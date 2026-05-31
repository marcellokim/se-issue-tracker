package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.Optional;

interface ProjectDetailDialogs {

    Optional<String> requestRename(ProjectDetailPanel parent, ProjectResult project);

    Optional<String> requestDescription(ProjectDetailPanel parent, ProjectResult project);

    Optional<String> requestParticipantLoginId(ProjectDetailPanel parent);

    boolean confirmRemove(ProjectDetailPanel parent, ProjectMemberResult selectedParticipant);
}
