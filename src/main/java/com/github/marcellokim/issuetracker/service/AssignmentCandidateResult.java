package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Objects;

public record AssignmentCandidateResult(
        String loginId,
        String name,
        Role role,
        int completedIssueCount,
        String reason) {

    public AssignmentCandidateResult {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        role = Objects.requireNonNull(role, "role");
        reason = Objects.requireNonNull(reason, "reason");
    }

}
