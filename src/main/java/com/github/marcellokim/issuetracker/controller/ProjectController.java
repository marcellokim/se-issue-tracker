package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.ProjectDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import java.util.List;
import java.util.Objects;

public final class ProjectController {

    private final AuthenticationService authenticationService;
    private final ProjectService projectService;

    // factory
    public static ProjectController create(
            AuthenticationService authenticationService,
            ProjectService projectService) {
        return new ProjectController(authenticationService, projectService);
    }

    private ProjectController(
            AuthenticationService authenticationService,
            ProjectService projectService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.projectService = Objects.requireNonNull(projectService, "projectService");
    }

    public List<ProjectResult> viewProjects() {
        User user = requireCurrentUser();
        return projectService.viewProjects(user.getLoginId());
    }

    public ProjectResult viewProject(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProject(projectId, user.getLoginId());
    }

    public List<ProjectMemberResult> viewProjectParticipants(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectParticipants(projectId, user.getLoginId());
    }

    public ProjectDetail viewProjectDetail(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectDetail(projectId, user.getLoginId());
    }

    public ProjectResult createProject(String name, String description) {
        User user = requireCurrentUser();
        return projectService.createProject(name, description, user.getLoginId());
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
