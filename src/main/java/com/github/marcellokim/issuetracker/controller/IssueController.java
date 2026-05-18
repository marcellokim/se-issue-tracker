package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.util.Objects;

public final class IssueController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IssueController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            ProjectRepository projectRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * 이슈 등록/검색/상세 조회/수정 UC의 입력 검증, DTO 변환, UI 요청 처리를 구현한다.
     */
}
