package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.DeletedIssueRepository;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Deleted issue service")
class DeletedIssueServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

    private final User reporter = User.fromPersistence("tester1", "Tester One", "hash", Role.TESTER, true, NOW, NOW);
    private final User projectPl = User.fromPersistence("pl1", "PL One", "hash", Role.PL, true, NOW, NOW);
    private final User otherProjectPl = User.fromPersistence("pl2", "PL Two", "hash", Role.PL, true, NOW, NOW);

    @Test
    @DisplayName("PL opens the deleted issue list")
    void plOpensDeletedList() {
        var deletedIssues = new FakeDeletedIssueRepository(deletedIssue());
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(deletedIssue()),
                deletedIssues,
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        var result = service.viewDeletedIssues(PROJECT_ID, projectPl);

        assertEquals(1, result.size());
        assertEquals(IssueStatus.DELETED, result.get(0).status());
    }

    @Test
    @DisplayName("other project PL is turned away")
    void otherPlIsTurnedAway() {
        var users = new InMemoryUserRepository(reporter, projectPl, otherProjectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(deletedIssue()),
                new FakeDeletedIssueRepository(),
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        assertThrows(SecurityException.class,
                () -> service.viewDeletedIssues(PROJECT_ID, otherProjectPl));
    }

    @Test
    @DisplayName("soft delete keeps the reason")
    void softDeleteKeepsReason() {
        var deletedIssues = new FakeDeletedIssueRepository();
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(issueWithStatus(IssueStatus.NEW)),
                deletedIssues,
                users,
                new PermissionPolicy(),
                () -> NOW);

        var result = service.deleteIssue(ISSUE_ID, "not needed anymore", projectPl);

        assertEquals(IssueStatus.DELETED, result.status());
        assertEquals("not needed anymore", deletedIssues.lastMessage);
        assertEquals(PROJECT_ID, deletedIssues.lastOverflowProjectId);
    }

    @Test
    @DisplayName("only new or closed issues can be deleted")
    void onlyNewOrClosedCanBeDeleted() {
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(issueWithStatus(IssueStatus.ASSIGNED)),
                new FakeDeletedIssueRepository(),
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        assertThrows(SecurityException.class,
                () -> service.deleteIssue(ISSUE_ID, "delete rejected", projectPl));
    }

    @Test
    @DisplayName("delete needs a reason")
    void deleteNeedsReason() {
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(issueWithStatus(IssueStatus.NEW)),
                new FakeDeletedIssueRepository(),
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteIssue(ISSUE_ID, " ", projectPl));
    }

    @Test
    @DisplayName("restore needs the deleted issue and reason")
    void restoreNeedsDeletedIssueAndReason() {
        var deletedIssues = new FakeDeletedIssueRepository();
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                new InMemoryIssueRepository(deletedIssue()),
                deletedIssues,
                users,
                new PermissionPolicy(),
                () -> NOW);

        var result = service.restoreIssue(ISSUE_ID, "restore it", projectPl);

        assertEquals(IssueStatus.NEW, result.status());
        assertEquals("restore it", deletedIssues.lastMessage);
        assertThrows(IllegalArgumentException.class,
                () -> service.restoreIssue(ISSUE_ID, " ", projectPl));
    }

    @Test
    @DisplayName("PL purges one deleted issue")
    void plPurgesOneIssue() {
        var issues = new InMemoryIssueRepository(deletedIssue());
        var deletedIssues = new FakeDeletedIssueRepository();
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                issues,
                deletedIssues,
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        service.purgeDeletedIssue(ISSUE_ID, projectPl);

        assertTrue(deletedIssues.lastPurgedId == ISSUE_ID);
    }

    @Test
    @DisplayName("purge skips non-deleted issues")
    void purgeSkipsNonDeletedIssue() {
        var issues = new InMemoryIssueRepository(issueWithStatus(IssueStatus.NEW));
        var users = new InMemoryUserRepository(reporter, projectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                issues,
                new FakeDeletedIssueRepository(),
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        assertThrows(IllegalArgumentException.class,
                () -> service.purgeDeletedIssue(ISSUE_ID, projectPl));
    }

    @Test
    @DisplayName("other project PL cannot purge")
    void otherPlCannotPurge() {
        var issues = new InMemoryIssueRepository(deletedIssue());
        var users = new InMemoryUserRepository(reporter, projectPl, otherProjectPl)
                .withProjectMembers(PROJECT_ID, projectPl.getLoginId());
        var service = new DeletedIssueService(
                issues,
                new FakeDeletedIssueRepository(),
                users,
                new PermissionPolicy(),
                java.time.LocalDateTime::now);

        assertThrows(SecurityException.class,
                () -> service.purgeDeletedIssue(ISSUE_ID, otherProjectPl));
        assertTrue(issues.findById(ISSUE_ID).isPresent());
    }

    private Issue deletedIssue() {
        return issueWithStatus(IssueStatus.DELETED);
    }

    private Issue issueWithStatus(IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Deleted issue", "Deleted description", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .updatedAt(NOW));
    }

    private final class FakeDeletedIssueRepository implements DeletedIssueRepository {

        private final java.util.List<Issue> deletedIssues;
        long lastPurgedId;
        long lastOverflowProjectId;
        String lastMessage;

        private FakeDeletedIssueRepository(Issue... deletedIssues) {
            this.deletedIssues = java.util.List.of(deletedIssues);
        }

        @Override
        public java.util.List<Issue> findDeletedByProject(long projectId) {
            return deletedIssues.stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .toList();
        }

        @Override
        public Issue softDelete(Issue issue, String changedById, String message, java.time.LocalDateTime changedDate) {
            lastMessage = message;
            return copyWithStatus(issue, IssueStatus.DELETED, changedDate);
        }

        @Override
        public Issue restore(Issue issue, String changedById, String message, java.time.LocalDateTime changedDate) {
            lastMessage = message;
            return copyWithStatus(issue, IssueStatus.NEW, changedDate);
        }

        @Override
        public int purgeDeletedById(long issueId) {
            lastPurgedId = issueId;
            return 1;
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            lastOverflowProjectId = projectId;
            return 0;
        }

        private Issue copyWithStatus(Issue issue, IssueStatus status, LocalDateTime updatedAt) {
            return Issue.fromPersistence(Issue.persistedState(issue.projectId(), issue.title(), issue.description(),
                    issue.getReporter())
                    .id(issue.id())
                    .issueId(issue.getIssueId())
                    .reportedDate(issue.reportedDate())
                    .priority(issue.priority())
                    .status(status)
                    .updatedAt(updatedAt));
        }
    }
}
