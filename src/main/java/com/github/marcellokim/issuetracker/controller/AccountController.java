package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.util.Objects;

public final class AccountController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AccountController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * Admin 계정 생성/수정/비활성화 UC의 입력 검증, DTO 변환, 감사 이력 처리를 구현한다.
     */
}
