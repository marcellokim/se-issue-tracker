package com.github.marcellokim.issuetracker.technical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("rejects null and blank password inputs")
    void rejectsNullAndBlankPasswordInputs() {
        PasswordHasher hasher = new PasswordHasher();
        String credential = hasher.hash("DemoLocalAdmin!");

        assertThrows(NullPointerException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(" "));
        assertFalse(hasher.matches(null, credential));
        assertFalse(hasher.matches("DemoLocalAdmin!", null));
    }

    @Test
    @DisplayName("rejects malformed hashed credentials")
    void rejectsMalformedHashedCredentials() {
        PasswordHasher hasher = new PasswordHasher();

        assertFalse(hasher.isHashed(null));
        assertFalse(hasher.isHashed("abc:def"));
        assertFalse(hasher.isHashed("00112233445566778899aabbccddeeff:not-hex"));
        assertThrows(IllegalArgumentException.class, () -> hasher.saltOf("abc:def"));
        assertThrows(IllegalArgumentException.class, () -> hasher.hashOf("abc:def"));
    }

    @Test
    @DisplayName("normalizes credential parts to lowercase")
    void normalizesCredentialPartsToLowercase() {
        PasswordHasher hasher = new PasswordHasher();
        String uppercaseCredential = "00112233445566778899AABBCCDDEEFF:"
                + "00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF";

        assertTrue(hasher.isHashed(uppercaseCredential));
        assertEquals("00112233445566778899aabbccddeeff", hasher.saltOf(uppercaseCredential));
        assertEquals(
                "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff",
                hasher.hashOf(uppercaseCredential));
    }
}
