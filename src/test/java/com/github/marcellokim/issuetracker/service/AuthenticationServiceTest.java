package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.UserRepository;
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

    @Test
    @DisplayName("accepts matching seeded admin credentials")
    void loginAcceptsSeededAdminCredentials() {
        var service = new AuthenticationService(new FakeUserRepository(List.of(
                user("admin", ADMIN_PASSWORD, Role.ADMIN, true)
        )));

        AuthenticationResult result = service.login("admin", ADMIN_PASSWORD);

        assertTrue(result.success());
        assertNotNull(result.user());
        assertEquals(Role.ADMIN, result.user().role());
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

    private static User user(String loginId, String password, Role role, boolean active) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 18, 0, 0);
        return new User(loginId, password, role, active, timestamp, timestamp);
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> usersByLoginId = new LinkedHashMap<>();

        private FakeUserRepository(List<User> users) {
            for (User user : users) {
                usersByLoginId.put(user.loginId(), user);
            }
        }

        @Override
        public Optional<User> findById(String loginId) {
            return findByLoginId(loginId);
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
        public List<User> findActiveByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(User::active)
                    .filter(user -> user.role() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            usersByLoginId.put(user.loginId(), user);
            return user;
        }

        @Override
        public void deactivate(String loginId) {
            findById(loginId).ifPresent(user -> usersByLoginId.put(
                    user.loginId(),
                    new User(
                            user.loginId(),
                            user.password(),
                            user.role(),
                            false,
                            user.createdAt(),
                            user.updatedAt()
                    )
            ));
        }
    }
}
