package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import java.util.List;
import java.util.Objects;

public final class ProjectController {

    private final AuthenticationService authenticationService;
    private final ProjectService projectService;

    public ProjectController(
            AuthenticationService authenticationService,
            ProjectService projectService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.projectService = Objects.requireNonNull(projectService, "projectService");
    }

    public ProjectResult viewProjectNonAdminDetail(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectNonAdminDetail(projectId, user.getLoginId());
    }

    public ProjectAdminDetail viewProjectAdminDetail(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectAdminDetail(projectId, user.getLoginId());
    }

    // viewProjectAdminDetail에서 프로젝트 멤버가지지만 일단 남겨
    public List<ProjectMemberResult> viewProjectParticipants(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectParticipants(projectId, user.getLoginId());
    }

    public ProjectResult createProject(String name, String description) {
        User user = requireCurrentUser();
        return projectService.createProject(name, description, user.getLoginId());
    }

    public ProjectResult renameProject(long projectId, String name) {
        User user = requireCurrentUser();
        return projectService.renameProject(projectId, name, user.getLoginId());
    }

    public ProjectResult changeProjectDescription(long projectId, String description) {
        User user = requireCurrentUser();
        return projectService.changeProjectDescription(projectId, description, user.getLoginId());
    }

    public void deleteProject(long projectId) {
        User user = requireCurrentUser();
        projectService.deleteProject(projectId, user.getLoginId());
    }

    public void addProjectParticipant(long projectId, String loginId) {
        User user = requireCurrentUser();
        projectService.addProjectParticipant(projectId, loginId, user.getLoginId());
    }

    public void removeProjectParticipant(long projectId, String loginId) {
        User user = requireCurrentUser();
        projectService.removeProjectParticipant(projectId, loginId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
