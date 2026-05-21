package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class ProjectMember {

    private final long projectId;
    private final String userId;
    private final LocalDateTime joinedAt;

    public static ProjectMember create(long projectId, String userId,
            LocalDateTime joinedAt) {
        // New project memberships are created by application flow before persistence.
        return new ProjectMember(projectId, userId, joinedAt);
    }

    public static ProjectMember fromPersistence(long projectId, String userId,
            LocalDateTime joinedAt) {
        // Persistence reconstruction keeps the stored membership row exactly as read.
        return new ProjectMember(projectId, userId, joinedAt);
    }

    private ProjectMember(long projectId, String userId, LocalDateTime joinedAt) {
        this.projectId = projectId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public long projectId() {
        return projectId;
    }

    public String userId() {
        return userId;
    }

    public LocalDateTime joinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProjectMember that)) {
            return false;
        }
        return projectId == that.projectId
                && Objects.equals(userId, that.userId)
                && Objects.equals(joinedAt, that.joinedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId, joinedAt);
    }

    @Override
    public String toString() {
        return "ProjectMember[projectId=" + projectId
                + ", userId=" + userId
                + ", joinedAt=" + joinedAt + "]";
    }
}
