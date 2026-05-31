package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing login presenter")
class LoginPresenterTest {

    private static final String PASSWORD = "DemoLocalAdmin!";
    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("routes admin login to admin dashboard")
    void routesAdminLoginToAdminDashboard() {
        var view = new FakeLoginView("admin", PASSWORD);
        var navigator = new RecordingNavigator();
        var presenter = new LoginPresenter(controller(user("admin", Role.ADMIN)), view, navigator);

        presenter.loginRequested();

        assertEquals("admin", navigator.adminUser.loginId());
        assertNull(navigator.projectUser);
        assertEquals("", view.message);
        assertFalse(view.error);
        assertTrue(view.passwordCleared);
        assertTrue(view.enabled);
    }

    @Test
    @DisplayName("routes project users to project list")
    void routesProjectUsersToProjectList() {
        for (Role role : new Role[] {Role.PL, Role.DEV, Role.TESTER}) {
            var view = new FakeLoginView(role.name().toLowerCase(), PASSWORD);
            var navigator = new RecordingNavigator();
            var presenter = new LoginPresenter(controller(user(role.name().toLowerCase(), role)), view, navigator);

            presenter.loginRequested();

            assertNull(navigator.adminUser);
            assertEquals(role, navigator.projectUser.role());
            assertEquals("", view.message);
            assertFalse(view.error);
            assertTrue(view.passwordCleared);
            assertTrue(view.enabled);
        }
    }

    @Test
    @DisplayName("shows returned failure message and stays on login screen")
    void showsFailureMessageAndStaysOnLoginScreen() {
        var view = new FakeLoginView("admin", "wrong-password");
        var navigator = new RecordingNavigator();
        var presenter = new LoginPresenter(controller(user("admin", Role.ADMIN)), view, navigator);

        presenter.loginRequested();

        assertEquals("Invalid ID or password.", view.message);
        assertTrue(view.error);
        assertNull(navigator.adminUser);
        assertNull(navigator.projectUser);
        assertFalse(view.passwordCleared);
        assertTrue(view.enabled);
    }

    @Test
    @DisplayName("re-enables login when controller returns blank credential failure")
    void reEnablesLoginOnBlankCredentialFailure() {
        var view = new FakeLoginView(" ", PASSWORD);
        var navigator = new RecordingNavigator();
        var presenter = new LoginPresenter(controller(), view, navigator);

        presenter.loginRequested();

        assertEquals("ID and password are required.", view.message);
        assertTrue(view.error);
        assertTrue(view.enabled);
    }

    private static AuthenticationController controller(User... users) {
        var repository = new InMemoryUserRepository(users);
        var service = new AuthenticationService(repository, PASSWORD_HASHER, new SessionStore());
        return new AuthenticationController(service);
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(
                loginId,
                loginId,
                PASSWORD_HASHER.hash(PASSWORD),
                role,
                true,
                NOW,
                NOW);
    }

    private static final class FakeLoginView implements LoginView {

        private final String loginId;
        private final String password;
        private boolean enabled = true;
        private String message = "";
        private boolean error;
        private boolean passwordCleared;

        private FakeLoginView(String loginId, String password) {
            this.loginId = loginId;
            this.password = password;
        }

        @Override
        public String loginId() {
            return loginId;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public void setLoginEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
            this.error = error;
        }

        @Override
        public void clearPassword() {
            passwordCleared = true;
        }
    }

    private static final class RecordingNavigator implements SwingNavigator {

        private UserResult adminUser;
        private UserResult projectUser;

        @Override
        public void showAdminDashboard(UserResult user) {
            adminUser = user;
        }

        @Override
        public void showProjectList(UserResult user) {
            projectUser = user;
        }
    }
}
