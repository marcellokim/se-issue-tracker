package com.github.marcellokim.issuetracker.service;

import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
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
    private final AssignmentRecommendationService recommendationService;
    private final Clock clock;

    public AssignmentService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            PermissionPolicy permissionPolicy,
            AssignmentRecommendationService recommendationService,
            Clock clock
    ) {
        this.issueRepository = Objects.requireNonNull(issueRepository, "issueRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
        this.recommendationService = Objects.requireNonNull(recommendationService, "recommendationService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AssignmentOptions startAssignment(long issueId, String currentUserId) {
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
        assertCanStartAssignment(actor, issue);
        return recommendationService.recommendAssignmentCandidates(issue);
    }

    public AssignmentResult assignIssue(long issueId, String assigneeId, String verifierId, String currentUserId) {
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
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

    public AssignmentResult reassignIssue(long issueId, String assigneeId, String currentUserId) {
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
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

    public AssignmentResult changeVerifier(long issueId, String verifierId, String currentUserId) {
        Issue issue = findIssue(issueId);
        User actor = findUser(currentUserId);
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
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private LocalDateTime now() {
        return clock.now();
    }

    private static AssignmentResult toResult(Issue issue) {
        return new AssignmentResult(
                issue.id(),
                issue.getIssueId(),
                issue.status(),
                issue.getAssignee(),
                issue.getVerifier());
    }
}
