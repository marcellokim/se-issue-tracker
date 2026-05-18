package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 배정과 상태 변경 통합 흐름")
class IssueWorkflowServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-18T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User assignee = new User("U-2", "dev1", "Dev One", "hash", Role.DEV);
    private final User verifier = new User("U-3", "tester2", "Tester Two", "hash", Role.TESTER);
    private final User pl = new User("U-4", "pl1", "PL One", "hash", Role.PL);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("tester -> PL -> dev -> tester -> PL 메인 데모 흐름이 완료된다")
    void completeMainDemoWorkflow() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
        var issueRepository = new InMemoryIssueRepository(issue);
        var userRepository = new InMemoryUserRepository(reporter, assignee, verifier, pl);
        var policy = new PermissionPolicy();
        var assignmentService = new AssignmentService(
                issueRepository,
                userRepository,
                policy,
                AssignmentRecommendationService.noRecommendations(),
                FIXED_CLOCK
        );
        var stateService = new IssueStateService(issueRepository, userRepository, policy, FIXED_CLOCK);

        assignmentService.assignIssue("ISSUE-1", assignee.getUserId(), verifier.getUserId(), pl.getUserId());
        stateService.changeStatus("ISSUE-1", IssueStatus.FIXED, "Fix completed", assignee.getUserId());
        stateService.changeStatus("ISSUE-1", IssueStatus.RESOLVED, "Verified", verifier.getUserId());
        stateService.changeStatus("ISSUE-1", IssueStatus.CLOSED, "Release completed", pl.getUserId());

        var completedIssue = issueRepository.findById("ISSUE-1").orElseThrow();
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
}
