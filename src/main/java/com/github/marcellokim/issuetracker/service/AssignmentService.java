package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;

public final class AssignmentService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PermissionPolicy permissionPolicy;
    private final AssignmentRecommendationService assignmentRecommendationService;
    private final Clock clock;

    public AssignmentService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            AssignmentRecommendationService assignmentRecommendationService,
            Clock clock) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.assignmentRecommendationService = Objects.requireNonNull(
                assignmentRecommendationService, "recommendationService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AssignmentOptionsResult startAssignment(long issueId, String loginId) {
        if (issueId <= 0L) {
            throw new IllegalArgumentException("issueId must be positive");
        }
        Issue issue = findIssue(issueId);
        User actor = findUser(loginId);
        assertCanStartAssignment(actor, issue);
        return assignmentRecommendationService.recommendAssignmentCandidates(issue);
    }

    public AssignmentResult assignIssue(long issueId, String assigneeId, String verifierId, String loginId) {
        if (issueId <= 0L) {
            throw new IllegalArgumentException("issueId must be positive");
        }
        Issue issue = findIssue(issueId);
        User actor = findUser(loginId);
        assertCanManageAssignment(actor, issue);
        User assignee = findUser(assigneeId);
        User verifier = findUser(verifierId);
        requireActiveProjectMember(
                assignee,
                issue.projectId(),
                Role.DEV,
                "Assignee must be an active DEV in the issue project.");
        requireActiveProjectMember(verifier, issue.projectId(), Role.TESTER,
                "Verifier must be an active TESTER in the issue project.");
        if (issue.status() == IssueStatus.NEW) {
            issue.assignFromNew(assignee, verifier, actor, now());
        } else if (issue.status() == IssueStatus.REOPENED) {
            issue.assignReopened(assignee, verifier, actor, now());
        } else {
            throw new IllegalStateException("Issue status does not allow assignment: " + issue.status());
        }
        issueRepository.save(issue);
        return toResult(issue);
    }

    public AssignmentResult reassignIssue(long issueId, String assigneeId, String loginId) {
        if (issueId <= 0L) {
            throw new IllegalArgumentException("issueId must be positive");
        }
        Issue issue = findIssue(issueId);
        User actor = findUser(loginId);
        assertCanManageAssignment(actor, issue);
        User assignee = findUser(assigneeId);
        requireActiveProjectMember(
                assignee,
                issue.projectId(),
                Role.DEV,
                "Assignee must be an active DEV in the issue project.");
        issue.reassignAssignee(assignee, actor, now());
        issueRepository.save(issue);
        return toResult(issue);
    }

    public AssignmentResult changeVerifier(long issueId, String verifierId, String loginId) {
        if (issueId <= 0L) {
            throw new IllegalArgumentException("issueId must be positive");
        }
        Issue issue = findIssue(issueId);
        User actor = findUser(loginId);
        assertCanManageAssignment(actor, issue);
        User verifier = findUser(verifierId);
        requireActiveProjectMember(verifier, issue.projectId(), Role.TESTER,
                "Verifier must be an active TESTER in the issue project.");
        issue.changeVerifier(verifier, actor, now());
        issueRepository.save(issue);
        return toResult(issue);
    }

    private void assertCanStartAssignment(User actor, Issue issue) {
        switch (issue.status()) {
            case NEW, REOPENED, ASSIGNED, FIXED -> {
                assertCanManageAssignment(actor, issue);
            }
            default -> throw new IllegalStateException("Issue status does not allow assignment updates");
        }
    }

    private void assertCanManageAssignment(User actor, Issue issue) {
        permissionPolicy.assertCanAssignIssue(actor, issue);
        requireProjectLead(actor, issue.projectId(), "Only the project PL can assign issue owners.");
    }

    private void requireProjectLead(User actor, long projectId, String message) {
        boolean projectLead = userRepository.findActiveByRole(projectId, Role.PL).stream()
                .anyMatch(user -> user.getLoginId().equals(actor.getLoginId()));
        if (!projectLead) {
            throw new SecurityException(message);
        }
    }

    private void requireActiveProjectMember(User user, long projectId, Role role, String message) {
        boolean memberWithRole = userRepository.findActiveByRole(projectId, role).stream()
                .anyMatch(candidate -> candidate.getLoginId().equals(user.getLoginId()));
        if (!memberWithRole) {
            throw new SecurityException(message);
        }
    }

    private Issue findIssue(long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
    }

    private User findUser(String userId) {
        String requiredUserId = requireText(userId, "currentUserId");
        return userRepository.findById(requiredUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + requiredUserId));
    }

    private LocalDateTime now() {
        return clock.now();
    }

    private static AssignmentResult toResult(Issue issue) {
        return new AssignmentResult(
                issue.id(),
                issue.getIssueId(),
                issue.status(),
                toUserResult(issue.getAssignee()),
                toUserResult(issue.getVerifier()));
    }

    private static UserResult toUserResult(User user) {
        return user == null ? null : UserResult.from(user);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
