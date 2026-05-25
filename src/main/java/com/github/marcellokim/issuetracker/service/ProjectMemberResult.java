package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import java.util.Objects;

public record ProjectMemberResult(
        long projectId,
        String userId,
        String userName,
        Role role,
        boolean active,
        LocalDateTime joinedAt
) {

    public ProjectMemberResult {
        if (projectId <= 0L) {
            throw new IllegalArgumentException("projectId must be positive");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("userName must not be blank");
        }
        role = Objects.requireNonNull(role, "role");
    }

    public static ProjectMemberResult from(ProjectMember member, User user) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(user, "user");
        return new ProjectMemberResult(
                member.projectId(),
                member.userId(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                member.joinedAt());
    }
}
