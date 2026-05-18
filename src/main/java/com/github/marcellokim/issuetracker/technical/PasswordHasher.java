package com.github.marcellokim.issuetracker.technical;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final String HASH_SEPARATOR = ":";

    public String hash(String password) {
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);
        return salt + HASH_SEPARATOR + digest(salt, requireText(password, "password"));
    }

    public boolean matches(String password, String storedCredential) {
        if (password == null || storedCredential == null) {
            return false;
        }

        if (!isHashed(storedCredential)) {
            byte[] expected = storedCredential.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] actual = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        }

        String[] parts = storedCredential.split(HASH_SEPARATOR, 2);
        byte[] expected = parts[1].getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actual = digest(parts[0], password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    public boolean isHashed(String storedCredential) {
        if (storedCredential == null) {
            return false;
        }
        String[] parts = storedCredential.split(HASH_SEPARATOR, 2);
        return parts.length == 2
                && !parts[0].isBlank()
                && parts[1].matches("[0-9a-fA-F]{64}");
    }

    public String saltOf(String storedCredential) {
        requireHashed(storedCredential);
        return storedCredential.split(HASH_SEPARATOR, 2)[0];
    }

    public String hashOf(String storedCredential) {
        requireHashed(storedCredential);
        return storedCredential.split(HASH_SEPARATOR, 2)[1].toLowerCase(java.util.Locale.ROOT);
    }

    private static void requireHashed(String storedCredential) {
        String[] parts = storedCredential == null ? new String[0] : storedCredential.split(HASH_SEPARATOR, 2);
        if (parts.length != 2 || parts[0].isBlank() || !parts[1].matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("storedCredential must be salt:sha256");
        }
    }

    private static String digest(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (salt + HASH_SEPARATOR + password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable.", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (Objects.requireNonNull(value, fieldName).isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
