package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Password hasher")
class PasswordHasherTest {

    @Test
    @DisplayName("stores passwords as salted PBKDF2 credentials")
    void storesPasswordsAsSaltedPbkdf2Credentials() {
        PasswordHasher hasher = new PasswordHasher();

        String credential = hasher.hash("DemoLocalAdmin!");

        assertTrue(hasher.isHashed(credential));
        assertEquals(32, hasher.saltOf(credential).length());
        assertEquals(64, hasher.hashOf(credential).length());
        assertTrue(hasher.matches("DemoLocalAdmin!", credential));
        assertFalse(hasher.matches("wrong-password", credential));
    }

    @Test
    @DisplayName("rejects plain stored credentials")
    void rejectsPlainStoredCredentials() {
        PasswordHasher hasher = new PasswordHasher();

        assertFalse(hasher.isHashed("DemoLocalAdmin!"));
        assertFalse(hasher.matches("DemoLocalAdmin!", "DemoLocalAdmin!"));
        assertFalse(hasher.matches("wrong-password", "DemoLocalAdmin!"));
    }
}
