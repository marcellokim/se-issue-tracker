package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Authentication service")
class AuthenticationServiceTest {

    private static final String ADMIN_PASSWORD = "DemoLocalAdmin!";
    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();

    @Test
    @DisplayName("accepts matching seeded admin credentials")
    void loginAcceptsSeededAdminCredentials() {
        var service = new AuthenticationService(new FakeUserRepository(List.of(
                user("admin", ADMIN_PASSWORD, Role.ADMIN, true)
        )));

        AuthenticationResult result = service.login("admin", ADMIN_PASSWORD);

        assertTrue(result.success());
        assertNotNull(result.user());
        assertEquals(Role.ADMIN, result.user().getRole());
    }

    @Test
    @DisplayName("rejects incorrect password")
    void loginRejectsIncorrectPassword() {
        var service = new AuthenticationService(new FakeUserRepository(List.of(
                user("admin", ADMIN_PASSWORD, Role.ADMIN, true)
        )));

        AuthenticationResult result = service.login("admin", "wrong-password");

        assertFalse(result.success());
        assertEquals("Invalid ID or password.", result.message());
    }

    @Test
    @DisplayName("rejects inactive account")
    void loginRejectsInactiveAccount() {
        var service = new AuthenticationService(new FakeUserRepository(List.of(
                user("dev1", ADMIN_PASSWORD, Role.DEV, false)
        )));

        AuthenticationResult result = service.login("dev1", ADMIN_PASSWORD);

        assertFalse(result.success());
        assertEquals("This account is inactive.", result.message());
    }

    @Test
    @DisplayName("rejects missing credentials before repository lookup")
    void loginRejectsMissingCredentials() {
        var service = new AuthenticationService(new FakeUserRepository(List.of()));

        assertEquals("ID and password are required.", service.login(null, ADMIN_PASSWORD).message());
        assertEquals("ID and password are required.", service.login(" ", ADMIN_PASSWORD).message());
        assertEquals("ID and password are required.", service.login("admin", null).message());
        assertEquals("ID and password are required.", service.login("admin", " ").message());
    }

    @Test
    @DisplayName("trims login id and stores authenticated current user")
    void loginTrimsLoginIdAndStoresCurrentUser() {
        var service = new AuthenticationService(new FakeUserRepository(List.of(
                user("admin", ADMIN_PASSWORD, Role.ADMIN, true)
        )));

        assertFalse(service.currentUser().isPresent());
        AuthenticationResult result = service.logIn(" admin ", ADMIN_PASSWORD);

        assertTrue(result.success());
        assertTrue(service.currentUser().isPresent());
        assertEquals("admin", service.currentUser().orElseThrow().getLoginId());
    }

    @Test
    @DisplayName("rejects unknown account")
    void loginRejectsUnknownAccount() {
        var service = new AuthenticationService(new FakeUserRepository(List.of()));

        AuthenticationResult result = service.login("missing", ADMIN_PASSWORD);

        assertFalse(result.success());
        assertEquals("Invalid ID or password.", result.message());
    }

    private static User user(String loginId, String password, Role role, boolean active) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 18, 0, 0);
        return User.fromPersistence(loginId, loginId, PASSWORD_HASHER.hash(password), role, active, timestamp, timestamp);
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> usersByLoginId = new LinkedHashMap<>();

        private FakeUserRepository(List<User> users) {
            for (User user : users) {
                usersByLoginId.put(user.getLoginId(), user);
            }
        }

        @Override
        public Optional<User> findById(String userId) {
            return findByLoginId(userId);
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return Optional.ofNullable(usersByLoginId.get(loginId));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersByLoginId.values());
        }

        @Override
        public List<User> findByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public List<User> findActiveByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(User::isActive)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            usersByLoginId.put(user.getLoginId(), user);
            return user;
        }

        @Override
        public void deactivate(String loginId) {
            findById(loginId).ifPresent(user -> usersByLoginId.put(
                    user.getLoginId(),
                    User.fromPersistence(
                            user.getLoginId(),
                            user.getName(),
                            user.getPasswordHash(),
                            user.getRole(),
                            false,
                            user.getCreatedAt(),
                            user.getUpdatedAt()
                    )
            ));
        }
    }
}
