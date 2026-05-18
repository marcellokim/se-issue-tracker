package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class AssignmentService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final AssignmentRecommendationService recommendationService;
    private final Clock clock;

    public AssignmentService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            AssignmentRecommendationService recommendationService,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy must not be null");
        this.recommendationService = Objects.requireNonNull(recommendationService, "recommendationService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AssignmentOptions startAssignment(String issueId, String currentUserId) {
        var issue = findIssue(issueId);
        var actor = findUser(currentUserId);
        assertCanStartAssignment(actor, issue);
        var developers = userRepository.findActiveDevelopers();
        var testers = userRepository.findActiveTesters();
        return new AssignmentOptions(
                issue.getIssueId(),
                issue.getStatus(),
                issue.getAssignee(),
                issue.getVerifier(),
                developers,
                testers,
                recommendationService.recommendAssignmentCandidates(issue, developers, testers)
        );
    }

    public AssignmentResult assignIssue(String issueId, String assigneeId, String verifierId, String currentUserId) {
        var issue = findIssue(issueId);
        var actor = findUser(currentUserId);
        var assignee = findUser(assigneeId);
        var verifier = findUser(verifierId);
        permissionPolicy.assertCanAssignIssue(actor, issue);
        if (issue.getStatus() == IssueStatus.NEW) {
            issue.assignFromNew(assignee, verifier, actor, now());
        } else if (issue.getStatus() == IssueStatus.REOPENED) {
            issue.assignReopened(assignee, verifier, actor, now());
        } else {
            throw new IllegalStateException("Issue status does not allow assignment: " + issue.getStatus());
        }
        issueRepository.save(issue);
        return toResult(issue);
    }

    public AssignmentResult reassignIssue(String issueId, String assigneeId, String currentUserId) {
        var issue = findIssue(issueId);
        var actor = findUser(currentUserId);
        var assignee = findUser(assigneeId);
        permissionPolicy.assertCanReassignIssue(actor, issue);
        issue.reassignAssignee(assignee, actor, now());
        issueRepository.save(issue);
        return toResult(issue);
    }

    public AssignmentResult changeVerifier(String issueId, String verifierId, String currentUserId) {
        var issue = findIssue(issueId);
        var actor = findUser(currentUserId);
        var verifier = findUser(verifierId);
        permissionPolicy.assertCanChangeVerifier(actor, issue);
        issue.changeVerifier(verifier, actor, now());
        issueRepository.save(issue);
        return toResult(issue);
    }

    private void assertCanStartAssignment(User actor, Issue issue) {
        switch (issue.getStatus()) {
            case NEW, REOPENED -> permissionPolicy.assertCanAssignIssue(actor, issue);
            case ASSIGNED -> permissionPolicy.assertCanReassignIssue(actor, issue);
            case FIXED -> permissionPolicy.assertCanChangeVerifier(actor, issue);
            default -> throw new IllegalStateException("Issue status does not allow assignment updates");
        }
    }

    private Issue findIssue(String issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static AssignmentResult toResult(Issue issue) {
        return new AssignmentResult(issue.getIssueId(), issue.getStatus(), issue.getAssignee(), issue.getVerifier());
    }
}
