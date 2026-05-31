package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import java.util.Objects;

final class ProjectDetailPresenter {

    private final ProjectController projectController;
    private final ProjectDetailView view;

    ProjectDetailPresenter(ProjectController projectController, ProjectDetailView view) {
        this.projectController = Objects.requireNonNull(projectController, "projectController");
        this.view = Objects.requireNonNull(view, "view");
    }

    void loadProject(long projectId) {
        refreshDetail(projectId, " ");
    }

    void renameProject(long projectId, String name) {
        try {
            ProjectResult renamed = projectController.renameProject(projectId, name);
            refreshDetail(projectId, "Project renamed: " + renamed.name());
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void changeProjectDescription(long projectId, String description) {
        try {
            ProjectResult changed = projectController.changeProjectDescription(projectId, description);
            refreshDetail(projectId, "Project description changed: " + changed.name());
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    void addProjectParticipant(long projectId, String loginId) {
        runParticipantRefresh(
                projectId,
                "Participant added: " + loginId,
                () -> projectController.addProjectParticipant(projectId, loginId));
    }

    void removeProjectParticipant(long projectId, String loginId) {
        runParticipantRefresh(
                projectId,
                "Participant removed: " + loginId,
                () -> projectController.removeProjectParticipant(projectId, loginId));
    }

    private void refreshDetail(long projectId, String successMessage) {
        try {
            view.showDetail(projectController.viewProjectAdminDetail(projectId));
            view.showMessage(successMessage, false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }

    private void runParticipantRefresh(long projectId, String successMessage, Runnable action) {
        try {
            action.run();
            view.showParticipants(projectController.viewProjectParticipants(projectId));
            view.showMessage(successMessage, false);
        } catch (RuntimeException exception) {
            view.showMessage(exception.getMessage(), true);
        }
    }
}
