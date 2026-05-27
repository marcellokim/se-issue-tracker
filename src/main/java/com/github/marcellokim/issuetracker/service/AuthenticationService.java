package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.util.Objects;
import java.util.Optional;

public final class AuthenticationService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final CurrentUserSession session;

    public AuthenticationService(
            UserRepository users,
            PasswordHasher passwordHasher,
            CurrentUserSession sessionStore) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.session = Objects.requireNonNull(sessionStore, "session");
    }

    public Optional<User> currentUser() {
        return session.currentLoginId()
                .flatMap(users::findByLoginId)
                .filter(User::isActive);
    }

    public void logout() {
        session.clear();
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
                    session.start(user.getLoginId());
                    return AuthenticationResult.success(user);
                })
                .orElseGet(() -> AuthenticationResult.failure("Invalid ID or password."));
    }
}
