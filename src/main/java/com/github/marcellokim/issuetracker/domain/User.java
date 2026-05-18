package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record User(
        String loginId,
        String password,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public User {
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(role, "role");
    }

    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
    }

    public boolean isActive() {
        return active;
    }
}
