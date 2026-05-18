package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

@DisplayName("상태 변경 서비스")
class IssueStateServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-18T02:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final User assignee = new User("U-2", "dev1", "Dev One", "hash", Role.DEV);
    private final User verifier = new User("U-3", "tester2", "Tester Two", "hash", Role.TESTER);
    private final User pl = new User("U-4", "pl1", "PL One", "hash", Role.PL);
    private final User otherDev = new User("U-5", "dev2", "Dev Two", "hash", Role.DEV);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("assignee DEV는 ASSIGNED 이슈를 FIXED로 변경한다")
    void markAssignedIssueFixed() {
        var issue = assignedIssue();
        var service = service(issue);

        var result = service.changeStatus("ISSUE-1", IssueStatus.FIXED, "Fix completed", assignee.getUserId());

        assertEquals(IssueStatus.FIXED, result.status());
        assertSame(assignee, issue.getFixer());
        assertEquals(1, issue.getComments().size());
        assertEquals("Fix completed", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
        assertCommentedThenStatusChanged(issue);
    }

    @Test
    @DisplayName("verifier TESTER는 FIXED 이슈를 RESOLVED로 변경한다")
    void resolveFixedIssue() {
        var issue = fixedIssue();
        var service = service(issue);

        var result = service.changeStatus("ISSUE-1", IssueStatus.RESOLVED, "Verified", verifier.getUserId());

        assertEquals(IssueStatus.RESOLVED, result.status());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Verified", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
        assertCommentedThenStatusChanged(issue);
    }

    @Test
    @DisplayName("PL은 RESOLVED 이슈를 CLOSED로 변경하고 active assignment를 비운다")
    void closeResolvedIssue() {
        var issue = resolvedIssue();
        var service = service(issue);

        var result = service.changeStatus("ISSUE-1", IssueStatus.CLOSED, "Release completed", pl.getUserId());

        assertEquals(IssueStatus.CLOSED, result.status());
        assertNull(issue.getAssignee());
        assertNull(issue.getVerifier());
        assertSame(assignee, issue.getFixer());
        assertSame(verifier, issue.getResolver());
        assertEquals(1, issue.getComments().size());
        assertEquals("Release completed", issue.getComments().getFirst().getContent());
        assertEquals(ActionType.STATUS_CHANGED, issue.getHistories().getLast().getAction());
        assertCommentedThenStatusChanged(issue);
    }

    @Test
    @DisplayName("comment가 없거나 담당자가 아니면 상태 변경은 실패한다")
    void rejectBlankCommentAndWrongParticipant() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus("ISSUE-1", IssueStatus.FIXED, "", assignee.getUserId()));
        assertThrows(SecurityException.class,
                () -> service.changeStatus("ISSUE-1", IssueStatus.FIXED, "Fix completed", otherDev.getUserId()));
    }

    @Test
    @DisplayName("상태 변경 comment가 비어 있으면 issue/user 조회 전에 거부한다")
    void rejectBlankCommentBeforeLookup() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus("UNKNOWN", IssueStatus.FIXED, " ", "UNKNOWN"));
    }

    @Test
    @DisplayName("#20 범위 밖 상태 변경 target은 거부한다")
    void rejectUnsupportedTargetStatus() {
        var issue = resolvedIssue();
        var service = service(issue);

        assertThrows(UnsupportedOperationException.class,
                () -> service.changeStatus("ISSUE-1", IssueStatus.REOPENED, "Needs more work", pl.getUserId()));
    }

    @Test
    @DisplayName("target status는 필수 값이다")
    void rejectNullTargetStatus() {
        var issue = assignedIssue();
        var service = service(issue);

        assertThrows(NullPointerException.class,
                () -> service.changeStatus("ISSUE-1", null, "Fix completed", assignee.getUserId()));
    }

    private IssueStateService service(Issue issue) {
        return new IssueStateService(
                new InMemoryIssueRepository(issue),
                new InMemoryUserRepository(reporter, assignee, verifier, pl, otherDev),
                new PermissionPolicy(),
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

    private Issue resolvedIssue() {
        var issue = fixedIssue();
        issue.resolve(verifier, "Verified", createdAt.plusMinutes(30));
        return issue;
    }

    private static void assertCommentedThenStatusChanged(Issue issue) {
        var histories = issue.getHistories();
        assertEquals(ActionType.COMMENTED, histories.get(histories.size() - 2).getAction());
        assertEquals(ActionType.STATUS_CHANGED, histories.getLast().getAction());
    }
}
