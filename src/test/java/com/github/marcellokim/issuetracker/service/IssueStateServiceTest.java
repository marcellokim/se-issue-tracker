package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("상태 변경 서비스")
class IssueStateServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private final User reporter = new User("tester1", "Tester One", "hash", Role.TESTER, true, createdAt(), createdAt());
    private final User assignee = new User("dev1", "Dev One", "hash", Role.DEV, true, createdAt(), createdAt());
    private final User verifier = new User("tester2", "Tester Two", "hash", Role.TESTER, true, createdAt(), createdAt());
    private final User pl = new User("pl1", "PL One", "hash", Role.PL, true, createdAt(), createdAt());
    private final User otherDev = new User("dev2", "Dev Two", "hash", Role.DEV, true, createdAt(), createdAt());

    @Test
    @DisplayName("assignee DEV는 ASSIGNED 이슈를 FIXED로 변경한다")
    void markAssignedIssueFixed() {
        var issue = assignedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", assignee.loginId());

        assertEquals(IssueStatus.FIXED, result.status());
        assertSame(assignee, issue.getFixer());
        assertEquals(1, issue.getComments().size());
        assertEquals("Fix completed", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("verifier TESTER는 FIXED 이슈를 RESOLVED로 변경한다")
    void resolveFixedIssue() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.RESOLVED, "Verified", verifier.loginId());

        assertEquals(IssueStatus.RESOLVED, result.status());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Verified", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("PL은 RESOLVED 이슈를 CLOSED로 변경하고 active assignment를 비운다")
    void closeResolvedIssue() {
        var issue = resolvedIssue();
        var service = service(issue);

        var result = service.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Release completed", pl.loginId());

        assertEquals(IssueStatus.CLOSED, result.status());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Release completed", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.COMMENTED, issue.getHistories().getLast().getAction());
        assertStatusChangedThenCommented(issue);
    }

    @Test
    @DisplayName("comment가 없거나 담당자가 아니면 상태 변경은 실패한다")
    void rejectBlankCommentAndWrongParticipant() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "", assignee.loginId()));
        assertThrows(SecurityException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.FIXED, "Fix completed", otherDev.loginId()));
    }

    @Test
    @DisplayName("상태 변경 comment가 비어 있으면 issue/user 조회 전에 거부한다")
    void rejectBlankCommentBeforeLookup() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(999L, IssueStatus.FIXED, " ", "missing"));
    }

    @Test
    @DisplayName("#20 범위 밖 상태 변경 target은 거부한다")
    void rejectUnsupportedTargetStatus() {
        var issue = resolvedIssue();
        var service = service(issue);

        assertThrows(UnsupportedOperationException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.REOPENED, "Needs more work", pl.loginId()));
    }

    @Test
    @DisplayName("도메인 전이가 실패하면 comment/history 부작용을 남기지 않는다")
    void failedDomainTransitionLeavesNoCommentOrHistorySideEffect() {
        var issue = fixedIssue();
        int commentCount = issue.getComments().size();
        int historyCount = issue.getHistories().size();
        var service = service(issue);

        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(ISSUE_ID, IssueStatus.CLOSED, "Close too early", pl.loginId()));

        assertEquals(commentCount, issue.getComments().size());
        assertEquals(historyCount, issue.getHistories().size());
    }

    @Test
    @DisplayName("target status는 필수 값이다")
    void rejectNullTargetStatus() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(NullPointerException.class,
                () -> service.changeStatus(ISSUE_ID, null, "Fix completed", assignee.loginId()));
    }

    private IssueStateService service(Issue issue) {
        return new IssueStateService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev),
                new PermissionPolicy(),
                new Clock()
        );
    }

    private Issue newIssue() {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(createdAt())
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(createdAt()));
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

    private Issue resolvedIssue() {
        var issue = fixedIssue();
        issue.resolve(verifier, "Verified", createdAt().plusMinutes(30));
        return issue;
    }

    private static void assertStatusChangedThenCommented(Issue issue) {
        var histories = issue.getHistories();
        assertEquals(ActionType.STATUS_CHANGED, histories.get(histories.size() - 2).getAction());
        assertEquals(ActionType.COMMENTED, histories.getLast().getAction());
    }

    private static LocalDateTime createdAt() {
        return LocalDateTime.of(2026, 5, 18, 10, 0);
    }
}
