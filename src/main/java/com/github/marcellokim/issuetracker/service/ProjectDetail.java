package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import java.util.List;
import java.util.Objects;

public final class ProjectDetail {

    private final Project project;
    private final List<ProjectMember> participants;
    private final List<Issue> issues;

    private ProjectDetail(
            Project project,
            List<ProjectMember> participants,
            List<Issue> issues) {
        this.project = Objects.requireNonNull(project, "project");
        this.participants = List.copyOf(Objects.requireNonNull(participants, "participants"));
        this.issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static ProjectDetail create(
            Project project,
            List<ProjectMember> participants,
            List<Issue> issues) {
        return new ProjectDetail(project, participants, issues);
    }

    public Project project() {
        return project;
    }

    public List<ProjectMember> participants() {
        return participants;
    }

    public List<Issue> issues() {
        return issues;
    }
}
