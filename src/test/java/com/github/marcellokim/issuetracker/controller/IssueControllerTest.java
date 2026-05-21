package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue controller")
class IssueControllerTest {

    private static final long PROJECT_ID = 10L;
    private static final long ISSUE_ID = 1L;
    private static final String PASSWORD = "pass123";
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);
    private final PasswordHasher hasher = new PasswordHasher();
    private final User dev = User.create("dev1", "Dev One", hasher.hash(PASSWORD), Role.DEV, true, now, now);
    private final Project project = Project.create(PROJECT_ID, "ITS", "Issue Tracking", "admin", now, now);

    @Test
    @DisplayName("authenticated user registers issue")
    void registerIssue() {
        var controller = authenticatedController(dev);

        IssueResult result = controller.registerIssue(PROJECT_ID, "Bug", "Login fails", Priority.MAJOR);

        assertNotNull(result.issueId());
        assertEquals(IssueStatus.NEW, result.status());
        assertEquals("Bug", result.title());
    }

    @Test
    @DisplayName("authenticated user adds comment")
    void addComment() {
        var issue = persistedIssue();
        var controller = authenticatedController(dev, issue);

        CommentResult result = controller.addComment(ISSUE_ID, "Confirmed this bug");

        assertEquals("Confirmed this bug", result.content());
        assertEquals(dev, result.writer());
    }

    @Test
    @DisplayName("unauthenticated user is rejected")
    void rejectUnauthenticated() {
        var controller = unauthenticatedController();

        assertThrows(SecurityException.class,
                () -> controller.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR));
        assertThrows(SecurityException.class,
                () -> controller.addComment(ISSUE_ID, "comment"));
    }

    private IssueController authenticatedController(User user, Issue... issues) {
        var users = new InMemoryUserRepository(user);
        var sessionStore = new SessionStore();
        var authService = new AuthenticationService(users, hasher, sessionStore);
        authService.login(user.getLoginId(), PASSWORD);
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                new InMemoryIssueRepository(issues),
                users,
                new PermissionPolicy(),
                new Clock()
        );
        return new IssueController(authService, issueService);
    }

    private IssueController unauthenticatedController() {
        var users = new InMemoryUserRepository(dev);
        var authService = new AuthenticationService(users, hasher, new SessionStore());
        var issueService = new IssueService(
                new FakeProjectRepository(project),
                new InMemoryIssueRepository(),
                users,
                new PermissionPolicy(),
                new Clock()
        );
        return new IssueController(authService, issueService);
    }

    private Issue persistedIssue() {
        return Issue.fromPersistence(
                Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", dev)
                        .id(ISSUE_ID)
                        .issueId("ISSUE-1")
                        .reportedDate(now)
                        .priority(Priority.MAJOR)
                        .status(IssueStatus.NEW)
                        .updatedAt(now));
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projects = new LinkedHashMap<>();

        private FakeProjectRepository(Project... projects) {
            for (Project p : projects) {
                this.projects.put(p.getId(), p);
            }
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return Optional.ofNullable(projects.get(projectId));
        }

        @Override
        public Optional<Project> findByName(String name) {
            return Optional.empty();
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public Project save(Project project) {
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public void deleteById(long projectId) {
            projects.remove(projectId);
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return List.of();
        }
    }
}
