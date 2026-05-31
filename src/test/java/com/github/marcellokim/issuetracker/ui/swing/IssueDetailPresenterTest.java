package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryAssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing issue detail presenter")
class IssueDetailPresenterTest {

    private static final long PROJECT_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads issue detail with available actions and comment permissions through controllers")
    void loadsIssueDetailWithActionsAndCommentPermissions() {
        User reporter = user("dev1", Role.DEV);
        Issue issue = issue(7L, "Login bug", reporter);
        FakeCommentRepository comments = new FakeCommentRepository(
                comment(100L, issue.id(), reporter, CommentPurpose.GENERAL),
                comment(101L, issue.id(), reporter, CommentPurpose.STATUS_CHANGE));
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                controller(reporter, comments, issue),
                view);

        presenter.loadIssue(issue.id());

        assertEquals("Login bug", view.detail().title());
        assertTrue(view.detail().availableActions().contains("UPDATE_ISSUE"));
        assertTrue(view.detail().availableActions().contains("ADD_COMMENT"));
        assertEquals(true, view.commentAction("100").canUpdate());
        assertEquals(true, view.commentAction("100").canDelete());
        assertEquals(false, view.commentAction("101").canUpdate());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("refreshes action buttons without replacing the loaded detail")
    void refreshesAvailableActionsOnly() {
        User reporter = user("dev1", Role.DEV);
        Issue issue = issue(7L, "Login bug", reporter);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                controller(reporter, new FakeCommentRepository(), issue),
                view);
        presenter.loadIssue(issue.id());

        presenter.refreshActions(issue.id());

        assertEquals("Login bug", view.detail().title());
        assertTrue(view.actions().canUpdateIssue());
        assertTrue(view.actions().canAddComment());
    }

    @Test
    @DisplayName("changes status through state controller and reloads detail")
    void changesStatusAndReloadsDetail() {
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        Issue issue = assignedIssue(7L, "Login bug", assignee, verifier);
        IssueDetailControllerFixture fixture = controllerFixture(assignee, new FakeCommentRepository(), issue);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                fixture.issueController(),
                fixture.issueStateController(),
                view);
        presenter.loadIssue(issue.id());

        presenter.changeStatus(issue.id(), IssueStatus.FIXED, "fixed in swing");

        assertEquals(IssueStatus.FIXED, view.detail().status());
        assertEquals(assignee.getLoginId(), view.detail().fixer().loginId());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("changes assignment through assignment controller and reloads detail")
    void changesAssignmentAndReloadsDetail() {
        User pl = user("pl1", Role.PL);
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        Issue issue = issue(7L, "Login bug", pl);
        IssueDetailControllerFixture fixture = controllerFixture(
                pl,
                new FakeCommentRepository(),
                List.of(assignee, verifier),
                issue);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                fixture.issueController(),
                fixture.issueStateController(),
                fixture.assignmentController(),
                view);
        presenter.loadIssue(issue.id());

        presenter.changeAssignment(
                issue.id(),
                new IssueAssignmentRequest(
                        IssueAssignmentMode.ASSIGN,
                        assignee.getLoginId(),
                        verifier.getLoginId()));

        assertEquals(IssueStatus.ASSIGNED, view.detail().status());
        assertEquals(assignee.getLoginId(), view.detail().assignee().loginId());
        assertEquals(verifier.getLoginId(), view.detail().verifier().loginId());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("reassigns assignee through assignment controller and reloads detail")
    void reassignsAssigneeAndReloadsDetail() {
        User pl = user("pl1", Role.PL);
        User assignee = user("dev1", Role.DEV);
        User nextAssignee = user("dev2", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        Issue issue = assignedIssue(7L, "Login bug", assignee, verifier);
        IssueDetailControllerFixture fixture = controllerFixture(
                pl,
                new FakeCommentRepository(),
                List.of(assignee, nextAssignee, verifier),
                issue);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                fixture.issueController(),
                fixture.issueStateController(),
                fixture.assignmentController(),
                view);
        presenter.loadIssue(issue.id());

        presenter.changeAssignment(
                issue.id(),
                new IssueAssignmentRequest(
                        IssueAssignmentMode.REASSIGN_DEV,
                        nextAssignee.getLoginId(),
                        null));

        assertEquals(IssueStatus.ASSIGNED, view.detail().status());
        assertEquals(nextAssignee.getLoginId(), view.detail().assignee().loginId());
        assertEquals(verifier.getLoginId(), view.detail().verifier().loginId());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("changes verifier through assignment controller and reloads detail")
    void changesVerifierAndReloadsDetail() {
        User pl = user("pl1", Role.PL);
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        User nextVerifier = user("tester2", Role.TESTER);
        Issue issue = fixedIssue(7L, "Login bug", assignee, verifier);
        IssueDetailControllerFixture fixture = controllerFixture(
                pl,
                new FakeCommentRepository(),
                List.of(assignee, verifier, nextVerifier),
                issue);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                fixture.issueController(),
                fixture.issueStateController(),
                fixture.assignmentController(),
                view);
        presenter.loadIssue(issue.id());

        presenter.changeAssignment(
                issue.id(),
                new IssueAssignmentRequest(
                        IssueAssignmentMode.CHANGE_TESTER,
                        null,
                        nextVerifier.getLoginId()));

        assertEquals(IssueStatus.FIXED, view.detail().status());
        assertEquals(assignee.getLoginId(), view.detail().assignee().loginId());
        assertEquals(nextVerifier.getLoginId(), view.detail().verifier().loginId());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("shows controller errors without replacing current detail")
    void showsErrorsWithoutReplacingCurrentDetail() {
        User reporter = user("dev1", Role.DEV);
        Issue issue = issue(7L, "Login bug", reporter);
        RecordingIssueDetailView view = new RecordingIssueDetailView();
        IssueDetailPresenter presenter = new IssueDetailPresenter(
                controller(reporter, new FakeCommentRepository(), issue),
                view);
        presenter.loadIssue(issue.id());

        presenter.loadIssue(999L);

        assertEquals("Login bug", view.detail().title());
        assertEquals("Issue not found: 999", view.message());
    }

    private static IssueController controller(User currentUser, FakeCommentRepository comments, Issue... issues) {
        return controllerFixture(currentUser, comments, issues).issueController();
    }

    private static IssueDetailControllerFixture controllerFixture(
            User currentUser,
            FakeCommentRepository comments,
            Issue... issues) {
        return controllerFixture(currentUser, comments, List.of(), issues);
    }

    private static IssueDetailControllerFixture controllerFixture(
            User currentUser,
            FakeCommentRepository comments,
            List<User> extraUsers,
            Issue... issues) {
        InMemoryUserRepository users = new InMemoryUserRepository(currentUser)
                .withProjectMembers(PROJECT_ID, currentUser.getLoginId());
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project())
                .withParticipant(PROJECT_ID, currentUser.getLoginId());
        for (User user : extraUsers) {
            users.save(user);
            users.withProjectMembers(PROJECT_ID, user.getLoginId());
            projects.withParticipant(PROJECT_ID, user.getLoginId());
        }
        for (Issue issue : issues) {
            if (issue.getVerifier() != null) {
                users.save(issue.getVerifier());
                users.withProjectMembers(PROJECT_ID, issue.getVerifier().getLoginId());
                projects.withParticipant(PROJECT_ID, issue.getVerifier().getLoginId());
            }
        }
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        PermissionPolicy permissionPolicy = new PermissionPolicy();
        AuthenticationService authentication = new AuthenticationService(
                users,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login(currentUser.getLoginId(), "password");
        IssueService issueService = new IssueService(
                projects,
                issueRepository,
                dependencies,
                comments,
                new FakeIssueHistoryRepository(),
                users,
                permissionPolicy,
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issueRepository,
                dependencies,
                comments,
                users,
                permissionPolicy);
        IssueController issueController = new IssueController(authentication, issueService, workflowService);
        IssueStateController issueStateController = new IssueStateController(
                authentication,
                new IssueStateService(
                        issueRepository,
                        dependencies,
                        users,
                        permissionPolicy,
                        () -> NOW,
                        () -> "status-change-comment"));
        AssignmentController assignmentController = new AssignmentController(
                authentication,
                new AssignmentService(
                        issueRepository,
                        users,
                        permissionPolicy,
                        new AssignmentRecommendationService(
                                new InMemoryAssignmentRecommendationRepository(extraUsers.toArray(User[]::new)),
                                new KNNAssignmentRecommendation()),
                        () -> NOW));
        return new IssueDetailControllerFixture(issueController, issueStateController, assignmentController);
    }

    private static Project project() {
        return Project.fromPersistence(PROJECT_ID, "Alpha", "Alpha project", "admin", NOW, NOW);
    }

    private static Issue issue(long id, String title, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, title, "Issue description", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .status(IssueStatus.NEW)
                .priority(Priority.CRITICAL)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Issue assignedIssue(long id, String title, User assignee, User verifier) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, title, "Issue description", assignee)
                .id(id)
                .issueId("ISSUE-" + id)
                .status(IssueStatus.ASSIGNED)
                .priority(Priority.CRITICAL)
                .assignee(assignee)
                .verifier(verifier)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Issue fixedIssue(long id, String title, User assignee, User verifier) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, title, "Issue description", assignee)
                .id(id)
                .issueId("ISSUE-" + id)
                .status(IssueStatus.FIXED)
                .priority(Priority.CRITICAL)
                .assignee(assignee)
                .verifier(verifier)
                .fixer(assignee)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static Comment comment(long id, long issueId, User writer, CommentPurpose purpose) {
        return Comment.fromPersistence(id, issueId, writer.getLoginId(), "Comment " + id, purpose, NOW, NOW);
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private record IssueDetailControllerFixture(
            IssueController issueController,
            IssueStateController issueStateController,
            AssignmentController assignmentController) {
    }

    private static final class RecordingIssueDetailView implements IssueDetailView {

        private IssueDetailResult detail;
        private IssueWorkflowActions actions;
        private List<IssueCommentActionState> commentActions = List.of();
        private String message = " ";

        @Override
        public void showDetail(IssueDetailResult detail, List<IssueCommentActionState> commentActions) {
            this.detail = detail;
            this.commentActions = List.copyOf(commentActions);
        }

        @Override
        public void showActions(IssueWorkflowActions actions) {
            this.actions = actions;
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
        }

        private IssueDetailResult detail() {
            return detail;
        }

        private IssueWorkflowActions actions() {
            return actions;
        }

        private IssueCommentActionState commentAction(String commentId) {
            return commentActions.stream()
                    .filter(action -> action.commentId().equals(commentId))
                    .findFirst()
                    .orElseThrow();
        }

        private String message() {
            return message;
        }
    }

    private static final class FakeCommentRepository implements CommentRepository {

        private final List<Comment> comments;

        private FakeCommentRepository(Comment... comments) {
            this.comments = List.of(comments);
        }

        @Override
        public Optional<Comment> findById(long commentId) {
            return comments.stream()
                    .filter(comment -> comment.id() == commentId)
                    .findFirst();
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return comments.stream()
                    .filter(comment -> comment.issueId() == issueId)
                    .toList();
        }

        @Override
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
            throw new UnsupportedOperationException("Comment writes are outside this presenter test.");
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            throw new UnsupportedOperationException("Comment deletes are outside this presenter test.");
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
