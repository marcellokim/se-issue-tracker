package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.util.Objects;

public final class ProjectController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ProjectController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * 프로젝트 생성/수정, PL 1명 제한 검증, 프로젝트 멤버 추가/제거 UC를 구현한다.
     */
    public void deleteProject(long projectId) {
        if (projectId <= 0L) {
            throw new IllegalArgumentException("projectId must be positive");
        }

        var user = authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
        permissionPolicy.assertCanManageProject(user);
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project was not found."));
        projectRepository.deleteById(projectId);
    }
}
