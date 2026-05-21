package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    // loginId를 시스템 식별자로 겸용 (DCD: loginId가 유일 식별자)
    private final String loginId;
    private String name;
    private final String passwordHash;
    private Role role;
    private boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static User create(String loginId, String name, String passwordHash, Role role, LocalDateTime now) {
        // 신규 계정은 저장소 복원과 달리 활성 상태와 동일한 생성/수정 시각으로 시작함.
        LocalDateTime timestamp = requireTime(now, "now");
        return new User(loginId, name, passwordHash, role, true, timestamp, timestamp);
    }

    public static User fromPersistence(String loginId, String name, String passwordHash, Role role,
            boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        // DB row 복원은 비활성 여부와 저장된 timestamp를 그대로 보존해야 함.
        return new User(loginId, name, passwordHash, role, active, createdAt, updatedAt);
    }

    // private 생성자
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

    // --- domain methods ---

    // 실제 계정 역할과 파라미터 계정 역할이 같은지 비교
    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
    }

    // 계정 비활성화
    public void deactivate() {
        active = false;
    }

    // getter
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

    // setter
    public void setName(String name) {
        this.name = name;
    }

    public void setRole(Role role) {
        this.role = role;
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
