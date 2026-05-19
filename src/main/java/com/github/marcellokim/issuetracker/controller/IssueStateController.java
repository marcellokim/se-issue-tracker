package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.util.Objects;

public final class IssueStateController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final IssueRepository issueRepository;
    private final Clock clock;

    public IssueStateController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            IssueRepository issueRepository,
            Clock clock) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * ASSIGNED/FIXED/RESOLVED/CLOSED/REOPENED 상태 전이, 필수 comment 검증, 담당 role 검증을
     * 구현한다.
     */
}
