package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Project {

    private final String projectId;
    private final String name;
    private final String description;
    private final LocalDateTime createdDate;
    private final List<Issue> issues = new ArrayList<>();
    private final List<User> participants = new ArrayList<>();

    private Project(String projectId, String name, String description, LocalDateTime createdDate) {
        this.projectId = requireText(projectId, "projectId");
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate must not be null");
    }

    public static Project create(String projectId, String name, String description, LocalDateTime createdDate) {
        return new Project(projectId, name, description, createdDate);
    }

    public Issue registerIssue( // Issue issue = project.registerIssue(...) 식으로 사용
            String issueId,
            String title,
            String description,
            Priority priority,
            User reporter,
            LocalDateTime now
    ) {
        var issue = Issue.create(issueId, title, description, priority, reporter, now);
        issues.add(issue);
        return issue;
    }

    //addIssue 메서드 삭제함.

    public void addParticipant(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (participants.stream().anyMatch(p -> p.getUserId().equals(user.getUserId()))) {
            throw new IllegalArgumentException("user is already a participant");
        }
        participants.add(user);
    }

    public void removeParticipant(User user) {
        Objects.requireNonNull(user, "user must not be null");
        boolean removed = participants.removeIf(p -> p.getUserId().equals(user.getUserId()));
        if (!removed) {
            throw new IllegalArgumentException("user is not a participant");
        }
    }

    // Getters-------------------------------------------------------------------
    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public List<User> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
    //---------------------------------------------------------------------------
}