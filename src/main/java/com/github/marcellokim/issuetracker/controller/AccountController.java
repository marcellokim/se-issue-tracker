package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import java.util.Objects;

public final class AccountController {

    private final AuthenticationService authenticationService;
    private final AccountService accountService;

    public AccountController(
            AuthenticationService authenticationService,
            AccountService accountService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.accountService = Objects.requireNonNull(accountService, "accountService");
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * Admin 계정 생성/수정/비활성화 UC의 입력 검증, DTO 변환, 감사 이력 처리를 구현한다.
     */
}
