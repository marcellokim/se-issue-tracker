package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.service.PasswordHashing;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher implements PasswordHashing {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 120_000;
    private static final String HASH_SEPARATOR = ":";
    private static final String HEX_16_BYTES = "[0-9a-fA-F]{32}";
    private static final String HEX_32_BYTES = "[0-9a-fA-F]{64}";

    @Override
    public String hash(String password) {
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);
        return salt + HASH_SEPARATOR + derive(salt, requireText(password, "password"));
    }

    @Override
    public boolean matches(String password, String storedCredential) {
        if (password == null || storedCredential == null) {
            return false;
        }

        if (!isHashed(storedCredential)) {
            return false;
        }

        String[] parts = storedCredential.split(HASH_SEPARATOR, 2);
        byte[] expected = parts[1].getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actual = derive(parts[0], password).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    @Override
    public boolean isHashed(String storedCredential) {
        if (storedCredential == null) {
            return false;
        }
        String[] parts = storedCredential.split(HASH_SEPARATOR, 2);
        return parts.length == 2
                && parts[0].matches(HEX_16_BYTES)
                && parts[1].matches(HEX_32_BYTES);
    }

    @Override
    public String saltOf(String storedCredential) {
        requireHashed(storedCredential);
        return storedCredential.split(HASH_SEPARATOR, 2)[0].toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String hashOf(String storedCredential) {
        requireHashed(storedCredential);
        return storedCredential.split(HASH_SEPARATOR, 2)[1].toLowerCase(java.util.Locale.ROOT);
    }

    private static void requireHashed(String storedCredential) {
        String[] parts = storedCredential == null ? new String[0] : storedCredential.split(HASH_SEPARATOR, 2);
        if (parts.length != 2 || !parts[0].matches(HEX_16_BYTES) || !parts[1].matches(HEX_32_BYTES)) {
            throw new IllegalArgumentException("storedCredential must be pbkdf2-salt:pbkdf2-hash");
        }
    }

    private static String derive(String salt, String password) {
        try {
            var keySpec = new PBEKeySpec(
                    password.toCharArray(),
                    HexFormat.of().parseHex(salt),
                    ITERATIONS,
                    HASH_BYTES * 8);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            return HexFormat.of().formatHex(key);
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("PBKDF2 password hashing is unavailable.", exception);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (Objects.requireNonNull(value, fieldName).isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}