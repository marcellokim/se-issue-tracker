package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserResult(
        String loginId,
        String name,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public UserResult {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        role = Objects.requireNonNull(role, "role");
    }

    public static UserResult from(User user) {
        Objects.requireNonNull(user, "user");
        return new UserResult(
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
