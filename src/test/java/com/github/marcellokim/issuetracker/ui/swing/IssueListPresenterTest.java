package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.support.SequentialIssueIdProvider;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue list presenter")
class IssueListPresenterTest {

    private static final long PROJECT_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads project info, project issues, and register permission through controllers")
    void loadsProjectAndProjectIssues() {
        User dev = user("dev1", Role.DEV);
        ControllerFixture fixture = controllers(
                dev,
                issue(1L, "Login bug", "Cannot login", Priority.CRITICAL, dev),
                issue(2L, "Tester-only issue", "Visible to tester", Priority.MINOR, user("tester1", Role.TESTER)));
        RecordingIssueListView view = new RecordingIssueListView();
        IssueListPresenter presenter = new IssueListPresenter(
                fixture.projectController(),
                fixture.issueController(),
                view);

        presenter.loadProjectAndIssues(PROJECT_ID);

        assertEquals("Alpha", view.projectName());
        assertEquals(List.of("Login bug", "Tester-only issue"), view.issueTitles());
        assertEquals(true, view.registerEnabled());
        assertEquals("2 issues", view.message());
    }

    @Test
    @DisplayName("searches issues with keyword, status, and priority filters")
    void searchesIssuesWithFilters() {
        User dev = user("dev1", Role.DEV);
        ControllerFixture fixture = controllers(
                dev,
                issue(1L, "Login bug", "Cannot login", Priority.CRITICAL, dev),
                issue(2L, "Profile typo", "Minor copy", Priority.MINOR, dev));
        RecordingIssueListView view = new RecordingIssueListView();
        IssueListPresenter presenter = new IssueListPresenter(
                fixture.projectController(),
                fixture.issueController(),
                view);

        presenter.searchIssues(PROJECT_ID, new IssueSearchRequest("login", IssueStatus.NEW, Priority.CRITICAL));

        assertEquals(List.of("Login bug"), view.issueTitles());
        assertEquals("1 issues", view.message());
    }

    @Test
    @DisplayName("search returns all project issues matching criteria regardless of role")
    void searchReturnsAllMatchingIssues() {
        User dev = user("dev1", Role.DEV);
        ControllerFixture fixture = controllers(
                dev,
                issue(1L, "Login bug", "Cannot login", Priority.CRITICAL, dev),
                issue(2L, "Login hidden", "Visible to tester", Priority.CRITICAL, user("tester1", Role.TESTER)));
        RecordingIssueListView view = new RecordingIssueListView();
        IssueListPresenter presenter = new IssueListPresenter(
                fixture.projectController(),
                fixture.issueController(),
                view);

        presenter.searchIssues(PROJECT_ID, new IssueSearchRequest("login", null, null));

        assertEquals(List.of("Login bug", "Login hidden"), view.issueTitles());
        assertEquals("2 issues", view.message());
    }

    @Test
    @DisplayName("registers issue and refreshes the issue list")
    void registersIssueAndRefreshesList() {
        User dev = user("dev1", Role.DEV);
        ControllerFixture fixture = controllers(dev);
        RecordingIssueListView view = new RecordingIssueListView();
        IssueListPresenter presenter = new IssueListPresenter(
                fixture.projectController(),
                fixture.issueController(),
                view);

        presenter.registerIssue(PROJECT_ID, new IssueRegisterRequest("New issue", "New issue description", Priority.MINOR));

        assertEquals(List.of("New issue"), view.issueTitles());
        assertEquals("Issue registered: New issue", view.message());
    }

    @Test
    @DisplayName("shows controller errors without replacing current issues")
    void showsErrorsWithoutReplacingCurrentIssues() {
        User dev = user("dev1", Role.DEV);
        ControllerFixture fixture = controllers(dev, issue(1L, "Login bug", "Cannot login", Priority.CRITICAL, dev));
        RecordingIssueListView view = new RecordingIssueListView();
        IssueListPresenter presenter = new IssueListPresenter(
                fixture.projectController(),
                fixture.issueController(),
                view);
        presenter.loadProjectAndIssues(PROJECT_ID);

        presenter.searchIssues(PROJECT_ID, new IssueSearchRequest(null, IssueStatus.DELETED, null));

        assertEquals(List.of("Login bug"), view.issueTitles());
        assertEquals("Deleted issues must be managed through deleted issue workflow.", view.message());
    }

    private static ControllerFixture controllers(User currentUser, Issue... issues) {
        User tester = user("tester1", Role.TESTER);
        User pl = user("pl1", Role.PL);
        InMemoryUserRepository users = new InMemoryUserRepository(currentUser, tester, pl);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project())
                .withParticipant(PROJECT_ID, currentUser.getLoginId())
                .withParticipant(PROJECT_ID, tester.getLoginId())
                .withParticipant(PROJECT_ID, pl.getLoginId());
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        AuthenticationService authentication = new AuthenticationService(
                users,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login(currentUser.getLoginId(), "password");
        PermissionPolicy permissionPolicy = new PermissionPolicy();
        ProjectController projectController = new ProjectController(
                authentication,
                new ProjectService(
                        projects,
                        issueRepository,
                        users,
                        permissionPolicy,
                        () -> NOW));
        IssueController issueController = new IssueController(
                authentication,
                new IssueService(
                        projects,
                        issueRepository,
                        new FakeIssueDependencyRepository(),
                        new FakeCommentRepository(),
                        new FakeIssueHistoryRepository(),
                        users,
                        permissionPolicy,
                        new SequentialIssueIdProvider(),
                        () -> NOW));
        return new ControllerFixture(projectController, issueController);
    }

    private static Project project() {
        return Project.fromPersistence(PROJECT_ID, "Alpha", "Alpha project", "admin", NOW, NOW);
    }

    private static Issue issue(long id, String title, String description, Priority priority, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, title, description, reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .priority(priority)
                .reportedDate(NOW.plusMinutes(id))
                .updatedAt(NOW.plusMinutes(id)));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private record ControllerFixture(ProjectController projectController, IssueController issueController) {
    }

    private static final class RecordingIssueListView implements IssueListView {

        private ProjectResult project;
        private List<IssueSummary> issues = List.of();
        private boolean registerEnabled;
        private String message = " ";

        @Override
        public void showProject(ProjectResult project) {
            this.project = project;
        }

        @Override
        public void showIssues(List<IssueSummary> issues) {
            this.issues = List.copyOf(issues);
        }

        @Override
        public void setRegisterEnabled(boolean enabled) {
            registerEnabled = enabled;
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
        }

        private String projectName() {
            return project.name();
        }

        private List<String> issueTitles() {
            return issues.stream()
                    .map(IssueSummary::title)
                    .toList();
        }

        private boolean registerEnabled() {
            return registerEnabled;
        }

        private String message() {
            return message;
        }
    }

    private static final class FakeCommentRepository implements CommentRepository {

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.empty();
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return List.of();
        }

        @Override
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
            return comment;
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            // Comment deletion is outside the presenter scenarios covered by this fake.
        }
    }

    private static final class AcceptingPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return "hashed-" + password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            return true;
        }

        @Override
        public boolean isHashed(String storedCredential) {
            return true;
        }

        @Override
        public String saltOf(String storedCredential) {
            return "salt";
        }

        @Override
        public String hashOf(String storedCredential) {
            return storedCredential;
        }
    }
}
