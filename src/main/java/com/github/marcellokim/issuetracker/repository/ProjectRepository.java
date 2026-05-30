package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Optional<Project> findById(long projectId);

    Optional<Project> findByName(String name);

    Project save(Project project);

    void deleteById(long projectId);

    void addParticipant(long projectId, String userLoginId);

    void removeParticipant(long projectId, String userLoginId);

    List<ProjectMember> findParticipants(long projectId);

    boolean existsByParticipant(String userLoginId);
}
