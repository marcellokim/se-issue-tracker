package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AssignmentResult;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import java.util.Objects;

public final class AssignmentController {

    private final AuthenticationService authenticationService;
    private final AssignmentService assignmentService;

    public AssignmentController(
            AuthenticationService authenticationService,
            AssignmentService assignmentService
    ) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService");
    }

    public AssignmentOptions startAssignment(long issueId) {
        User user = requireCurrentUser();
        return assignmentService.startAssignment(issueId, user.getLoginId());
    }

    public AssignmentResult assignIssue(long issueId, String assigneeId, String verifierId) {
        User user = requireCurrentUser();
        return assignmentService.assignIssue(issueId, assigneeId, verifierId, user.getLoginId());
    }

    public AssignmentResult reassignIssue(long issueId, String assigneeId) {
        User user = requireCurrentUser();
        return assignmentService.reassignIssue(issueId, assigneeId, user.getLoginId());
    }

    public AssignmentResult changeVerifier(long issueId, String verifierId) {
        User user = requireCurrentUser();
        return assignmentService.changeVerifier(issueId, verifierId, user.getLoginId());
    }

    private User requireCurrentUser() {
        return authenticationService.currentUser()
                .orElseThrow(() -> new SecurityException("Login is required."));
    }
}
