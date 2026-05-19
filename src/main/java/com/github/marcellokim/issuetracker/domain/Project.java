package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Project {

    private final long id;
    private final String name;
    private final String description;
    private final String managedById;
    private final LocalDateTime createdDate;
    private final LocalDateTime updatedAt;
    private final List<Issue> issues = new ArrayList<>();
    private final List<User> participants = new ArrayList<>();

    public static Project create(long id, String name, String description, String managedById,
                                  LocalDateTime createdDate, LocalDateTime updatedAt) {
        return new Project(id, name, description, managedById, createdDate, updatedAt);
    }

    private Project(long id, String name, String description, String managedById,
                    LocalDateTime createdDate, LocalDateTime updatedAt) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(managedById, "managedById");
        this.id = id;
        this.name = name;
        this.description = description;
        this.managedById = managedById;
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    // --- domain methods ---

    public Issue registerIssue(
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

    // --- getters ---

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String managedById() {
        return managedById;
    }

    public LocalDateTime createdDate() {
        return createdDate;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // --- collection accessors ---

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public List<User> getParticipants() {
        return Collections.unmodifiableList(participants);
    }
}
