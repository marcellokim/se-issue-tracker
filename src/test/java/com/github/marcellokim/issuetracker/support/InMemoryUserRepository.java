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
        Arrays.stream(users).forEach(user -> this.users.put(user.getUserId(), user));
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public List<User> findActiveDevelopers() {
        return findActiveByRole(Role.DEV);
    }

    @Override
    public List<User> findActiveTesters() {
        return findActiveByRole(Role.TESTER);
    }

    private List<User> findActiveByRole(Role role) {
        return users.values().stream()
                .filter(User::isActive)
                .filter(user -> user.hasRole(role))
                .toList();
    }
}
