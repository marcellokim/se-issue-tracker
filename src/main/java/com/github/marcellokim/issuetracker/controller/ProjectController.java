package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProjectController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ProjectController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            Clock clock) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Project createProject(String name, String description) {
        User admin = requireProjectAdmin();
        String projectName = requireProjectName(name);
        rejectDuplicateProjectName(projectName, null);

        LocalDateTime now = clock.now();
        return projectRepository.save(new Project(
                0L,
                projectName,
                description,
                admin.loginId(),
                now,
                now));
    }

    public Project updateProject(long projectId, String name, String description) {
        requireProjectId(projectId);
        requireProjectAdmin();
        Project existingProject = requireProject(projectId);
        String projectName = requireProjectName(name);
        rejectDuplicateProjectName(projectName, projectId);

        return projectRepository.save(new Project(
                existingProject.id(),
                projectName,
                description,
                existingProject.managedById(),
                existingProject.createdDate(),
                clock.now()));
    }

    public void deleteProject(long projectId) {
        requireProjectId(projectId);
        requireProjectAdmin();
        requireProject(projectId);

        projectRepository.deleteById(projectId);
    }

    public void addProjectParticipant(long projectId, String userId) {
        requireProjectId(projectId);
        requireProjectAdmin();
        requireProject(projectId);
        User participant = requireUser(userId);
        if (!participant.active()) {
            throw new IllegalArgumentException("Only active users can be added to a project.");
        }

        List<ProjectMember> participants = projectRepository.findParticipants(projectId);
        rejectDuplicateParticipant(participants, participant.loginId());
        rejectSecondProjectLead(participants, participant);

        projectRepository.addParticipant(projectId, participant.loginId());
    }

    public void removeProjectParticipant(long projectId, String userId) {
        requireProjectId(projectId);
        requireProjectAdmin();
        requireProject(projectId);
        String participantId = requireText(userId, "userId");

        if (!isParticipant(projectRepository.findParticipants(projectId), participantId)) {
            throw new IllegalArgumentException("Project participant was not found.");
        }

        projectRepository.removeParticipant(projectId, participantId);
    }

    private User requireProjectAdmin() {
        User user = authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
        permissionPolicy.assertCanManageProject(user);
        return user;
    }

    private Project requireProject(long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project was not found."));
    }

    private User requireUser(String userId) {
        String requiredUserId = requireText(userId, "userId");
        return userRepository.findById(requiredUserId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    private void rejectDuplicateProjectName(String projectName, Long currentProjectId) {
        projectRepository.findByName(projectName).ifPresent(existingProject -> {
            if (currentProjectId == null || existingProject.id() != currentProjectId) {
                throw new IllegalArgumentException("Project name already exists.");
            }
        });
    }

    private void rejectDuplicateParticipant(List<ProjectMember> participants, String userId) {
        if (isParticipant(participants, userId)) {
            throw new IllegalArgumentException("Project participant already exists.");
        }
    }

    private void rejectSecondProjectLead(List<ProjectMember> participants, User participant) {
        if (participant.role() != Role.PL) {
            return;
        }

        boolean hasProjectLead = participants.stream()
                .map(ProjectMember::userId)
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .anyMatch(user -> user.role() == Role.PL);
        if (hasProjectLead) {
            throw new IllegalArgumentException("Only one PL can be assigned to a project.");
        }
    }

    private static void requireProjectId(long projectId) {
        if (projectId <= 0L) {
            throw new IllegalArgumentException("projectId must be positive");
        }
    }

    private static String requireProjectName(String name) {
        return requireText(name, "name");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static boolean isParticipant(List<ProjectMember> participants, String userId) {
        return participants.stream()
                .map(ProjectMember::userId)
                .anyMatch(userId::equals);
    }
}
