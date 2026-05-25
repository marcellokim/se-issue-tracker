package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserResponse(
        String loginId,
        String name,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public UserResponse {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static UserResponse from(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new UserResponse(
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
