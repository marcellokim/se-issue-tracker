package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User")
class UserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("new users are active")
    void createsActiveUser() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        assertEquals("dev1", user.getLoginId());
        assertEquals("Dev One", user.getName());
        assertEquals("hash", user.getPasswordHash());
        assertEquals(Role.DEV, user.getRole());
        assertTrue(user.isActive());
        assertEquals(NOW, user.getCreatedAt());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    @DisplayName("stored users keep their saved state")
    void restoresSavedState() {
        LocalDateTime createdAt = NOW.minusDays(1);

        User user = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, false, createdAt, NOW);

        assertFalse(user.isActive());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(NOW, user.getUpdatedAt());
    }

    @Test
    @DisplayName("missing user info is rejected")
    void rejectsMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> User.create(null, "Dev One", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("", "Dev One", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create(" ", "Dev One", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", null, "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", " ", "hash", Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", "Dev One", null, Role.DEV, NOW));
        assertThrows(IllegalArgumentException.class, () -> User.create("dev1", "Dev One", "", Role.DEV, NOW));
        assertThrows(NullPointerException.class, () -> User.create("dev1", "Dev One", "hash", null, NOW));
    }

    @Test
    @DisplayName("login id is trimmed")
    void trimsLoginId() {
        User created = User.create(" dev1", "Dev One", "hash", Role.DEV, NOW);
        User restored = User.fromPersistence(" tester1 ", "Tester One", "hash", Role.TESTER, false, NOW, NOW);

        assertEquals("dev1", created.getLoginId());
        assertEquals("tester1", restored.getLoginId());
    }

    @Test
    @DisplayName("user changes require a timestamp")
    void rejectsMissingChangeTime() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        assertThrows(NullPointerException.class, () -> User.create("dev2", "Dev Two", "hash", Role.DEV, null));
        assertThrows(NullPointerException.class, () -> user.rename("Dev Updated", null));
    }

    @Test
    @DisplayName("user can be deactivated")
    void deactivatesUser() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        user.deactivate(NOW.plusMinutes(1));

        assertFalse(user.isActive());
        assertEquals(NOW.plusMinutes(1), user.getUpdatedAt());
    }

    @Test
    @DisplayName("user can be reactivated")
    void reactivatesUser() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);
        user.deactivate(NOW.plusMinutes(1));

        user.activate(NOW.plusMinutes(2));

        assertTrue(user.isActive());
        assertEquals(NOW.plusMinutes(2), user.getUpdatedAt());
    }

    @Test
    @DisplayName("rename changes name and timestamp")
    void renamesUser() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        user.rename("Dev Updated", NOW.plusMinutes(1));

        assertEquals("Dev Updated", user.getName());
        assertEquals(NOW.plusMinutes(1), user.getUpdatedAt());
    }

    @Test
    @DisplayName("renaming blank names is rejected")
    void rejectsBlankName() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        assertThrows(IllegalArgumentException.class, () -> user.rename("", NOW.plusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> user.rename(" ", NOW.plusMinutes(1)));
    }

    @Test
    @DisplayName("role change updates role and timestamp")
    void changesRole() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        user.changeRole(Role.PL, NOW.plusMinutes(1));

        assertEquals(Role.PL, user.getRole());
        assertTrue(user.hasRole(Role.PL));
        assertFalse(user.hasRole(Role.DEV));
        assertEquals(NOW.plusMinutes(1), user.getUpdatedAt());
    }

    @Test
    @DisplayName("changing to null role is rejected")
    void rejectsNullRole() {
        User user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

        assertThrows(NullPointerException.class, () -> user.changeRole(null, NOW.plusMinutes(1)));
    }
}
