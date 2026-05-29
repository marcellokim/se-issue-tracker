package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.service.CurrentUserSession;
import java.util.Optional;

public final class SessionStore implements CurrentUserSession {

    private String currentLoginId;

    @Override
    public void start(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId must not be blank");
        }
        currentLoginId = loginId.trim();
    }

    @Override
    public Optional<String> currentLoginId() {
        return Optional.ofNullable(currentLoginId);
    }

    @Override
    public void clear() {
        currentLoginId = null;
    }
}
