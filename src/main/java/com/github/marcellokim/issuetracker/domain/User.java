package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class User {

    private final String userId;
    private final String loginId;
    private final String name;
    private final String password;
    private final Role role;
    private boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public User(String userId, String loginId, String name, String passwordHash, Role role) {
        this.userId = requireText(userId, "userId");
        this.loginId = requireText(loginId, "loginId");
        this.name = requireText(name, "name");
        this.password = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = true;
        this.createdAt = null;
        this.updatedAt = null;
    }

    public User(
            String loginId,
            String password,
            Role role,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.loginId = requireText(loginId, "loginId");
        this.userId = this.loginId;
        this.name = this.loginId;
        this.password = requireText(password, "password");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
    }

    public void deactivate() {
        active = false;
    }

    public String loginId() {
        return loginId;
    }

    public String password() {
        return password;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
