package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Session store")
class SessionStoreTest {

    @Test
    @DisplayName("starts, reads, and clears a user session")
    void startsReadsAndClearsSession() {
        SessionStore store = new SessionStore();
        User user = User.create("dev1", "Developer One", "hash", Role.DEV, true, null, null);

        assertFalse(store.currentUser().isPresent());

        store.startSession(user);

        assertTrue(store.currentUser().isPresent());
        assertEquals("dev1", store.currentUser().orElseThrow().getLoginId());

        store.clear();

        assertFalse(store.currentUser().isPresent());
    }

    @Test
    @DisplayName("rejects null session user")
    void rejectsNullSessionUser() {
        SessionStore store = new SessionStore();

        assertThrows(NullPointerException.class, () -> store.startSession(null));
    }
}
