package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 생성")
class IssueCreationTest {

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null, null);
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("이슈는 reporter, 기본 상태, 생성 이력을 가지고 생성된다")
    void createIssueWithReporterDefaultStatusAndCreationHistory() {
        var issue = IssueTestFactory.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, now);

        assertEquals("ISSUE-1", issue.getIssueId());
        assertEquals("Login fails", issue.getTitle());
        assertEquals("Cannot log in", issue.getDescription());
        assertSame(reporter, issue.getReporter());
        assertEquals(now, issue.getReportedDate());
        assertEquals(Priority.MAJOR, issue.getPriority());
        assertEquals(IssueStatus.NEW, issue.getStatus());
        assertEquals(1, issue.getHistories().size());

        var history = issue.getHistories().getFirst();
        assertEquals(ActionType.CREATED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals(IssueStatus.NEW.name(), history.getNewValue());
        assertSame(reporter, history.getChangedBy());
        assertEquals(now, history.getChangedDate());
    }

    @Test
    @DisplayName("명시한 우선순위가 있으면 해당 값으로 생성된다")
    void createIssueWithExplicitPriority() {
        var issue = IssueTestFactory.create("ISSUE-1", "Crash", "App crashes", Priority.CRITICAL, reporter, now);

        assertEquals(Priority.CRITICAL, issue.getPriority());
    }

    @Test
    @DisplayName("Persisted issue uses DB issue_id as the stable domain issueId")
    void persistedIssueRequiresStableIssueId() {
        var persisted = Issue.fromPersistence(Issue.persistedState(
                1L,
                "Persisted issue",
                "Loaded from DB.",
                reporter)
                .id(10L)
                .issueId("ISSUE-STABLE-1")
                .reportedDate(now)
                .updatedAt(now));

        assertEquals(10L, persisted.id());
        assertEquals("ISSUE-STABLE-1", persisted.getIssueId());
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(Issue.persistedState(
                1L,
                "Missing issue id",
                "Persisted state must carry DB issue_id.",
                reporter)
                .id(11L)
                .reportedDate(now)
                .updatedAt(now)));
    }

    @Test
    @DisplayName("New issue create may generate issueId before save")
    void createIssueCanGenerateIssueIdBeforeSave() {
        var issue = Issue.create(Issue.persistedState(
                1L,
                "New repository issue",
                "Before DB identity is assigned.",
                reporter)
                .reportedDate(now)
                .updatedAt(now));

        assertEquals(0L, issue.id());
        assertFalse(issue.getIssueId().isBlank());
        assertEquals(1, issue.getHistories().size());
        var history = issue.getHistories().getFirst();
        assertEquals(ActionType.CREATED, history.getAction());
        assertNull(history.getPreviousValue());
        assertEquals(IssueStatus.NEW.name(), history.getNewValue());
        assertEquals("Issue created", history.getMessage());
        assertSame(reporter, history.getChangedBy());
        assertEquals(now, history.getChangedDate());
    }

    @Test
    @DisplayName("create with explicit issueId preserves it")
    void createWithExplicitIssueId() {
        var issue = Issue.create(Issue.persistedState(
                1L, "Bug", "desc", reporter)
                .issueId("CUSTOM-ID")
                .reportedDate(now)
                .updatedAt(now));

        assertEquals("CUSTOM-ID", issue.getIssueId());
    }

    @Test
    @DisplayName("fromPersistence rejects non-positive id")
    void fromPersistenceRejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(0L)
                        .issueId("ISSUE-1")
                        .reportedDate(now)
                        .updatedAt(now)));
        assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(-1L)
                        .issueId("ISSUE-1")
                        .reportedDate(now)
                        .updatedAt(now)));
    }

    @Test
    @DisplayName("create rejects non-zero id")
    void createRejectsNonZeroId() {
        assertThrows(IllegalArgumentException.class, () -> Issue.create(
                Issue.persistedState(1L, "Bug", "desc", reporter)
                        .id(5L)
                        .reportedDate(now)
                        .updatedAt(now)));
    }

    @Test
    @DisplayName("persistence getters return expected values")
    void persistenceGettersReturnExpectedValues() {
        var issue = Issue.fromPersistence(Issue.persistedState(
                1L, "Bug", "desc", reporter)
                .id(10L)
                .issueId("ISSUE-1")
                .reportedDate(now)
                .updatedAt(now));

        assertEquals("Bug", issue.title());
        assertEquals("desc", issue.description());
        assertEquals(now, issue.reportedDate());
        assertEquals(Priority.MAJOR, issue.priority());
        assertEquals(IssueStatus.NEW, issue.status());
        assertEquals(reporter.getLoginId(), issue.reporterId());
        assertNull(issue.assigneeId());
        assertNull(issue.verifierId());
        assertNull(issue.fixerId());
        assertNull(issue.resolverId());
        assertEquals(now, issue.updatedAt());
    }

    @Test
    @DisplayName("이슈 생성 필수 값은 비어 있을 수 없다")
    void rejectInvalidIssueCreationArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> IssueTestFactory.create("", "Title", "Description", null, reporter, now));
        assertThrows(IllegalArgumentException.class,
                () -> IssueTestFactory.create("ISSUE-1", "", "Description", null, reporter, now));
        assertThrows(IllegalArgumentException.class,
                () -> IssueTestFactory.create("ISSUE-1", "Title", "", null, reporter, now));
        assertThrows(NullPointerException.class,
                () -> IssueTestFactory.create("ISSUE-1", "Title", "Description", null, null, now));
        assertThrows(NullPointerException.class,
                () -> IssueTestFactory.create("ISSUE-1", "Title", "Description", null, reporter, null));
    }
}
