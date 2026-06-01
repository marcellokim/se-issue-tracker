package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.IssueController;
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
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
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
        InMemoryUserRepository users = new InMemoryUserRepository(currentUser)
                .withProjectMembers(PROJECT_ID, currentUser.getLoginId());
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project())
                .withParticipant(PROJECT_ID, currentUser.getLoginId());
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
        return new IssueController(authentication, issueService, workflowService);
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

    private static Comment comment(long id, long issueId, User writer, CommentPurpose purpose) {
        return Comment.fromPersistence(id, issueId, writer.getLoginId(), "Comment " + id, purpose, NOW, NOW);
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
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
