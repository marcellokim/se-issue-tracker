package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DB initialization resources")
class PersistenceResourceSmokeTest {

    @Test
    @DisplayName("Oracle schema and seed SQL are packaged as application resources")
    void oracleSchemaAndSeedScriptsArePackaged() {
        assertNotNull(resource("db/oracle/schema-oracle.sql"));
        assertNotNull(resource("db/oracle/seed-oracle.sql"));
    }

    @Test
    @DisplayName("Issue schema does not add deletedAt or preDeleteStatus")
    void issueSchemaDoesNotContainDeletedAtOrPreDeleteStatus() throws IOException {
        String schema = readResource("db/oracle/schema-oracle.sql").toLowerCase();

        assertFalse(schema.contains("deleted_at"));
        assertFalse(schema.contains("pre_delete_status"));
    }

    @Test
    @DisplayName("Users schema uses login id and separate credential hashes")
    void usersSchemaUsesLoginIdPrimaryKeyAndCredentialHashes() throws IOException {
        String schema = readResource("db/oracle/schema-oracle.sql").toLowerCase();
        String seed = readResource("db/oracle/seed-oracle.sql").toLowerCase();
        int usersTableStart = schema.indexOf("create table users");
        String usersTable = schema.substring(
                usersTableStart,
                schema.indexOf("create table user_credentials", usersTableStart));

        assertTrue(schema.contains("login_id varchar2(50) primary key"));
        assertTrue(schema.contains("name varchar2(100) not null"));
        assertTrue(schema.contains("create table user_credentials"));
        assertTrue(schema.contains("password_salt varchar2(64) not null"));
        assertTrue(schema.contains("password_hash varchar2(64) not null"));
        assertTrue(schema.contains("reporter_login_id"));
        assertTrue(schema.contains("changed_by_login_id"));
        assertTrue(schema.contains("writer_login_id"));
        assertTrue(seed.contains("'admin' as login_id"));
        assertTrue(seed.contains("'admin' as name"));
        assertFalse(usersTable.contains("password varchar2"));
        assertFalse(seed.contains("demolocaladmin!"));
        assertTrue(seed.contains("merge into user_credentials"));
        assertTrue(seed.contains("'4eefdf0a692b0a9f55b0a25aa92ddd3c' as password_salt"));
        assertTrue(
                seed.contains("'e0029239253cae8b9f8851e1e6a59a0c6b2d8692af7d7a3843da2ca4665da673' as password_hash"));
        assertFalse(usersTable.contains("password_hash"));
        assertFalse(seed.contains("target.password = source.password"));
    }

    @Test
    @DisplayName("Seed SQL uses repeatable merge flow")
    void seedUsesMergeForRepeatability() throws IOException {
        String seed = readResource("db/oracle/seed-oracle.sql").toLowerCase();

        assertTrue(seed.contains("merge into users"));
        assertTrue(seed.contains("merge into projects"));
        assertTrue(seed.contains("merge into issues"));
        assertTrue(seed.contains("merge into issue_history"));
    }

    @Test
    @DisplayName("Comments schema upgrades existing tables with purpose column")
    void commentsSchemaUpgradesExistingPurposeColumn() throws IOException {
        String schema = readResource("db/oracle/schema-oracle.sql").toLowerCase();

        assertTrue(schema.contains("alter table comments add purpose varchar2(32) default 'general' not null"));
        assertTrue(schema.contains("chk_comments_purpose"));
        assertTrue(schema.contains("'status_change_reason'"));
    }

    @Test
    @DisplayName("Seed SQL includes required demo accounts and status history samples")
    void seedIncludesRequiredDemoAccountsAndHistorySamples() throws IOException {
        String schema = readResource("db/oracle/schema-oracle.sql").toLowerCase();
        String seed = readResource("db/oracle/seed-oracle.sql").toLowerCase();
        String compactSeed = seed.replaceAll("\\s+", "");

        assertTrue(seed.contains("'admin'"));
        assertTrue(seed.contains("'pl1'"));
        assertTrue(seed.contains("'pl2'"));
        assertTrue(seed.contains("'project2'"));
        assertTrue(seed.contains("'dev10'"));
        assertTrue(seed.contains("'tester5'"));
        assertTrue(seed.contains("delete from project_members target"));
        assertTrue(seed.contains("p.name in ( 'project1'"));
        assertTrue(seed.contains("'resolved'"));
        assertTrue(seed.contains("'closed'"));
        assertTrue(seed.contains("'reopened'"));
        assertTrue(seed.contains("'fixed'"));
        assertTrue(seed.contains("'assigned'"));
        assertTrue(seed.contains("'closed'"));
        assertTrue(seed.contains("'reopened'"));
        assertTrue(seed.contains("merge into comments"));
        assertTrue(seed.contains("merge into issue_dependencies"));
        assertTrue(schema.contains("dependency_id varchar2(128) not null"));
        assertTrue(compactSeed.contains(
                "standard_hash(to_char(blocking_issue.id)||':'||to_char(blocked_issue.id),'sha256')"));
    }

    private static java.net.URL resource(String path) {
        return PersistenceResourceSmokeTest.class.getClassLoader().getResource(path);
    }

    private static String readResource(String path) throws IOException {
        try (var inputStream = PersistenceResourceSmokeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(inputStream, () -> "Missing resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
