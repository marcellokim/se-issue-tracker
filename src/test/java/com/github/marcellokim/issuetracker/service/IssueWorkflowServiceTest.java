package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("?댁뒋 諛곗젙怨??곹깭 蹂寃??듯빀 ?먮쫫")
class IssueWorkflowServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 18, 10, 0);
    private final User reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, CREATED_AT, CREATED_AT);
    private final User assignee = User.create("dev1", "Dev One", "hash", Role.DEV, true, CREATED_AT, CREATED_AT);
    private final User verifier = User.create("tester2", "Tester Two", "hash", Role.TESTER, true, CREATED_AT, CREATED_AT);
    private final User pl = User.create("pl1", "PL One", "hash", Role.PL, true, CREATED_AT, CREATED_AT);

    @Test
    @DisplayName("tester -> PL -> dev -> tester -> PL 硫붿씤 ?곕え ?먮쫫???꾨즺?쒕떎")
    void completeMainDemoWorkflow() {
        var issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(CREATED_AT));
        var issueRepository = new InMemoryIssueRepository(issue);
        var userRepository = new InMemoryUserRepository(reporter, assignee, verifier, pl);
        var policy = new PermissionPolicy();
        var assignmentService = new AssignmentService(
                issueRepository,
                userRepository,
                policy,
                new AssignmentRecommendationService(new EmptyAssignmentRecommendationRepository()),
                new Clock()
        );
        var stateService = new IssueStateService(issueRepository, userRepository, policy, new Clock());

        assignmentService.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(), pl.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", assignee.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.getLoginId());
        stateService.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Release completed", pl.getLoginId());

        var completedIssue = issueRepository.findById(ISSUE_ID).orElseThrow();
        assertEquals(IssueStatus.CLOSED, completedIssue.getStatus());
        assertNull(completedIssue.getAssignee());
        assertNull(completedIssue.getVerifier());
        assertSame(assignee, completedIssue.getFixer());
        assertSame(verifier, completedIssue.getResolver());
        assertEquals(3, completedIssue.getComments().size());
        assertEquals(4, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.STATUS_CHANGED)
                .count());
        assertEquals(1, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.ASSIGNMENT_CHANGED)
                .count());
        assertEquals(3, completedIssue.getHistories().stream()
                .filter(history -> history.getAction() == ActionType.COMMENTED)
                .count());
    }

    private static final class EmptyAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        @Override
        public List<AssignmentCandidate> findDevAssigneeCandidates(long projectId) {
            return List.of();
        }

        @Override
        public List<AssignmentCandidate> findTesterVerifierCandidates(long projectId) {
            return List.of();
        }
    }
}
