package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.support.FakeDeletedIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing deleted issue presenter")
class DeletedIssuePresenterTest {

    private static final long PROJECT_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads deleted issues after controller permission succeeds")
    void loadsDeletedIssuesAfterPermissionSucceeds() {
        User pl = user("pl1", Role.PL);
        Issue deletedIssue = issue(7L, "Removed login bug", pl, IssueStatus.DELETED);
        Fixture fixture = fixture(pl, deletedIssue);
        RecordingDeletedIssueView view = new RecordingDeletedIssueView();
        DeletedIssuePresenter presenter = new DeletedIssuePresenter(fixture.controller(), view);

        presenter.loadDeletedIssues(PROJECT_ID);

        assertEquals(30, view.maxRetentionLimit);
        assertEquals(List.of(7L), view.issues.stream().map(IssueSummary::id).toList());
        assertEquals("Deleted issues 1/30", view.message);
        assertEquals(false, view.error);
    }

    @Test
    @DisplayName("does not replace rows when permission fails")
    void doesNotReplaceRowsWhenPermissionFails() {
        User dev = user("dev1", Role.DEV);
        Issue deletedIssue = issue(7L, "Removed login bug", dev, IssueStatus.DELETED);
        Fixture fixture = fixture(dev, deletedIssue);
        RecordingDeletedIssueView view = new RecordingDeletedIssueView();
        DeletedIssuePresenter presenter = new DeletedIssuePresenter(fixture.controller(), view);

        presenter.loadDeletedIssues(PROJECT_ID);

        assertEquals(0, view.maxRetentionLimit);
        assertEquals(List.of(), view.issues);
        assertEquals("Only PL can manage deleted issues.", view.message);
        assertEquals(true, view.error);
    }

    @Test
    @DisplayName("restores and purges selected issue then refreshes list")
    void restoresAndPurgesSelectedIssueThenRefreshesList() {
        User pl = user("pl1", Role.PL);
        Issue restoreTarget = issue(7L, "Removed login bug", pl, IssueStatus.DELETED);
        Issue purgeTarget = issue(8L, "Removed profile typo", pl, IssueStatus.DELETED);
        Fixture fixture = fixture(pl, restoreTarget, purgeTarget);
        RecordingDeletedIssueView view = new RecordingDeletedIssueView();
        DeletedIssuePresenter presenter = new DeletedIssuePresenter(fixture.controller(), view);

        presenter.restoreIssue(PROJECT_ID, restoreTarget.id(), "Restore from Swing demo");

        assertEquals(List.of(8L), view.issues.stream().map(IssueSummary::id).toList());
        assertEquals("Issue restored.", view.message);
        assertEquals(false, view.error);

        presenter.purgeDeletedIssue(PROJECT_ID, purgeTarget.id());

        assertEquals(List.of(), view.issues.stream().map(IssueSummary::id).toList());
        assertEquals("Deleted issue purged.", view.message);
        assertEquals(false, view.error);
    }

    private static Fixture fixture(User actor, Issue... issues) {
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        FakeDeletedIssueRepository deletedIssueRepository = new FakeDeletedIssueRepository(issueRepository);
        for (Issue issue : issues) {
            if (issue.status() == IssueStatus.DELETED) {
                deletedIssueRepository.addDeletedIssue(issue);
            }
        }
        InMemoryUserRepository users = new InMemoryUserRepository(actor)
                .withProjectMembers(PROJECT_ID, actor.getLoginId());
        AuthenticationService authentication = new AuthenticationService(
                users,
                new PlainPasswordHashing(),
                new SessionStore());
        new AuthenticationController(authentication).login(actor.getLoginId(), "password");
        DeletedIssueController controller = new DeletedIssueController(
                authentication,
                new DeletedIssueService(
                        issueRepository,
                        deletedIssueRepository,
                        users,
                        new PermissionPolicy(),
                        () -> NOW));
        return new Fixture(controller);
    }

    private static Issue issue(long id, String title, User reporter, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(PROJECT_ID, title, "Issue description", reporter)
                .id(id)
                .issueId("ISSUE-" + id)
                .status(status)
                .priority(Priority.CRITICAL)
                .reportedDate(NOW)
                .updatedAt(NOW));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "password", role, true, NOW, NOW);
    }

    private record Fixture(DeletedIssueController controller) {
    }

    private static final class RecordingDeletedIssueView implements DeletedIssueView {

        private int maxRetentionLimit;
        private List<IssueSummary> issues = List.of();
        private String message;
        private boolean error;

        @Override
        public void showDeletedIssues(int maxRetentionLimit, List<IssueSummary> issues) {
            this.maxRetentionLimit = maxRetentionLimit;
            this.issues = issues;
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
            this.error = error;
        }
    }

    private static final class PlainPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            return storedCredential.equals(password);
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
