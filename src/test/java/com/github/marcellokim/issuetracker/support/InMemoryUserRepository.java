package com.github.marcellokim.issuetracker.support;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<Long, Set<String>> projectMembers = new LinkedHashMap<>();

    public InMemoryUserRepository(User... users) {
        Arrays.stream(users).forEach(user -> this.users.put(user.getLoginId(), user));
    }

    public InMemoryUserRepository withProjectMembers(long projectId, String... loginIds) {
        projectMembers.computeIfAbsent(projectId, ignored -> new HashSet<>()).addAll(Arrays.asList(loginIds));
        return this;
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
    public List<User> findByRole(long projectId, Role role) {
        Set<String> members = projectMembers.get(projectId);
        return users.values().stream()
                .filter(user -> user.getRole() == role)
                .filter(user -> projectMembers.isEmpty() || members != null && members.contains(user.getLoginId()))
                .toList();
    }

    @Override
    public List<User> findActiveByRole(long projectId, Role role) {
        Set<String> members = projectMembers.get(projectId);
        return users.values().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() == role)
                .filter(user -> projectMembers.isEmpty() || members != null && members.contains(user.getLoginId()))
                .toList();
    }

    @Override
    public boolean existsActiveProjectMember(long projectId, String loginId) {
        Set<String> members = projectMembers.get(projectId);
        return findByLoginId(loginId)
                .filter(User::isActive)
                .filter(user -> projectMembers.isEmpty() || members != null && members.contains(user.getLoginId()))
                .isPresent();
    }

    @Override
    public User save(User user) {
        users.put(user.getLoginId(), user);
        return user;
    }

}
