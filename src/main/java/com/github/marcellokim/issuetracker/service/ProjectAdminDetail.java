package com.github.marcellokim.issuetracker.service;

import java.util.List;
import java.util.Objects;

public final class ProjectAdminDetail {

    private final ProjectResult project;
    private final List<ProjectMemberResult> participants;
    private final List<IssueSummary> issues;

    private ProjectAdminDetail(
            ProjectResult project,
            List<ProjectMemberResult> participants,
            List<IssueSummary> issues) {
        this.project = Objects.requireNonNull(project, "project");
        this.participants = List.copyOf(participants);
        this.issues = List.copyOf(issues);
    }

    public static ProjectAdminDetail create(
            ProjectResult project,
            List<ProjectMemberResult> participants,
            List<IssueSummary> issues) {
        return new ProjectAdminDetail(project, participants, issues);
    }

    public ProjectResult project() {
        return project;
    }

    public List<ProjectMemberResult> participants() {
        return participants;
    }

    public List<IssueSummary> issues() {
        return issues;
    }
}
