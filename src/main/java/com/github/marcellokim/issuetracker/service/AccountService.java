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
     * Admin account UC 조율은 service에 둔다. 이후 계정 관리 메서드가 추가되어도
     * controller는 system operation adapter 역할만 유지한다.
     */
}
