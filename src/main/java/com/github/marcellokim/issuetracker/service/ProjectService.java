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

    public ProjectService(
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

    // Non-Admin -> 단순 프로젝트 기본정보만
    public ProjectResult viewProjectNonAdminDetail(long projectId, String currentLoginId) {
        requireProjectId(projectId);
        User actor = findUser(currentLoginId);
        Project project = findProject(projectId);
        requireNonAdminProjectParticipant(actor, projectId);
        return ProjectResult.from(project);
    }

    // Admin 기능 -> 프로젝트 기본정보 + 참여자 정보
    public ProjectAdminDetail viewProjectAdminDetail(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        Project project = findProject(projectId);
        return ProjectAdminDetail.create(
                ProjectResult.from(project),
                participantResults(projectId));
    }

    // 일단 controller쪽에서 중복 기능인데 남기는쪽으로
    public List<ProjectMemberResult> viewProjectParticipants(long projectId, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        findProject(projectId);
        return participantResults(projectId);
    }

    public ProjectResult createProject(String name, String description, String currentUserId) {
        User admin = requireProjectAdmin(currentUserId);
        String projectName = requireProjectName(name);
        String projectDescription = requireProjectDescription(description);
        rejectProjectNameAlreadyUsed(projectName);

        LocalDateTime now = clock.now();
        return ProjectResult.from(projectRepository.save(Project.create(
                projectName,
                projectDescription,
                admin.getLoginId(),
                now)));
    }

    public ProjectResult renameProject(long projectId, String name, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        Project project = findProject(projectId);
        String projectName = requireProjectName(name);
        rejectProjectNameUsedByAnotherProject(projectName, projectId);
        if (Objects.equals(project.getName(), projectName)) {
            throw new IllegalArgumentException("Project name is same as current name.");
        }

        project.rename(projectName, clock.now());
        return ProjectResult.from(projectRepository.save(project));
    }

    public ProjectResult changeProjectDescription(long projectId, String description, String currentUserId) {
        requireProjectId(projectId);
        requireProjectAdmin(currentUserId);
        Project project = findProject(projectId);
        String projectDescription = requireProjectDescription(description);
        if (Objects.equals(project.getDescription(), projectDescription)) {
            throw new IllegalArgumentException("Project description is same as current description.");
        }

        project.changeDescription(projectDescription, clock.now());
        return ProjectResult.from(projectRepository.save(project));
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

        rejectActiveIssueAssigneeOrVerifier(projectId, participantId);
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
        return userRepository.findByLoginId(requiredLoginId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found."));
    }

    private void requireNonAdminProjectParticipant(User actor, long projectId) {
        Objects.requireNonNull(actor, "actor");

        if (!actor.isActive()) {
            throw new SecurityException("Only active users can view project.");
        }

        if (actor.getRole() == Role.ADMIN) {
            throw new SecurityException("ADMIN must use admin project detail.");
        }

        if (!isParticipant(projectRepository.findParticipants(projectId), actor.getLoginId())) {
            throw new SecurityException("Only project participants can view project.");
        }
    }

    private List<ProjectMemberResult> participantResults(long projectId) {
        return projectRepository.findParticipants(projectId).stream()
                .map(member -> ProjectMemberResult.from(member, findUser(member.userId())))
                .toList();
    }

    private void rejectProjectNameAlreadyUsed(String projectName) {
        projectRepository.findByName(projectName).ifPresent(existingProject -> {
            throw new IllegalArgumentException("Project name already exists.");
        });
    }

    private void rejectProjectNameUsedByAnotherProject(String projectName, long projectId) {
        projectRepository.findByName(projectName)
                .filter(existingProject -> existingProject.getId() != projectId)
                .ifPresent(existingProject -> {
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

    private void rejectActiveIssueAssigneeOrVerifier(long projectId, String participantId) {
        if (issueRepository.existsActiveAssignmentByProjectAndUser(projectId, participantId)) {
            throw new IllegalArgumentException(
                    "Issue assignee or verifier cannot be removed while assignment is active.");
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

    private static String requireProjectDescription(String description) {
        return requireText(description, "description");
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

    // private static IssueSummary toIssueSummary(Issue issue) {
    // return new IssueSummary(
    // issue.id(),
    // issue.getIssueId(),
    // issue.projectId(),
    // issue.status(),
    // issue.priority(),
    // issue.title(),
    // issue.reporterId(),
    // issue.assigneeId(),
    // issue.verifierId(),
    // issue.reportedDate(),
    // issue.updatedAt());
    // }

}
