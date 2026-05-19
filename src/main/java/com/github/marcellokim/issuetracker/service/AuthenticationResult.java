package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import java.util.Objects;

public record AuthenticationResult(
        boolean success,
        User user,
        String message
) {

    public static AuthenticationResult success(User user) {
        return new AuthenticationResult(true, Objects.requireNonNull(user, "user"), "Login succeeded.");
    }

    public static AuthenticationResult failure(String message) {
        return new AuthenticationResult(false, null, message);
    }
}
