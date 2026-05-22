package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    private final String loginId;
    private String name;
    private final String passwordHash;
    private Role role;
    private boolean active;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static User create(
            String loginId,
            String name,
            String passwordHash,
            Role role,
            LocalDateTime now) {
        LocalDateTime timestamp = requireTime(now, "now");
        return new User(loginId, name, passwordHash, role, true, timestamp, timestamp);
    }

    public static User fromPersistence(
            String loginId,
            String name,
            String passwordHash,
            Role role,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new User(loginId, name, passwordHash, role, active, createdAt, updatedAt);
    }

    private User(
            String loginId,
            String name,
            String passwordHash,
            Role role,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.loginId = requireText(loginId, "loginId");
        this.name = requireText(name, "name");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String name, LocalDateTime changedAt) {
        this.name = requireText(name, "name");
        this.updatedAt = requireTime(changedAt, "changedAt");
    }

    public void changeRole(Role role, LocalDateTime changedAt) {
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.updatedAt = requireTime(changedAt, "changedAt");
    }

    public void deactivate(LocalDateTime changedAt) {
        this.active = false;
        this.updatedAt = requireTime(changedAt, "changedAt");
    }

    public void activate(LocalDateTime changedAt) {
        this.active = true;
        this.updatedAt = requireTime(changedAt, "changedAt");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static LocalDateTime requireTime(LocalDateTime value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }
}
