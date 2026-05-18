package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

@DisplayName("배정 서비스")
class AssignmentServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-18T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User assignee = new User("U-2", "dev1", "Dev One", "hash", Role.DEV);
    private final User verifier = new User("U-3", "tester2", "Tester Two", "hash", Role.TESTER);
    private final User pl = new User("U-4", "pl1", "PL One", "hash", Role.PL);
    private final User anotherAssignee = new User("U-5", "dev2", "Dev Two", "hash", Role.DEV);
    private final User anotherVerifier = new User("U-6", "tester3", "Tester Three", "hash", Role.TESTER);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("배정 시작은 dev/tester 목록과 추천 후보 구조를 반환한다")
    void startAssignmentReturnsOptions() {
        var issue = newIssue();
        var service = service(issue);

        var options = service.startAssignment("ISSUE-1", pl.getUserId());

        assertEquals(2, options.developers().size());
        assertEquals(3, options.testers().size());
        assertEquals(IssueStatus.NEW, options.issueStatus());
        assertEquals(0, options.candidates().assigneeCandidates().size());
        assertEquals(0, options.candidates().verifierCandidates().size());
    }

    @Test
    @DisplayName("NEW 이슈를 assignee/verifier와 함께 ASSIGNED로 배정한다")
    void assignNewIssue() {
        var issue = newIssue();
        var service = service(issue);

        var result = service.assignIssue("ISSUE-1", assignee.getUserId(), verifier.getUserId(), pl.getUserId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("REOPENED 이슈도 assignee/verifier와 함께 ASSIGNED로 재배정한다")
    void assignReopenedIssue() {
        var issue = reopenedIssue();
        var issueRepository = new InMemoryIssueRepository(issue);
        var service = service(issueRepository);

        var result = service.assignIssue("ISSUE-1", anotherAssignee.getUserId(), anotherVerifier.getUserId(), pl.getUserId());
        var savedIssue = issueRepository.findById("ISSUE-1").orElseThrow();

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(anotherAssignee, savedIssue.getAssignee());
        assertSame(anotherVerifier, savedIssue.getVerifier());
        assertSame(assignee, savedIssue.getFixer());
        assertSame(verifier, savedIssue.getResolver());

        var assignmentHistory = savedIssue.getHistories().get(savedIssue.getHistories().size() - 2);
        assertEquals(ActionType.ASSIGNMENT_CHANGED, assignmentHistory.getAction());
        assertEquals(anotherAssignee.getUserId() + "/" + anotherVerifier.getUserId(), assignmentHistory.getNewValue());

        var statusHistory = savedIssue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, statusHistory.getAction());
        assertEquals(IssueStatus.REOPENED.name(), statusHistory.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
    }

    @Test
    @DisplayName("ASSIGNED 이슈의 assignee만 변경한다")
    void reassignAssignedIssue() {
        var issue = assignedIssue();
        var service = service(issue);

        var result = service.reassignIssue("ISSUE-1", anotherAssignee.getUserId(), pl.getUserId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(anotherAssignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("FIXED 이슈의 verifier만 변경한다")
    void changeFixedIssueVerifier() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeVerifier("ISSUE-1", anotherVerifier.getUserId(), pl.getUserId());

        assertEquals(IssueStatus.FIXED, result.status());
        assertSame(anotherVerifier, issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("PL이 아니면 배정 흐름을 실행할 수 없다")
    void rejectNonPlAssignment() {
        var service = service(newIssue());

        assertThrows(SecurityException.class,
                () -> service.assignIssue("ISSUE-1", assignee.getUserId(), verifier.getUserId(), assignee.getUserId()));
    }

    private AssignmentService service(Issue issue) {
        return service(new InMemoryIssueRepository(issue));
    }

    private AssignmentService service(InMemoryIssueRepository issueRepository) {
        return new AssignmentService(
                issueRepository,
                new InMemoryUserRepository(reporter, assignee, verifier, pl, anotherAssignee, anotherVerifier),
                new PermissionPolicy(),
                AssignmentRecommendationService.noRecommendations(),
                FIXED_CLOCK
        );
    }

    private Issue newIssue() {
        return Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, createdAt);
    }

    private Issue assignedIssue() {
        var issue = newIssue();
        issue.assignFromNew(assignee, verifier, pl, createdAt.plusMinutes(10));
        return issue;
    }

    private Issue fixedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt.plusMinutes(20));
        return issue;
    }

    private Issue reopenedIssue() {
        var issue = fixedIssue();
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        issue.reopen(pl, "Needs more work", createdAt.plusMinutes(40));
        return issue;
    }

}
