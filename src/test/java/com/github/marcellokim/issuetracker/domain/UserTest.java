package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User domain model")
class UserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("create builds an active user with current timestamps")
    void createActiveUserWithLoginIdentifierAndRole() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        assertEquals("dev1", user.getLoginId());
        assertEquals("Dev One", user.getName());
        assertEquals("hash", user.getPasswordHash());
        assertEquals(Role.DEV, user.getRole());
        assertTrue(user.isActive());
        assertTrue(user.hasRole(Role.DEV));
        assertFalse(user.hasRole(Role.TESTER));
        assertEquals(NOW, user.getCreatedAt());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    @DisplayName("fromPersistence restores stored lifecycle fields")
    void restoreUserFromPersistence() {
        LocalDateTime createdAt = NOW.minusDays(1);

        User user = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, false, createdAt, NOW);

        assertFalse(user.isActive());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    @DisplayName("required user fields cannot be blank")
    void rejectBlankUserFields() {
        assertThrows(IllegalArgumentException.class, () -> User.create("", "Dev One", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", " ", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", "Dev One", "", Role.DEV, NOW));
    }

    @Test
    @DisplayName("role is required")
    void rejectMissingRole() {
        assertThrows(NullPointerException.class, () -> User.create("dev1", "Dev One", "hash", null, NOW));
    }

    @Test
    @DisplayName("user can be deactivated")
    void deactivateUser() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        user.deactivate(NOW.plusMinutes(1));

        assertFalse(user.isActive());
        assertEquals(NOW.plusMinutes(1), user.getUpdatedAt());
    }
}
