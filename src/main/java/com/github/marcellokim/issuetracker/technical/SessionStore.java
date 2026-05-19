package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.domain.User;
import java.util.Objects;
import java.util.Optional;

public final class SessionStore {

    private User currentUser;

    public void startSession(User user) {
        currentUser = Objects.requireNonNull(user, "user");
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void clear() {
        currentUser = null;
    }
}
