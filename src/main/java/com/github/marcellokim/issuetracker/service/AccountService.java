package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.time.LocalDateTime;
import java.util.Objects;

public final class AccountService {

    private final PermissionPolicy permissionPolicy;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AccountService(
            PermissionPolicy permissionPolicy,
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        this(permissionPolicy, userRepository, passwordHasher, new Clock());
    }

    public AccountService(
            PermissionPolicy permissionPolicy,
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public User createAccount(
            String loginId,
            String name,
            String password,
            Role role,
            String currentUserId) {
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageAccount(actor);
        Role newRole = Objects.requireNonNull(role, "role must not be null");
        requireNonAdminAccount(loginId, newRole);
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("Account already exists: " + loginId);
        }

        LocalDateTime now = clock.now();
        User user = User.create(
                loginId,
                name,
                passwordHasher.hash(password),
                newRole,
                now);
        return userRepository.save(user);
    }

    public User updateAccount(String loginId, String name, Role role, String currentUserId) {
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, currentUserId);
        User target = findUser(loginId);
        Role newRole = Objects.requireNonNull(role, "role must not be null");
        requireNonAdminTarget(target);
        requireNonAdminAccount(loginId, newRole);
        LocalDateTime now = clock.now();
        target.rename(name, now);
        target.changeRole(newRole, now);
        return userRepository.save(target);
    }

    public User activateAccount(String loginId, String currentUserId) {
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, currentUserId);
        User target = findUser(loginId);
        requireNonAdminTarget(target);
        target.activate(clock.now());
        return userRepository.save(target);
    }

    public User deactivateAccount(String loginId, String currentUserId) {
        User actor = findUser(currentUserId);
        permissionPolicy.assertCanManageAccount(actor);
        requireDifferentAccount(loginId, currentUserId);
        User target = findUser(loginId);
        requireNonAdminTarget(target);
        target.deactivate(clock.now());
        return userRepository.save(target);
    }

    private static void requireNonAdminAccount(String loginId, Role role) {
        if (isAdminLoginId(loginId) || role == Role.ADMIN) {
            throw new IllegalArgumentException("The admin account cannot be created or modified.");
        }
    }

    private static void requireNonAdminTarget(User target) {
        if (target.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("The admin account cannot be created or modified.");
        }
    }

    private static void requireDifferentAccount(String targetLoginId, String currentUserId) {
        if (Objects.equals(targetLoginId, currentUserId)) {
            throw new IllegalArgumentException("Admin must manage another account.");
        }
    }

    private static boolean isAdminLoginId(String loginId) {
        return loginId != null && "admin".equalsIgnoreCase(loginId.trim());
    }

    private User findUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + loginId));
    }
}
