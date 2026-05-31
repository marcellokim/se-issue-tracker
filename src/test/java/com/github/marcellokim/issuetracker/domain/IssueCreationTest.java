package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue creation")
class IssueCreationTest {

        private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, null,
                        null);
        private final LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);

        @Test
        @DisplayName("new issue starts in NEW state")
        void newIssueStartsInNewState() {
                var issue = Issue.create(Issue.persistedState(
                                1L,
                                "Login fails",
                                "Cannot log in",
                                reporter)
                                .reportedDate(createdAt)
                                .updatedAt(createdAt));

                assertEquals(0L, issue.id());
                assertFalse(issue.getIssueId().isBlank());
                assertEquals("Login fails", issue.getTitle());
                assertEquals("Cannot log in", issue.getDescription());
                assertSame(reporter, issue.getReporter());
                assertEquals(Priority.MAJOR, issue.getPriority());
                assertEquals(IssueStatus.NEW, issue.getStatus());

                var history = issue.getHistories().getFirst();
                assertEquals(ActionType.CREATED, history.getAction());
                assertEquals(IssueStatus.NEW.name(), history.getNewValue());
                assertSame(reporter, history.getChangedBy());
                assertEquals(createdAt, history.getChangedDate());
        }

        @Test
        @DisplayName("priority can be chosen")
        void priorityCanBeChosen() {
                var issue = Issue.create(Issue.persistedState(
                                1L,
                                "Crash",
                                "App crashes",
                                reporter)
                                .priority(Priority.CRITICAL)
                                .reportedDate(createdAt)
                                .updatedAt(createdAt));

                assertEquals(Priority.CRITICAL, issue.getPriority());
        }

        @Test
        @DisplayName("loaded issue has its saved id")
        void loadedIssueHasSavedId() {
                var persisted = Issue.fromPersistence(Issue.persistedState(
                                1L,
                                "Persisted issue",
                                "Loaded from DB.",
                                reporter)
                                .id(10L)
                                .issueId("ISSUE-STABLE-1")
                                .reportedDate(createdAt)
                                .updatedAt(createdAt));

                assertEquals(10L, persisted.id());
                assertEquals("ISSUE-STABLE-1", persisted.getIssueId());
                assertEquals("Persisted issue", persisted.getTitle());
                assertEquals(IssueStatus.NEW, persisted.getStatus());
        }

        @Test
        @DisplayName("saved issue needs its ids")
        void savedIssueNeedsItsIds() {
                assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(
                                Issue.persistedState(1L, "Bug", "desc", reporter)
                                                .id(0L)
                                                .issueId("ISSUE-1")
                                                .reportedDate(createdAt)
                                                .updatedAt(createdAt)));
                assertThrows(IllegalArgumentException.class, () -> Issue.fromPersistence(Issue.persistedState(
                                1L,
                                "Missing issue id",
                                "Persisted state must carry DB issue_id.",
                                reporter)
                                .id(11L)
                                .reportedDate(createdAt)
                                .updatedAt(createdAt)));
        }

        @Test
        @DisplayName("new issue has no database id yet")
        void newIssueHasNoDatabaseIdYet() {
                assertThrows(IllegalArgumentException.class, () -> Issue.create(
                                Issue.persistedState(1L, "Bug", "desc", reporter)
                                                .id(5L)
                                                .reportedDate(createdAt)
                                                .updatedAt(createdAt)));
        }

        @Test
        @DisplayName("issue needs required info")
        void issueNeedsRequiredInfo() {
                assertThrows(IllegalArgumentException.class,
                                () -> IssueFixtures.create("ISSUE-1", "", "Description", null, reporter, createdAt));
                assertThrows(IllegalArgumentException.class,
                                () -> IssueFixtures.create("ISSUE-1", "Title", "", null, reporter, createdAt));
                assertThrows(NullPointerException.class,
                                () -> IssueFixtures.create("ISSUE-1", "Title", "Description", null, null, createdAt));
                assertThrows(NullPointerException.class,
                                () -> IssueFixtures.create("ISSUE-1", "Title", "Description", null, reporter, null));
        }
}
