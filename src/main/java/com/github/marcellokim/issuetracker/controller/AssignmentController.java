package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.service.AssignmentOptions;
import com.github.marcellokim.issuetracker.service.AssignmentResult;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import java.util.Objects;

public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = Objects.requireNonNull(assignmentService, "assignmentService must not be null");
    }

    public AssignmentOptions startAssignment(String issueId, String currentUserId) {
        return assignmentService.startAssignment(issueId, currentUserId);
    }

    public AssignmentResult assignIssue(String issueId, String assigneeId, String verifierId, String currentUserId) {
        return assignmentService.assignIssue(issueId, assigneeId, verifierId, currentUserId);
    }

    public AssignmentResult reassignIssue(String issueId, String assigneeId, String currentUserId) {
        return assignmentService.reassignIssue(issueId, assigneeId, currentUserId);
    }

    public AssignmentResult changeVerifier(String issueId, String verifierId, String currentUserId) {
        return assignmentService.changeVerifier(issueId, verifierId, currentUserId);
    }
}
