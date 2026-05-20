package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
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

    public List<Project> viewProjects() {
        User user = requireCurrentUser();
        return projectService.viewProjects(user.loginId());
    }

    public Project viewProject(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProject(projectId, user.loginId());
    }

    public List<ProjectMember> viewProjectParticipants(long projectId) {
        User user = requireCurrentUser();
        return projectService.viewProjectParticipants(projectId, user.loginId());
    }

    public Project createProject(String name, String description) {
        User user = requireCurrentUser();
        return projectService.createProject(name, description, user.loginId());
    }

    public void deleteProject(long projectId) {
        User user = requireCurrentUser();
        projectService.deleteProject(projectId, user.loginId());
    }

    public void addProjectParticipant(long projectId, String loginId) {
        User user = requireCurrentUser();
        projectService.addProjectParticipant(projectId, loginId, user.loginId());
    }

    public void removeProjectParticipant(long projectId, String loginId) {
        User user = requireCurrentUser();
        projectService.removeProjectParticipant(projectId, loginId, user.loginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
