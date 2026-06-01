package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Session store")
class SessionStoreTest {

    @Test
    @DisplayName("session remembers the logged in user")
    void remembersLoggedInUser() {
        SessionStore store = new SessionStore();

        assertFalse(store.currentLoginId().isPresent());

        store.start("dev1");

        assertTrue(store.currentLoginId().isPresent());
        assertEquals("dev1", store.currentLoginId().orElseThrow());

        store.clear();

        assertFalse(store.currentLoginId().isPresent());
    }

    @Test
    @DisplayName("blank login id is not a session")
    void blankLoginIdIsNotASession() {
        SessionStore store = new SessionStore();

        assertThrows(IllegalArgumentException.class, () -> store.start(null));
        assertThrows(IllegalArgumentException.class, () -> store.start(" "));
    }
}
