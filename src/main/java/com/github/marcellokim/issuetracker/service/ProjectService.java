package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class ProjectService {

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final Clock clock;

    private ProjectService(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static ProjectService create(
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            Clock clock) {
        return new ProjectService(projectRepository, issueRepository, userRepository, permissionPolicy, clock);
    }

    public List<Project> viewProjects(String currentUserId) {
        requireProjectAdmin(currentUserId);
        return projectRepository.findAll();
    }

    public Project viewProject(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        return findProject(projectId);
    }

    public List<ProjectMember> viewProjectParticipants(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        findProject(projectId);
        return projectRepository.findParticipants(projectId);
    }

    public ProjectDetail viewProjectDetail(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        Project project = findProject(projectId);
        List<ProjectMember> participants = projectRepository.findParticipants(projectId);
        return ProjectDetail.create(project, participants, issueRepository.findByProject(projectId));
    }

    public Project createProject(String name, String description, String currentUserId) {
        User admin = requireProjectAdmin(currentUserId);
        String projectName = requireProjectName(name);
        rejectDuplicateProjectName(projectName);

        LocalDateTime now = clock.now();
        return projectRepository.save(Project.create(
                projectName,
                description,
                admin.getLoginId(),
                now));
    }

    public void deleteProject(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        findProject(projectId);

        projectRepository.deleteById(projectId);
    }

    public void addProjectParticipant(long projectId, String loginId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        findProject(projectId);
        User participant = findUser(requireText(loginId, "loginId"));
        if (!participant.isActive()) {
            throw new IllegalArgumentException("Only active users can be added to a project.");
        }
        if (participant.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN cannot be added as a project participant.");
        }

        List<ProjectMember> participants = projectRepository.findParticipants(projectId);
        rejectDuplicateParticipant(participants, participant.getLoginId());
        rejectSecondProjectLeader(projectId, participant);

        projectRepository.addParticipant(projectId, participant.getLoginId());
    }

    public void removeProjectParticipant(long projectId, String loginId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        findProject(projectId);
        String participantId = requireText(loginId, "loginId");

        if (!isParticipant(projectRepository.findParticipants(projectId), participantId)) {
            throw new IllegalArgumentException("Project participant was not found.");
        }

        projectRepository.removeParticipant(projectId, participantId);
    }

    private User requireProjectAdmin(String currentUserId) {
        User user = findUser(currentUserId);
        permissionPolicy.assertCanManageProject(user);
        return user;
    }

    private Project findProject(long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project was not found."));
    }

    private User findUser(String loginId) {
        String requiredLoginId = requireText(loginId, "loginId");
        return userRepository.findById(requiredLoginId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    private void rejectDuplicateProjectName(String projectName) {
        projectRepository.findByName(projectName).ifPresent(existingProject -> {
            throw new IllegalArgumentException("Project name already exists.");
        });
    }

    private void rejectDuplicateParticipant(List<ProjectMember> participants, String loginId) {
        if (isParticipant(participants, loginId)) {
            throw new IllegalArgumentException("Project participant already exists.");
        }
    }

    private void rejectSecondProjectLeader(long projectId, User participant) {
        if (participant.getRole() != Role.PL) {
            return;
        }

        if (!userRepository.findByRole(projectId, Role.PL).isEmpty()) {
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

    private static boolean isParticipant(List<ProjectMember> participants, String loginId) {
        return participants.stream()
                .map(ProjectMember::userId)
                .anyMatch(loginId::equals);
    }
}
