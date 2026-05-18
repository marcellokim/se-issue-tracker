package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import java.util.Objects;

public final class AssignmentController {

    private final AuthenticationService authenticationService;
    private final PermissionPolicy permissionPolicy;
    private final AssignmentRecommendationService assignmentRecommendationService;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AssignmentController(
            AuthenticationService authenticationService,
            PermissionPolicy permissionPolicy,
            AssignmentRecommendationService assignmentRecommendationService,
            IssueRepository issueRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.assignmentRecommendationService = Objects.requireNonNull(
                assignmentRecommendationService,
                "assignmentRecommendationService"
        );
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AssignmentOptions startAssignment(long issueId) {
        User user = requireCurrentUser();
        Issue issue = findIssue(issueId);
        permissionPolicy.assertCanAssignIssue(user, issue);
        return assignmentRecommendationService.recommendAssignmentCandidates(issue);
    }

    /*
     * 다른 팀원이 구현해야하는 부분:
     * 실제 assignee/verifier 지정, 재지정 이력 기록, UI 입력 검증은 담당자 배정 UC 담당자가 구현한다.
     */

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue was not found: " + issueId));
    }
}
