package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.security.MessageDigest;
import java.util.Objects;

public final class AuthenticationService {

    private final UserRepository users;

    public AuthenticationService(UserRepository users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    public AuthenticationResult login(String loginId, String password) {
        if (loginId == null || loginId.isBlank() || password == null || password.isBlank()) {
            return AuthenticationResult.failure("ID and password are required.");
        }

        return users.findByLoginId(loginId.trim())
                .map(user -> {
                    if (!user.active()) {
                        return AuthenticationResult.failure("This account is inactive.");
                    }
                    if (!matches(password, user.password())) {
                        return AuthenticationResult.failure("Invalid ID or password.");
                    }
                    return AuthenticationResult.success(user);
                })
                .orElseGet(() -> AuthenticationResult.failure("Invalid ID or password."));
    }

    private static boolean matches(String password, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        byte[] expected = storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actual = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
