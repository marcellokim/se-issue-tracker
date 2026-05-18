package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이슈 생성")
class IssueCreationTest {

    private final User reporter = new User("U-1", "tester1", "Tester One", "hash", Role.TESTER);
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 18, 10, 0);

    @Test
    @DisplayName("이슈는 reporter, 기본 상태, 생성 이력을 가지고 생성된다")
    void createIssueWithReporterDefaultStatusAndCreationHistory() {
        var issue = Issue.create("ISSUE-1", "Login fails", "Cannot log in", null, reporter, now);

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
        var issue = Issue.create("ISSUE-1", "Crash", "App crashes", Priority.CRITICAL, reporter, now);

        assertEquals(Priority.CRITICAL, issue.getPriority());
    }

    @Test
    @DisplayName("이슈 생성 필수 값은 비어 있을 수 없다")
    void rejectInvalidIssueCreationArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Issue.create("", "Title", "Description", null, reporter, now));
        assertThrows(IllegalArgumentException.class,
                () -> Issue.create("ISSUE-1", "", "Description", null, reporter, now));
        assertThrows(IllegalArgumentException.class,
                () -> Issue.create("ISSUE-1", "Title", "", null, reporter, now));
        assertThrows(NullPointerException.class,
                () -> Issue.create("ISSUE-1", "Title", "Description", null, null, now));
        assertThrows(NullPointerException.class,
                () -> Issue.create("ISSUE-1", "Title", "Description", null, reporter, null));
    }
}
