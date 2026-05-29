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
    @DisplayName("starts, reads, and clears a user session")
    void startsReadsAndClearsSession() {
        SessionStore store = new SessionStore();

        assertFalse(store.currentLoginId().isPresent());

        store.start("dev1");

        assertTrue(store.currentLoginId().isPresent());
        assertEquals("dev1", store.currentLoginId().orElseThrow());

        store.clear();

        assertFalse(store.currentLoginId().isPresent());
    }

    @Test
    @DisplayName("rejects blank session login id")
    void rejectsBlankSessionLoginId() {
        SessionStore store = new SessionStore();

        assertThrows(IllegalArgumentException.class, () -> store.start(null));
        assertThrows(IllegalArgumentException.class, () -> store.start(" "));
    }
}
