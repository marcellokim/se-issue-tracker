package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Database environment")
class DatabaseEnvironmentTest {

    @Test
    @DisplayName("requires URL, user, and password for application database setup")
    void checksDatabaseEnvironmentReadiness() {
        assertFalse(DatabaseEnvironment.isConfigured(Map.of()));
        assertFalse(DatabaseEnvironment.isConfigured(Map.of(
                "ITS_DB_URL", "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                "ITS_DB_USER", "ITS_USER",
                "ITS_DB_PASSWORD", " "
        )));
        assertTrue(DatabaseEnvironment.isConfigured(databaseEnvironment()));
    }

    @Test
    @DisplayName("creates immutable connection settings from configured values")
    void createsDatabaseEnvironmentFromConfiguredValues() {
        DatabaseEnvironment environment = DatabaseEnvironment.requireConfigured(databaseEnvironment());

        assertEquals("jdbc:oracle:thin:@//localhost:1521/XEPDB1", environment.url());
        assertEquals("ITS_USER", environment.user());
        assertEquals("secret", environment.password());
    }

    @Test
    @DisplayName("uses one clear missing-environment message")
    void rejectsMissingDatabaseEnvironment() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> DatabaseEnvironment.requireConfigured(Map.of())
        );

        assertEquals(
                "Oracle environment is missing. Set ITS_DB_URL, ITS_DB_USER, ITS_DB_PASSWORD.",
                exception.getMessage());
    }

    private static Map<String, String> databaseEnvironment() {
        return Map.of(
                "ITS_DB_URL", "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                "ITS_DB_USER", "ITS_USER",
                "ITS_DB_PASSWORD", "secret"
        );
    }
}
