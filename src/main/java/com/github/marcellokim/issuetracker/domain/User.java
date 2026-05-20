package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    // loginId를 시스템 식별자로 겸용 (DCD: loginId가 유일 식별자)
    private final String loginId;
    private final String name;
    private final String password;
    private final Role role;
    private boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User create(String loginId, String name, String password, Role role,
                               boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(loginId, name, password, role, active, createdAt, updatedAt);
    }

    private User(
            String loginId,
            String name,
            String password,
            Role role,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.loginId = requireText(loginId, "loginId");
        this.name = requireText(name, "name");
        this.password = requireText(password, "password");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // --- domain methods ---

    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
    }

    public void deactivate() {
        active = false;
    }

    // --- getters ---

    // getUserId()는 loginId를 반환 (하위 호환)
    public String getUserId() {
        return loginId;
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

    // --- record-style accessors (기존 호환) ---

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
