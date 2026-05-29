package com.github.marcellokim.issuetracker.service;

import java.util.List;
import java.util.Objects;

public final class ProjectAdminDetail {

    private final ProjectResult project;
    private final List<ProjectMemberResult> participants;

    private ProjectAdminDetail(
            ProjectResult project,
            List<ProjectMemberResult> participants) {
        this.project = Objects.requireNonNull(project, "project");
        this.participants = List.copyOf(participants);
    }

    public static ProjectAdminDetail create(
            ProjectResult project,
            List<ProjectMemberResult> participants) {
        return new ProjectAdminDetail(project, participants);
    }

    public ProjectResult project() {
        return project;
    }

    public List<ProjectMemberResult> participants() {
        return participants;
    }
}
