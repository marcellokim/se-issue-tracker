package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing account management presenter")
class AccountManagementPresenterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads users through dashboard controller")
    void loadsUsers() {
        ControllerFixture fixture = controllers(
                user("admin", Role.ADMIN, true),
                user("dev1", Role.DEV, true));
        RecordingAccountManagementView view = new RecordingAccountManagementView();
        AccountManagementPresenter presenter = new AccountManagementPresenter(
                fixture.dashboardController(),
                fixture.accountController(),
                view);

        presenter.loadUsers();

        assertEquals(List.of("admin", "dev1"), view.loginIds());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("creates account and refreshes users after success")
    void createsAccountAndRefreshesUsers() {
        ControllerFixture fixture = controllers(user("admin", Role.ADMIN, true));
        RecordingAccountManagementView view = new RecordingAccountManagementView();
        AccountManagementPresenter presenter = new AccountManagementPresenter(
                fixture.dashboardController(),
                fixture.accountController(),
                view);

        presenter.createAccount(new AccountCreateRequest("tester1", "Tester One", "password", Role.TESTER));

        assertEquals(List.of("admin", "tester1"), view.loginIds());
        assertEquals("Account created: tester1", view.message());
    }

    @Test
    @DisplayName("renames, changes role, deactivates, and activates an account through controller")
    void updatesAccountLifecycle() {
        ControllerFixture fixture = controllers(
                user("admin", Role.ADMIN, true),
                user("dev1", Role.DEV, true));
        RecordingAccountManagementView view = new RecordingAccountManagementView();
        AccountManagementPresenter presenter = new AccountManagementPresenter(
                fixture.dashboardController(),
                fixture.accountController(),
                view);

        presenter.renameAccount("dev1", "Developer One");
        presenter.changeAccountRole("dev1", Role.TESTER);
        presenter.deactivateAccount("dev1");
        presenter.activateAccount("dev1");

        UserResult updated = view.users().stream()
                .filter(user -> user.loginId().equals("dev1"))
                .findFirst()
                .orElseThrow();
        assertEquals("Developer One", updated.name());
        assertEquals(Role.TESTER, updated.role());
        assertTrue(updated.active());
        assertEquals("Account activated: dev1", view.message());
    }

    @Test
    @DisplayName("shows controller errors without replacing the current user list")
    void showsControllerError() {
        ControllerFixture fixture = controllers(
                user("admin", Role.ADMIN, true),
                user("dev1", Role.DEV, true));
        RecordingAccountManagementView view = new RecordingAccountManagementView();
        AccountManagementPresenter presenter = new AccountManagementPresenter(
                fixture.dashboardController(),
                fixture.accountController(),
                view);
        presenter.loadUsers();

        presenter.renameAccount("dev1", "dev1");

        assertEquals(List.of("admin", "dev1"), view.loginIds());
        assertEquals("name is same with current name.", view.message());
    }

    private static ControllerFixture controllers(User... users) {
        InMemoryUserRepository userRepository = new InMemoryUserRepository(users);
        AuthenticationService authentication = new AuthenticationService(
                userRepository,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login("admin", "password");
        PermissionPolicy permissionPolicy = new PermissionPolicy();
        return new ControllerFixture(
                new DashboardController(
                        authentication,
                        new DashboardSummaryService(
                                new EmptyDashboardSummaryRepository(),
                                userRepository,
                                permissionPolicy)),
                new AccountController(
                        authentication,
                        new AccountService(
                                permissionPolicy,
                                userRepository,
                                new InMemoryProjectRepository(),
                                new InMemoryIssueRepository(),
                                new AcceptingPasswordHashing(),
                                () -> NOW)));
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private record ControllerFixture(
            DashboardController dashboardController,
            AccountController accountController) {
    }

    private static final class RecordingAccountManagementView implements AccountManagementView {

        private List<UserResult> users = List.of();
        private String message = " ";

        @Override
        public void showUsers(List<UserResult> users) {
            this.users = List.copyOf(users);
            this.message = " ";
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
        }

        private List<UserResult> users() {
            return users;
        }

        private List<String> loginIds() {
            return users.stream()
                    .map(UserResult::loginId)
                    .toList();
        }

        private String message() {
            return message;
        }
    }

    private static final class EmptyDashboardSummaryRepository implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return List.of();
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return List.of();
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
