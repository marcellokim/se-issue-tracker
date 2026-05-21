package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.util.Objects;

public final class AccountService {

    private final PermissionPolicy permissionPolicy;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AccountService(
            PermissionPolicy permissionPolicy,
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    /*
     * Account management UC orchestration stays here so the controller can remain
     * a system-operation adapter when the admin account methods are implemented.
     */
}
