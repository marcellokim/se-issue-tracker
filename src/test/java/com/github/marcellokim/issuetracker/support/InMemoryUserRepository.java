package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();

    public InMemoryUserRepository(User... users) {
        Arrays.stream(users).forEach(user -> this.users.put(user.loginId(), user));
    }

    @Override
    public Optional<User> findById(String userId) {
        return findByLoginId(userId);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return Optional.ofNullable(users.get(loginId));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public List<User> findActiveByRole(long projectId, Role role) {
        return users.values().stream()
                .filter(User::active)
                .filter(user -> user.role() == role)
                .toList();
    }

    @Override
    public User save(User user) {
        users.put(user.loginId(), user);
        return user;
    }

    @Override
    public void deactivate(String loginId) {
        findByLoginId(loginId).ifPresent(User::deactivate);
    }
}
