package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Assignment service")
class AssignmentServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, createdAt(),
            createdAt());
    private final User assignee = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, createdAt(), createdAt());
    private final User verifier = User.fromPersistence("tester2", "Tester Two", "hash", Role.TESTER, true, createdAt(),
            createdAt());
    private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, createdAt(), createdAt());
    private final User otherProjectPl = User.fromPersistence("pl2", "PL Two", "hash", Role.PL, true, createdAt(),
            createdAt());
    private final User anotherAssignee = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, createdAt(),
            createdAt());
    private final User anotherVerifier = User.fromPersistence("tester3", "Tester Three", "hash", Role.TESTER, true,
            createdAt(), createdAt());

    @Test
    @DisplayName("start assignment returns status-aware recommendation options")
    void startAssignmentReturnsOptions() {
        var issue = newIssue();
        var service = service(issue);

        var options = service.startAssignment(ISSUE_ID, pl.getLoginId());

        assertEquals(2, options.allDevAssignees().size());
        assertEquals(2, options.allTesterVerifiers().size());
    }

    @Test
    @DisplayName("assigns NEW issue with assignee and verifier")
    void assignNewIssue() {
        var issue = newIssue();
        var service = service(issue);

        var result = service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(), pl.getLoginId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("assigns reopened issue with new assignee and verifier")
    void assignReopenedIssue() {
        var issue = reopenedIssue();
        var issueRepository = new InMemoryIssueRepository(issue);
        var service = service(issueRepository);

        var result = service.assignIssue(ISSUE_ID, anotherAssignee.getLoginId(), anotherVerifier.getLoginId(),
                pl.getLoginId());
        var savedIssue = issueRepository.findById(ISSUE_ID).orElseThrow();

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(anotherAssignee, savedIssue.getAssignee());
        assertSame(anotherVerifier, savedIssue.getVerifier());
        assertSame(assignee, savedIssue.getFixer());
        assertSame(verifier, savedIssue.getResolver());

        var assignmentHistory = savedIssue.getHistories().get(savedIssue.getHistories().size() - 2);
        assertEquals(ActionType.ASSIGNMENT_CHANGED, assignmentHistory.getAction());
        assertEquals(anotherAssignee.getLoginId() + "/" + anotherVerifier.getLoginId(), assignmentHistory.getNewValue());

        var statusHistory = savedIssue.getHistories().getLast();
        assertEquals(ActionType.STATUS_CHANGED, statusHistory.getAction());
        assertEquals(IssueStatus.REOPENED.name(), statusHistory.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
    }

    @Test
    @DisplayName("reassigns assignee for assigned issue")
    void reassignAssignedIssue() {
        var issue = assignedIssue();
        var service = service(issue);

        var result = service.reassignIssue(ISSUE_ID, anotherAssignee.getLoginId(), pl.getLoginId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertSame(anotherAssignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("changes verifier for fixed issue")
    void changeFixedIssueVerifier() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeVerifier(ISSUE_ID, anotherVerifier.getLoginId(), pl.getLoginId());

        assertEquals(IssueStatus.FIXED, result.status());
        assertSame(anotherVerifier, issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertEquals(ActionType.ASSIGNMENT_CHANGED, issue.getHistories().getLast().getAction());
    }

    @Test
    @DisplayName("non-PL users cannot assign issues")
    void rejectNonPlAssignment() {
        var service = service(newIssue());

        assertThrows(SecurityException.class,
                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                        assignee.getLoginId()));
    }

    @Test
    @DisplayName("PL must belong to the issue project to assign owners")
    void rejectPlFromOtherProjectAssignment() {
        var users = new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                otherProjectPl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID, pl.getLoginId());
        var service = service(new InMemoryIssueRepository(newIssue()), users);

        assertThrows(SecurityException.class,
                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(),
                        otherProjectPl.getLoginId()));
    }

    @Test
    @DisplayName("assignee must be an active DEV in the issue project")
    void rejectAssigneeOutsideIssueProject() {
        var users = new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID, pl.getLoginId(), verifier.getLoginId());
        var service = service(new InMemoryIssueRepository(newIssue()), users);

        assertThrows(SecurityException.class,
                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(), pl.getLoginId()));
    }

    @Test
    @DisplayName("verifier must be an active TESTER in the issue project")
    void rejectVerifierOutsideIssueProject() {
        var users = new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId());
        var service = service(new InMemoryIssueRepository(newIssue()), users);

        assertThrows(SecurityException.class,
                () -> service.assignIssue(ISSUE_ID, assignee.getLoginId(), verifier.getLoginId(), pl.getLoginId()));
    }

    @Test
    @DisplayName("reassignment target must be an active DEV in the issue project")
    void rejectReassignmentToAssigneeOutsideIssueProject() {
        var users = new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId(), verifier.getLoginId());
        var service = service(new InMemoryIssueRepository(assignedIssue()), users);

        assertThrows(SecurityException.class,
                () -> service.reassignIssue(ISSUE_ID, anotherAssignee.getLoginId(), pl.getLoginId()));
    }

    @Test
    @DisplayName("new verifier must be an active TESTER in the issue project")
    void rejectVerifierChangeToTesterOutsideIssueProject() {
        var users = new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID, pl.getLoginId(), assignee.getLoginId(), verifier.getLoginId());
        var service = service(new InMemoryIssueRepository(fixedIssue()), users);

        assertThrows(SecurityException.class,
                () -> service.changeVerifier(ISSUE_ID, anotherVerifier.getLoginId(), pl.getLoginId()));
    }

    private AssignmentService service(Issue issue) {
        return service(new InMemoryIssueRepository(issue));
    }

    private AssignmentService service(InMemoryIssueRepository issueRepository) {
        return service(issueRepository, projectMemberUsers());
    }

    private AssignmentService service(InMemoryIssueRepository issueRepository, InMemoryUserRepository userRepository) {
        return new AssignmentService(
                issueRepository,
                userRepository,
                new PermissionPolicy(),
                new AssignmentRecommendationService(issueRepository, userRepository, new KNNAssignmentRecommendation()),
                java.time.LocalDateTime::now
        );
    }

    private InMemoryUserRepository projectMemberUsers() {
        return new InMemoryUserRepository(
                reporter,
                assignee,
                verifier,
                pl,
                otherProjectPl,
                anotherAssignee,
                anotherVerifier)
                .withProjectMembers(PROJECT_ID,
                        pl.getLoginId(),
                        assignee.getLoginId(),
                        verifier.getLoginId(),
                        anotherAssignee.getLoginId(),
                        anotherVerifier.getLoginId());
    }

    private Issue newIssue() {
        return issue(IssueStatus.NEW);
    }

    private Issue assignedIssue() {
        var issue = newIssue();
        issue.assignFromNew(assignee, verifier, pl, createdAt().plusMinutes(10));
        return issue;
    }

    private Issue fixedIssue() {
        var issue = assignedIssue();
        issue.markFixed(assignee, "Fix completed", createdAt().plusMinutes(20));
        return issue;
    }

    private Issue reopenedIssue() {
        var issue = fixedIssue();
        issue.resolve(verifier, "Verified", createdAt().plusMinutes(30));
        issue.reopen(pl, "Needs more work", createdAt().plusMinutes(40));
        return issue;
    }

    private Issue issue(IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(createdAt())
                .priority(Priority.MAJOR)
                .status(status)
                .updatedAt(createdAt()));
    }

    private static LocalDateTime createdAt() {
        return LocalDateTime.of(2026, 5, 18, 10, 0);
    }
}
