package com.github.marcellokim.issuetracker.service;

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
    @DisplayName("PL must belong to the project to manage deleted issues")
    void rejectPlFromOtherProject() {
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
    @DisplayName("delete transition accepts only NEW or CLOSED issues")
    void rejectDeleteTransitionFromNonDeletableStatus() {
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
    @DisplayName("delete reason is required")
    void rejectBlankDeleteReason() {
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
    @DisplayName("project PL can purge one deleted issue permanently")
    void purgeDeletedIssueRemovesOnlyDeletedIssue() {
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
    @DisplayName("single issue purge accepts only deleted issues")
    void purgeDeletedIssueRejectsNonDeletedIssue() {
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
    @DisplayName("single issue purge requires the project PL")
    void purgeDeletedIssueRejectsOtherProjectPl() {
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

    private static final class FakeDeletedIssueRepository implements DeletedIssueRepository {

        long lastPurgedId;

        @Override
        public java.util.List<Issue> findDeletedByProject(long projectId) {
            return java.util.List.of();
        }

        @Override
        public Issue softDelete(Issue issue, String changedById, String message, java.time.LocalDateTime changedDate) {
            throw new UnsupportedOperationException("softDelete");
        }

        @Override
        public Issue restore(Issue issue, String changedById, String message, java.time.LocalDateTime changedDate) {
            throw new UnsupportedOperationException("restore");
        }

        @Override
        public int purgeDeletedById(long issueId) {
            lastPurgedId = issueId;
            return 1;
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            return 0;
        }
    }
}
