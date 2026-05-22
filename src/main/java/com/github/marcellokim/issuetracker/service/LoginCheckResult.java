package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Objects;
import java.util.Optional;

public record LoginCheckResult(
        String loginId,
        Optional<AccountSummary> account,
        boolean success,
        String message
) {

    public LoginCheckResult {
        loginId = Objects.requireNonNull(loginId, "loginId");
        account = Objects.requireNonNull(account, "account");
        message = Objects.requireNonNull(message, "message");
    }

    public record AccountSummary(Role role, boolean active) {

        public AccountSummary {
            role = Objects.requireNonNull(role, "role");
        }
    }
}
