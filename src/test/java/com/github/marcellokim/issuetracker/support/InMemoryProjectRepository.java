package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        projects.put(project.getId(), project);
        nextProjectId = Math.max(nextProjectId, project.getId() + 1L);
        return this;
    }

    public InMemoryProjectRepository withParticipant(long projectId, String loginId) {
        return withParticipant(projectId, loginId, DEFAULT_JOINED_AT);
    }

    public InMemoryProjectRepository withParticipant(long projectId, String loginId, LocalDateTime joinedAt) {
        participants.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                .add(ProjectMember.create(projectId, loginId, joinedAt));
        return this;
    }

    public InMemoryProjectRepository withParticipants(long projectId, List<ProjectMember> projectMembers) {
        participants.put(projectId, new ArrayList<>(projectMembers));
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
        return List.copyOf(projects.values());
    }

    @Override
    public Project save(Project project) {
        requireWritesAllowed("save");
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
        return List.copyOf(participants.getOrDefault(projectId, List.of()));
    }

    @Override
    public boolean existsByParticipant(String userLoginId) {
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
}
