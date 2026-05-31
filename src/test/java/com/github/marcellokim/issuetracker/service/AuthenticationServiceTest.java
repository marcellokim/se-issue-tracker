package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Authentication service")
class AuthenticationServiceTest {

    private static final String ADMIN_PASSWORD = "DemoLocalAdmin!";
    private static final TestPasswordHashing PASSWORDS = new TestPasswordHashing();

    @Test
    @DisplayName("admin logs in")
    void adminLogsIn() {
        var service = service(user("admin", ADMIN_PASSWORD, Role.ADMIN, true));

        AuthenticationResult result = service.login("admin", ADMIN_PASSWORD);

        assertTrue(result.success());
        assertEquals("admin", result.user().loginId());
        assertEquals(Role.ADMIN, result.user().role());
    }

    @Test
    @DisplayName("wrong password fails login")
    void wrongPasswordFails() {
        var service = service(user("admin", ADMIN_PASSWORD, Role.ADMIN, true));

        AuthenticationResult result = service.login("admin", "wrong-password");

        assertFalse(result.success());
        assertEquals("Invalid ID or password.", result.message());
    }

    @Test
    @DisplayName("inactive user cannot log in")
    void inactiveUserIsBlocked() {
        var service = service(user("dev1", ADMIN_PASSWORD, Role.DEV, false));

        AuthenticationResult result = service.login("dev1", ADMIN_PASSWORD);

        assertFalse(result.success());
        assertEquals("This account is inactive.", result.message());
    }

    @Test
    @DisplayName("wrong password is checked first")
    void passwordCheckedFirst() {
        var service = service(user("dev1", ADMIN_PASSWORD, Role.DEV, false));

        AuthenticationResult result = service.login("dev1", "wrong-password");

        assertFalse(result.success());
        assertEquals("Invalid ID or password.", result.message());
    }

    @Test
    @DisplayName("login needs id and password")
    void loginNeedsIdAndPassword() {
        var service = service();

        assertEquals("ID and password are required.", service.login(null, ADMIN_PASSWORD).message());
        assertEquals("ID and password are required.", service.login(" ", ADMIN_PASSWORD).message());
        assertEquals("ID and password are required.", service.login("admin", null).message());
        assertEquals("ID and password are required.", service.login("admin", " ").message());
    }

    @Test
    @DisplayName("login id can have spaces around it")
    void loginIdCanHaveSpaces() {
        var service = service(user("admin", ADMIN_PASSWORD, Role.ADMIN, true));

        assertFalse(service.currentUser().isPresent());
        AuthenticationResult result = service.login(" admin ", ADMIN_PASSWORD);

        assertTrue(result.success());
        assertEquals("admin", service.currentUser().orElseThrow().getLoginId());
    }

    @Test
    @DisplayName("second login waits for logout")
    void secondLoginWaitsForLogout() {
        var service = service(
                user("admin", ADMIN_PASSWORD, Role.ADMIN, true),
                user("dev1", "DevPassword1!", Role.DEV, true));

        assertTrue(service.login("admin", ADMIN_PASSWORD).success());
        AuthenticationResult result = service.login("dev1", "DevPassword1!");

        assertFalse(result.success());
        assertEquals("Already logged in. Please logout first.", result.message());
    }

    @Test
    @DisplayName("logout clears current user")
    void logoutClearsCurrentUser() {
        var service = service(user("admin", ADMIN_PASSWORD, Role.ADMIN, true));

        assertTrue(service.login("admin", ADMIN_PASSWORD).success());
        assertTrue(service.currentUser().isPresent());

        service.logout();

        assertFalse(service.currentUser().isPresent());
    }

    @Test
    @DisplayName("missing user cannot log in")
    void missingUserCannotLogIn() {
        var service = service();

        AuthenticationResult result = service.login("missing", ADMIN_PASSWORD);

        assertFalse(result.success());
        assertEquals("Invalid ID or password.", result.message());
    }

    private static User user(String loginId, String password, Role role, boolean active) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 18, 0, 0);
        return User.fromPersistence(loginId, loginId, PASSWORDS.hash(password), role, active, timestamp, timestamp);
    }

    private static AuthenticationService service(User... users) {
        return new AuthenticationService(
                new InMemoryUserRepository(users),
                PASSWORDS,
                new TestSession());
    }

    private static final class TestPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return "hash:" + password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            return storedCredential.equals(hash(password));
        }

        @Override
        public boolean isHashed(String storedCredential) {
            return storedCredential != null && storedCredential.startsWith("hash:");
        }

        @Override
        public String saltOf(String storedCredential) {
            return "";
        }

        @Override
        public String hashOf(String storedCredential) {
            return storedCredential == null ? "" : storedCredential.replaceFirst("^hash:", "");
        }
    }

    private static final class TestSession implements CurrentUserSession {
        private String currentLoginId;

        @Override
        public void start(String loginId) {
            currentLoginId = loginId;
        }

        @Override
        public Optional<String> currentLoginId() {
            return Optional.ofNullable(currentLoginId);
        }

        @Override
        public void clear() {
            currentLoginId = null;
        }
    }
}
