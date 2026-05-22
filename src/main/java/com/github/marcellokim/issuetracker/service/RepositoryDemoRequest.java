package com.github.marcellokim.issuetracker.service;

import java.util.Objects;

public record RepositoryDemoRequest(String adminLoginId, String projectName) {

    public static RepositoryDemoRequest seedDemo() {
        return new RepositoryDemoRequest("admin", "project1");
    }

    public RepositoryDemoRequest {
        adminLoginId = requireText(adminLoginId, "adminLoginId");
        projectName = requireText(projectName, "projectName");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return Objects.requireNonNull(value, fieldName).trim();
    }
}
