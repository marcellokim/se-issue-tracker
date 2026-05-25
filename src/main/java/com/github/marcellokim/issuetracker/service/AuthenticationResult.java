package com.github.marcellokim.issuetracker.service;

import java.util.Objects;

public record AuthenticationResult(
        boolean success,
        UserResult user,
        String message
) {

    public static AuthenticationResult success(com.github.marcellokim.issuetracker.domain.User user) {
        return new AuthenticationResult(true, UserResult.from(user), "Login succeeded.");
    }

    public static AuthenticationResult failure(String message) {
        return new AuthenticationResult(false, null, Objects.requireNonNull(message, "message"));
    }
}
