package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue history")
class IssueHistoryTest {

        private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 5, 19, 13, 0);
        private final User pl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, null, null);

        @Test
        @DisplayName("preserves persisted history fields")
        void persistedHistoryKeepsDatabaseFields() {
                IssueHistory history = IssueHistory.fromPersistence(
                                15L,
                                100L,
                                "pl1",
                                ActionType.STATUS_CHANGED,
                                IssueStatus.RESOLVED.name(),
                                IssueStatus.CLOSED.name(),
                                "close after verification",
                                CHANGED_AT);

                assertEquals(15L, history.id());
                assertEquals(100L, history.issueId());
                assertEquals("15", history.getHistoryId());
                assertEquals("pl1", history.changedById());
                assertEquals(ActionType.STATUS_CHANGED, history.actionType());
                assertEquals(ActionType.STATUS_CHANGED, history.getAction());
                assertEquals(IssueStatus.RESOLVED.name(), history.previousValue());
                assertEquals(IssueStatus.RESOLVED.name(), history.getPreviousValue());
                assertEquals(IssueStatus.CLOSED.name(), history.newValue());
                assertEquals(IssueStatus.CLOSED.name(), history.getNewValue());
                assertEquals("close after verification", history.message());
                assertEquals("close after verification", history.getMessage());
                assertEquals(CHANGED_AT, history.changedDate());
                assertEquals(CHANGED_AT, history.getChangedDate());
                assertNull(history.getChangedBy());
        }

        @Test
        @DisplayName("preserves domain-created history fields")
        void domainHistoryKeepsActorAndValues() {
                IssueHistory history = IssueHistory.create(
                                "H-1",
                                ActionType.PRIORITY_CHANGED,
                                Priority.MAJOR.name(),
                                Priority.CRITICAL.name(),
                                "urgent customer issue",
                                pl,
                                CHANGED_AT);

                assertEquals(0L, history.id());
                assertEquals(0L, history.issueId());
                assertEquals("H-1", history.getHistoryId());
                assertEquals("pl1", history.changedById());
                assertEquals(ActionType.PRIORITY_CHANGED, history.actionType());
                assertEquals(Priority.MAJOR.name(), history.previousValue());
                assertEquals(Priority.CRITICAL.name(), history.newValue());
                assertEquals("urgent customer issue", history.message());
                assertSame(pl, history.getChangedBy());
                assertEquals(CHANGED_AT, history.changedDate());
        }

        @Test
        @DisplayName("new history for persistence keeps issue id and transient database id")
        void newForPersistenceKeepsIssueIdAndTransientId() {
                IssueHistory history = IssueHistory.newForPersistence(
                                100L,
                                "pl1",
                                ActionType.STATUS_CHANGED,
                                IssueStatus.ASSIGNED.name(),
                                IssueStatus.DELETED.name(),
                                "delete issue",
                                CHANGED_AT);

                assertEquals(0L, history.id());
                assertEquals(100L, history.issueId());
                assertEquals("pl1", history.changedById());
                assertEquals(ActionType.STATUS_CHANGED, history.actionType());
                assertEquals(IssueStatus.ASSIGNED.name(), history.previousValue());
                assertEquals(IssueStatus.DELETED.name(), history.newValue());
                assertEquals("delete issue", history.message());
                assertNull(history.getChangedBy());
                assertEquals(CHANGED_AT, history.changedDate());
        }

        @Test
        @DisplayName("rejects invalid persisted history arguments")
        void rejectsInvalidPersistedHistoryArguments() {
                assertThrows(IllegalArgumentException.class,
                                () -> IssueHistory.fromPersistence(0L, 100L, "pl1", ActionType.CREATED, null, "NEW",
                                                null, CHANGED_AT));
                assertThrows(IllegalArgumentException.class,
                                () -> IssueHistory.fromPersistence(1L, 0L, "pl1", ActionType.CREATED, null, "NEW",
                                                null, CHANGED_AT));
                assertThrows(IllegalArgumentException.class,
                                () -> IssueHistory.fromPersistence(1L, 100L, " ", ActionType.CREATED, null, "NEW", null,
                                                CHANGED_AT));
                assertThrows(NullPointerException.class,
                                () -> IssueHistory.fromPersistence(1L, 100L, "pl1", null, null, "NEW", null,
                                                CHANGED_AT));
                assertThrows(NullPointerException.class,
                                () -> IssueHistory.fromPersistence(1L, 100L, "pl1", ActionType.CREATED, null, "NEW",
                                                null, null));
        }

        @Test
        @DisplayName("rejects invalid domain-created history arguments")
        void rejectsInvalidDomainHistoryArguments() {
                assertThrows(IllegalArgumentException.class,
                                () -> IssueHistory.create("", ActionType.CREATED, null, "NEW", null, pl, CHANGED_AT));
                assertThrows(NullPointerException.class,
                                () -> IssueHistory.create("H-1", null, null, "NEW", null, pl, CHANGED_AT));
                assertThrows(NullPointerException.class,
                                () -> IssueHistory.create("H-1", ActionType.CREATED, null, "NEW", null, null,
                                                CHANGED_AT));
                assertThrows(NullPointerException.class,
                                () -> IssueHistory.create("H-1", ActionType.CREATED, null, "NEW", null, pl, null));
        }
}
