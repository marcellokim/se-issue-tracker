package com.github.marcellokim.issuetracker.domain;

import java.util.Objects;

public final class User {

    private final String userId;
    private final String loginId;
    private final String name;
    private final String passwordHash;
    private final Role role;
    private boolean active;

    public User(String userId, String loginId, String name, String passwordHash, Role role) {
        this.userId = requireText(userId, "userId");
        this.loginId = requireText(loginId, "loginId");
        this.name = requireText(name, "name");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = true;
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
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
