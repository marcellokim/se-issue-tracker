package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.util.Objects;
import java.util.Optional;

public final class AuthenticationService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionStore sessionStore;

    public AuthenticationService(UserRepository users) {
        this(users, new PasswordHasher(), new SessionStore());
    }

    public AuthenticationService(
            UserRepository users,
            PasswordHasher passwordHasher,
            SessionStore sessionStore
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    public Optional<User> currentUser() {
        return sessionStore.currentUser();
    }

    public void logout() {
        sessionStore.clear();
    }

    public AuthenticationResult logIn(String loginId, String password) {
        return login(loginId, password);
    }

    public AuthenticationResult login(String loginId, String password) {
        if (loginId == null || loginId.isBlank() || password == null || password.isBlank()) {
            return AuthenticationResult.failure("ID and password are required.");
        }

        return users.findByLoginId(loginId.trim())
                .map(user -> {
                    if (!passwordHasher.matches(password, user.getPasswordHash())) {
                        return AuthenticationResult.failure("Invalid ID or password.");
                    }
                    if (!user.isActive()) {
                        return AuthenticationResult.failure("This account is inactive.");
                    }
                    sessionStore.startSession(user);
                    return AuthenticationResult.success(user);
                })
                .orElseGet(() -> AuthenticationResult.failure("Invalid ID or password."));
    }
}
