package com.github.marcellokim.issuetracker.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;

@DisplayName("Issue state service")
class IssueStateServiceTest {

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
    private final User otherDev = User.fromPersistence("dev2", "Dev Two", "hash", Role.DEV, true, createdAt(), createdAt());

    @Test
    @DisplayName("assignee marks assigned issue fixed")
    void markAssignedIssueFixed() {
        var issue = assignedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", assignee.getLoginId());

        assertEquals(IssueStatus.FIXED, result.status());
        assertSame(assignee, issue.getFixer());
        assertEquals(1, issue.getComments().size());
        assertEquals("Fix completed", issue.getComments().getFirst().getContent());
        assertEquals(CommentPurpose.STATUS_CHANGE, issue.getComments().getFirst().getPurpose());
        org.junit.jupiter.api.Assertions.assertTrue(issue.getComments().getFirst().getCommentId().startsWith("COMMENT-"));
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("verifier resolves fixed issue")
    void resolveFixedIssue() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.getLoginId());

        assertEquals(IssueStatus.RESOLVED, result.status());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Verified", issue.getComments().getFirst().getContent());
        assertEquals(CommentPurpose.STATUS_CHANGE, issue.getComments().getFirst().getPurpose());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("verifier rejects fixed issue back to assigned")
    void rejectFixedIssueBackToAssigned() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                verifier.getLoginId());

        assertEquals(IssueStatus.ASSIGNED, result.status());
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        assertSame(assignee, result.assignee());
        assertSame(verifier, result.verifier());
        assertSame(assignee, result.fixer());
        assertNull(result.resolver());
        assertSame(assignee, issue.getAssignee());
        assertSame(verifier, issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertNull(issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Needs more work", issue.getComments().getFirst().getContent());
        assertEquals(CommentPurpose.STATUS_CHANGE, issue.getComments().getFirst().getPurpose());

        var histories = issue.getHistories();
        var statusHistory = histories.get(histories.size() - 2);
        assertEquals(ActionType.STATUS_CHANGED, statusHistory.getAction());
        assertEquals(IssueStatus.FIXED.name(), statusHistory.getPreviousValue());
        assertEquals(IssueStatus.ASSIGNED.name(), statusHistory.getNewValue());
        assertEquals("Needs more work", statusHistory.getMessage());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("PL closes resolved issue and clears active assignment")
    void closeResolvedIssue() {
        var issue = resolvedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Release completed", pl.getLoginId());

        assertEquals(IssueStatus.CLOSED, result.status());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Release completed", issue.getComments().getFirst().getContent());
        assertEquals(CommentPurpose.STATUS_CHANGE, issue.getComments().getFirst().getPurpose());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("PL must belong to the issue project to close an issue")
    void rejectCloseByPlFromOtherProject() {
        var issue = resolvedIssue();
        var users = new InMemoryUserRepository(reporter, assignee, verifier, pl, otherProjectPl, otherDev)
                .withProjectMembers(PROJECT_ID, pl.getLoginId());
        var service = service(new FakeIssueDependencyRepository(), users, issue);

        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Release completed",
                        otherProjectPl.getLoginId()));
    }

    @Test
    @DisplayName("only current verifier can reject a fixed issue")
    void rejectFixRequiresCurrentVerifier() {
        var issue = fixedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                        otherDev.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("reject fix requires verifier to belong to the issue project")
    void rejectFixRequiresVerifierProjectMembership() {
        var issue = fixedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var users = new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev)
                .withProjectMembers(PROJECT_ID, reporter.getLoginId(), assignee.getLoginId(), pl.getLoginId());
        var service = service(new FakeIssueDependencyRepository(), users, issue);

        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                        verifier.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("reject fix requires fixed issue status")
    void rejectFixRequiresFixedIssueStatus() {
        var issue = assignedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.ASSIGNED, "Needs more work",
                        pl.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("blank comment or wrong actor fails status change")
    void rejectBlankCommentAndWrongParticipant() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "", assignee.getLoginId()));
        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", otherDev.getLoginId()));
    }

    @Test
    @DisplayName("blank status change comment is rejected before lookup")
    void rejectBlankCommentBeforeLookup() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(999L, IssueStatus.FIXED, " ", "missing"));
    }

    @Test
    @DisplayName("reopened status change target remains a feature gap")
    void rejectUnsupportedReopenedTargetStatus() {
        var resolved = resolvedIssue();
        var resolvedService = service(resolved);

        assertThrows(UnsupportedOperationException.class,
                () -> resolvedService.changeStatus(ISSUE_ID, IssueStatus.REOPENED, "Needs more work", pl.getLoginId()));
    }

    @Test
    @DisplayName("failed domain transition leaves no comment or history side effects")
    void failedDomainTransitionLeavesNoCommentOrHistorySideEffect() {
        var issue = fixedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Close too early", pl.getLoginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("target status is required")
    void rejectNullTargetStatus() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(NullPointerException.class,
                () -> service.changeStatus(ISSUE_ID, null, "Fix completed", assignee.getLoginId()));
    }

    @Test
    @DisplayName("blocking issue가 미해결이면 resolve할 수 없다")
    void rejectResolveWhenBlockingIssueUnresolved() {
        var blockedIssue = fixedIssue();
        var blockingIssue = newIssue(2L, "ISSUE-2");
        var depRepo = new FakeIssueDependencyRepository();
        depRepo.addFixture(IssueDependency.fromPersistence(1L, blockingIssue.id(), blockedIssue.id(), createdAt()));
        var service = service(depRepo, blockedIssue, blockingIssue);

        var exception = assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.getLoginId()));
        assertEquals("Cannot resolve: blocking issue ISSUE-2 is still NEW", exception.getMessage());
    }

    @Test
    @DisplayName("여러 blocking issue 중 하나라도 미해결이면 resolve할 수 없다")
    void rejectResolveWhenAnyBlockingIssueUnresolved() {
        var blockedIssue = fixedIssue();
        var resolvedBlocking = resolvedIssue(2L, "ISSUE-2");
        var unresolvedBlocking = newIssue(3L, "ISSUE-3");
        var depRepo = new FakeIssueDependencyRepository();
        depRepo.addFixture(IssueDependency.fromPersistence(1L, resolvedBlocking.id(), blockedIssue.id(), createdAt()));
        depRepo.addFixture(IssueDependency.fromPersistence(2L, unresolvedBlocking.id(), blockedIssue.id(), createdAt()));
        var service = service(depRepo, blockedIssue, resolvedBlocking, unresolvedBlocking);

        var exception = assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.getLoginId()));
        assertEquals("Cannot resolve: blocking issue ISSUE-3 is still NEW", exception.getMessage());
    }

    private IssueStateService service(Issue issue) {
        return service(new FakeIssueDependencyRepository(), issue);
    }

    private IssueStateService service(FakeIssueDependencyRepository depRepo, Issue... issues) {
        return service(depRepo, new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev), issues);
    }

    private IssueStateService service(FakeIssueDependencyRepository depRepo, InMemoryUserRepository users, Issue... issues) {
        return new IssueStateService(
                new InMemoryIssueRepository(issues),
                depRepo,
                users,
                new PermissionPolicy(),
                new Clock()
        );
    }

    private Issue newIssue() {
        return newIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue newIssue(long id, String issueId) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(id)
                .issueId(issueId)
                .reportedDate(createdAt())
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(createdAt()));
    }

    private Issue assignedIssue() {
        return assignedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue assignedIssue(long id, String issueId) {
        var issue = newIssue(id, issueId);
        issue.assignFromNew(assignee, verifier, pl, createdAt().plusMinutes(10));
        return issue;
    }

    private Issue fixedIssue() {
        return fixedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue fixedIssue(long id, String issueId) {
        var issue = assignedIssue(id, issueId);
        issue.markFixed(assignee, "Fix completed", createdAt().plusMinutes(20));
        return issue;
    }

    private Issue resolvedIssue() {
        return resolvedIssue(ISSUE_ID, "ISSUE-1");
    }

    private Issue resolvedIssue(long id, String issueId) {
        var issue = fixedIssue(id, issueId);
        issue.resolve(verifier, "Verified", createdAt().plusMinutes(30));
        return issue;
    }

    private static void assertStatusChangedThenCommented(Issue issue) {
        var histories = issue.getHistories();
        var latestCommentContent = issue.getComments().getLast().getContent();
        assertEquals(ActionType.STATUS_CHANGED, histories.get(histories.size() - 2).getAction());
        assertEquals(ActionType.COMMENTED, histories.getLast().getAction());
        assertNull(histories.getLast().getPreviousValue());
        assertEquals(latestCommentContent, histories.getLast().getNewValue());
        assertEquals(latestCommentContent, histories.getLast().getMessage());
    }

    private static LocalDateTime createdAt() {
        return LocalDateTime.of(2026, 5, 18, 10, 0);
    }
}
