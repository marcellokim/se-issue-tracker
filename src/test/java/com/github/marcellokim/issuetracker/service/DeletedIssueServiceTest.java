package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
                users,
                new PermissionPolicy(),
                new Clock());

        assertThrows(SecurityException.class,
                () -> service.viewDeletedIssues(PROJECT_ID, otherProjectPl));
    }

    private Issue deletedIssue() {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Deleted issue", "Deleted description", reporter)
                .id(ISSUE_ID)
                .issueId("ISSUE-1")
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.DELETED)
                .updatedAt(NOW));
    }
}
