package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.IssueDependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDependencyService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue dependency controller")
class IssueDependencyControllerTest {

    private static final long PROJECT_ID = 10L;
    private static final String PASSWORD = "pass123";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

    private final PasswordHasher hasher = new PasswordHasher();
    private final User reporter = User.create("tester1", "Tester One", hasher.hash(PASSWORD), Role.TESTER, true,
            NOW, NOW);
    private final User pl = User.create("pl1", "PL One", hasher.hash(PASSWORD), Role.PL, true, NOW, NOW);

    @Test
    @DisplayName("authenticated PL adds, lists, and removes dependency")
    void authenticatedAddListRemove() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        InMemoryIssueDependencyRepository dependencies = new InMemoryIssueDependencyRepository();
        IssueDependencyController controller = authenticatedController(dependencies, blocking, blocked);

        IssueDependencyResult added = controller.addDependency(blocking.id(), blocked.id());
        List<IssueDependencyResult> listed = controller.listDependencies(blocked.id());
        controller.removeDependency(added.id());

        assertEquals(blocking.id(), added.blockingIssueId());
        assertEquals(blocked.id(), added.blockedIssueId());
        assertEquals(List.of(added.id()), listed.stream().map(IssueDependencyResult::id).toList());
        assertEquals(List.of(), dependencies.findAll());
    }

    @Test
    @DisplayName("unauthenticated add, list, and remove are rejected")
    void rejectUnauthenticated() {
        Issue blocking = issue(101L);
        Issue blocked = issue(102L);
        IssueDependency dependency = IssueDependency.fromPersistence(1L, blocking.id(), blocked.id(), NOW);
        IssueDependencyController controller = unauthenticatedController(
                new InMemoryIssueDependencyRepository(dependency),
                blocking,
                blocked);

        assertThrows(SecurityException.class, () -> controller.addDependency(blocking.id(), blocked.id()));
        assertThrows(SecurityException.class, () -> controller.listDependencies(blocked.id()));
        assertThrows(SecurityException.class, () -> controller.removeDependency(dependency.id()));
    }

    private IssueDependencyController authenticatedController(
            InMemoryIssueDependencyRepository dependencies,
            Issue... issues
    ) {
        InMemoryUserRepository users = new InMemoryUserRepository(reporter, pl);
        SessionStore sessionStore = new SessionStore();
        AuthenticationService authService = new AuthenticationService(users, hasher, sessionStore);
        authService.login(pl.getLoginId(), PASSWORD);
        return new IssueDependencyController(authService, dependencyService(users, dependencies, issues));
    }

    private IssueDependencyController unauthenticatedController(
            InMemoryIssueDependencyRepository dependencies,
            Issue... issues
    ) {
        InMemoryUserRepository users = new InMemoryUserRepository(reporter, pl);
        AuthenticationService authService = new AuthenticationService(users, hasher, new SessionStore());
        return new IssueDependencyController(authService, dependencyService(users, dependencies, issues));
    }

    private IssueDependencyService dependencyService(
            InMemoryUserRepository users,
            InMemoryIssueDependencyRepository dependencies,
            Issue... issues
    ) {
        return new IssueDependencyService(
                new InMemoryIssueRepository(issues),
                dependencies,
                dependencies,
                users,
                new PermissionPolicy(),
                new Clock());
    }

    private Issue issue(long id) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Issue " + id, "Dependency test", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(NOW));
    }
}
