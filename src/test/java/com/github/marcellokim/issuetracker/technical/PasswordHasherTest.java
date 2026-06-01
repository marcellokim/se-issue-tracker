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
    @DisplayName("passwords are stored as salted hashes")
    void storesSaltedHash() {
        PasswordHasher hasher = new PasswordHasher();

        String credential = hasher.hash("DemoLocalAdmin!");

        assertTrue(hasher.isHashed(credential));
        assertEquals(32, hasher.saltOf(credential).length());
        assertEquals(64, hasher.hashOf(credential).length());
        assertTrue(hasher.matches("DemoLocalAdmin!", credential));
        assertFalse(hasher.matches("wrong-password", credential));
    }

    @Test
    @DisplayName("plain text is not a stored password")
    void plainTextIsNotAStoredPassword() {
        PasswordHasher hasher = new PasswordHasher();

        assertFalse(hasher.isHashed("DemoLocalAdmin!"));
        assertFalse(hasher.matches("DemoLocalAdmin!", "DemoLocalAdmin!"));
        assertFalse(hasher.matches("wrong-password", "DemoLocalAdmin!"));
    }

    @Test
    @DisplayName("missing password input is handled safely")
    void missingPasswordIsHandledSafely() {
        PasswordHasher hasher = new PasswordHasher();
        String credential = hasher.hash("DemoLocalAdmin!");

        assertThrows(NullPointerException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(" "));
        assertFalse(hasher.matches(null, credential));
        assertFalse(hasher.matches("DemoLocalAdmin!", null));
    }

    @Test
    @DisplayName("broken stored hashes are not accepted")
    void brokenHashesAreNotAccepted() {
        PasswordHasher hasher = new PasswordHasher();

        assertFalse(hasher.isHashed(null));
        assertFalse(hasher.isHashed("abc:def"));
        assertFalse(hasher.isHashed("00112233445566778899aabbccddeeff:not-hex"));
        assertThrows(IllegalArgumentException.class, () -> hasher.saltOf("abc:def"));
        assertThrows(IllegalArgumentException.class, () -> hasher.hashOf("abc:def"));
    }

    @Test
    @DisplayName("hash parts are read in lowercase")
    void readsHashPartsInLowercase() {
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
