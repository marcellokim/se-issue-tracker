package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryProjectRepository implements ProjectRepository {

    private static final LocalDateTime DEFAULT_JOINED_AT = LocalDateTime.of(2026, 5, 1, 0, 0);

    private final Map<Long, Project> projects = new LinkedHashMap<>();
    private final Map<Long, List<ProjectMember>> participants = new LinkedHashMap<>();
    private long nextProjectId = 100L;
    private long lastDeletedProjectId;
    private boolean rejectWrites;

    public InMemoryProjectRepository(Project... projects) {
        for (Project project : projects) {
            withProject(project);
        }
    }

    public InMemoryProjectRepository rejectWrites() {
        rejectWrites = true;
        return this;
    }

    public InMemoryProjectRepository withProject(Project project) {
        Objects.requireNonNull(project, "project");
        projects.put(project.getId(), project);
        nextProjectId = Math.max(nextProjectId, project.getId() + 1L);
        return this;
    }

    public InMemoryProjectRepository withParticipant(long projectId, String loginId) {
        return withParticipant(projectId, loginId, DEFAULT_JOINED_AT);
    }

    public InMemoryProjectRepository withParticipant(long projectId, String loginId, LocalDateTime joinedAt) {
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(joinedAt, "joinedAt");
        addParticipantIfAbsent(ProjectMember.create(projectId, loginId, joinedAt));
        return this;
    }

    public InMemoryProjectRepository withParticipants(long projectId, List<ProjectMember> projectMembers) {
        Objects.requireNonNull(projectMembers, "projectMembers");
        participants.put(projectId, new ArrayList<>());
        projectMembers.forEach(this::addParticipantIfAbsent);
        return this;
    }

    public long lastDeletedProjectId() {
        return lastDeletedProjectId;
    }

    public List<String> participantIds(long projectId) {
        return findParticipants(projectId).stream()
                .map(ProjectMember::userId)
                .toList();
    }

    @Override
    public Optional<Project> findById(long projectId) {
        return Optional.ofNullable(projects.get(projectId));
    }

    @Override
    public Optional<Project> findByName(String name) {
        return projects.values().stream()
                .filter(project -> project.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<Project> findAll() {
        return projects.values().stream()
                .sorted(Comparator.comparingLong(Project::getId))
                .toList();
    }

    @Override
    public Project save(Project project) {
        requireWritesAllowed("save");
        Objects.requireNonNull(project, "project");
        Project saved = project.getId() == 0L
                ? Project.fromPersistence(
                        nextProjectId++,
                        project.getName(),
                        project.getDescription(),
                        project.getManagedByLoginId(),
                        project.getCreatedDate(),
                        project.getUpdatedAt())
                : project;
        projects.put(saved.getId(), saved);
        nextProjectId = Math.max(nextProjectId, saved.getId() + 1L);
        return saved;
    }

    @Override
    public void deleteById(long projectId) {
        requireWritesAllowed("deleteById");
        lastDeletedProjectId = projectId;
        projects.remove(projectId);
        participants.remove(projectId);
    }

    @Override
    public void addParticipant(long projectId, String userLoginId) {
        requireWritesAllowed("addParticipant");
        withParticipant(projectId, userLoginId);
    }

    @Override
    public void removeParticipant(long projectId, String userLoginId) {
        requireWritesAllowed("removeParticipant");
        participants.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                .removeIf(participant -> participant.userId().equals(userLoginId));
    }

    @Override
    public List<ProjectMember> findParticipants(long projectId) {
        return participants.getOrDefault(projectId, List.of()).stream()
                .sorted(Comparator.comparing(ProjectMember::userId))
                .toList();
    }

    @Override
    public boolean existsByParticipant(String userLoginId) {
        Objects.requireNonNull(userLoginId, "userLoginId");
        return participants.values().stream()
                .flatMap(List::stream)
                .map(ProjectMember::userId)
                .anyMatch(userLoginId::equals);
    }

    private void requireWritesAllowed(String methodName) {
        if (rejectWrites) {
            throw new UnsupportedOperationException("Unexpected ProjectRepository call: " + methodName);
        }
    }

    private void addParticipantIfAbsent(ProjectMember member) {
        Objects.requireNonNull(member, "member");
        List<ProjectMember> projectParticipants = participants.computeIfAbsent(
                member.projectId(),
                ignored -> new ArrayList<>());
        boolean alreadyExists = projectParticipants.stream()
                .anyMatch(existing -> existing.userId().equals(member.userId()));
        if (!alreadyExists) {
            projectParticipants.add(member);
        }
    }
}
