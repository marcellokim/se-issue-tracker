package com.github.marcellokim.issuetracker.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.config.ApplicationRuntime;
import com.github.marcellokim.issuetracker.config.DatabaseConnectionSummary;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.LoginCheckService;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummary;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummaryService;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CLI commands")
class CliCommandTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 20, 10);
    private static final String PASSWORD = "DemoLocalAdmin!";
    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();

    @Test
    @DisplayName("repository demo without database prints setup guide")
    void repositoryDemoWithoutDatabasePrintsSetupGuide() {
        RecordingOutput output = new RecordingOutput();

        new RepositoryDemoCommand(FakeRuntime.withoutDatabase(), output).run();

        assertTrue(output.text().contains("Oracle repository demo skipped."));
        assertTrue(output.text().contains("ITS_DB_URL"));
        assertTrue(output.text().contains("--cli-demo"));
    }

    @Test
    @DisplayName("repository demo prints summary from service when database is configured")
    void repositoryDemoPrintsServiceSummary() {
        RecordingOutput output = new RecordingOutput();
        RepositoryDemoSummary summary = new RepositoryDemoSummary(
                Optional.of(new RepositoryDemoSummary.AdminAccount("admin", Role.ADMIN, true)),
                Optional.of(new RepositoryDemoSummary.ProjectSummary(
                        "Project A",
                        3,
                        1,
                        1,
                        2,
                        Map.of(IssueStatus.CLOSED, 2, IssueStatus.NEW, 1, IssueStatus.ASSIGNED, 3),
                        Map.of(Priority.TRIVIAL, 4, Priority.BLOCKER, 1, Priority.MAJOR, 2),
                        1,
                        1)));

        new RepositoryDemoCommand(FakeRuntime.configured(), output, () -> summary).run();

        assertTrue(output.text().contains("Oracle repository demo ready."));
        assertTrue(output.text().contains("Admin: admin / ADMIN / active=true"));
        assertTrue(output.text().contains("Project: Project A"));
        assertTrue(output.text().contains("Status counts: {NEW=1, ASSIGNED=3, CLOSED=2}"));
        assertTrue(output.text().contains("Priority counts: {BLOCKER=1, MAJOR=2, TRIVIAL=4}"));
        assertTrue(output.text().contains("Tester recommendation candidates: 1"));
    }

    @Test
    @DisplayName("repository demo prints failure context when runtime setup fails")
    void repositoryDemoPrintsFailureContext() {
        RecordingOutput output = new RecordingOutput();

        new RepositoryDemoCommand(FakeRuntime.configured(), output, () -> {
            throw new IOException("boom");
        }).run();

        assertTrue(output.text().contains("Oracle repository demo failed."));
        assertTrue(output.text().contains("Cause: boom"));
        assertTrue(output.text().contains("Check that Oracle XE is running"));
    }

    @Test
    @DisplayName("login check without credentials prints usage")
    void loginCheckWithoutCredentialsPrintsUsage() {
        RecordingOutput output = new RecordingOutput();

        new LoginCheckCommand(FakeRuntime.withoutDatabase(), output).run(new String[] {"--login-check", "admin"});

        assertTrue(output.text().contains("Login check skipped."));
        assertTrue(output.text().contains("--login-check <loginId> <password>"));
    }

    @Test
    @DisplayName("login check without database delegates to repository setup guide")
    void loginCheckWithoutDatabasePrintsSetupGuide() {
        RecordingOutput output = new RecordingOutput();

        new LoginCheckCommand(FakeRuntime.withoutDatabase(), output)
                .run(new String[] {"--login-check", "admin", PASSWORD});

        assertTrue(output.text().contains("Oracle repository demo skipped."));
        assertTrue(output.text().contains("Set database environment variables"));
    }

    @Test
    @DisplayName("login check prints connection context and authentication result")
    void loginCheckPrintsAuthenticationResult() {
        RecordingOutput output = new RecordingOutput();

        new LoginCheckCommand(FakeRuntime.withLoginUser(user("admin", Role.ADMIN, true)), output)
                .run(new String[] {"--login-check", " admin ", PASSWORD});

        assertTrue(output.text().contains("DB URL: jdbc:oracle:thin:@//localhost:1521/XEPDB1"));
        assertTrue(output.text().contains("DB user: ITS_USER"));
        assertTrue(output.text().contains("Account: found"));
        assertTrue(output.text().contains("Role: ADMIN"));
        assertTrue(output.text().contains("Login result: SUCCESS"));
    }

    @Test
    @DisplayName("login check prints failure context when runtime setup fails")
    void loginCheckPrintsRuntimeFailureContext() {
        RecordingOutput output = new RecordingOutput();

        new LoginCheckCommand(FakeRuntime.failing(), output)
                .run(new String[] {"--login-check", "admin", PASSWORD});

        assertTrue(output.text().contains("Login check failed."));
        assertTrue(output.text().contains("Cause: boom"));
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, PASSWORD_HASHER.hash(PASSWORD), role, active, NOW, NOW);
    }

    private static final class RecordingOutput implements CliOutput {

        private final StringBuilder text = new StringBuilder();

        @Override
        public void println(String message) {
            text.append(message).append(System.lineSeparator());
        }

        private String text() {
            return text.toString();
        }
    }

    private static final class FakeRuntime implements ApplicationRuntime {

        private final boolean configured;
        private final User loginUser;
        private final boolean fail;

        private FakeRuntime(
                boolean configured,
                User loginUser,
                boolean fail
        ) {
            this.configured = configured;
            this.loginUser = loginUser;
            this.fail = fail;
        }

        private static FakeRuntime withoutDatabase() {
            return new FakeRuntime(false, null, false);
        }

        private static FakeRuntime configured() {
            return new FakeRuntime(true, null, false);
        }

        private static FakeRuntime withLoginUser(User user) {
            return new FakeRuntime(true, user, false);
        }

        private static FakeRuntime failing() {
            return new FakeRuntime(true, null, true);
        }

        @Override
        public boolean hasDatabaseEnvironment() {
            return configured;
        }

        @Override
        public RepositoryDemoSummaryService repositoryDemoSummaryService() throws IOException {
            if (fail) {
                throw new IOException("boom");
            }
            throw new UnsupportedOperationException("repository demo summary is injected directly in this test.");
        }

        @Override
        public LoginCheckService loginCheckService() throws IOException {
            if (fail) {
                throw new IOException("boom");
            }
            return new LoginCheckService(new InMemoryUserRepository(loginUser));
        }

        @Override
        public DatabaseConnectionSummary databaseConnectionSummary() throws SQLException {
            if (fail) {
                throw new SQLException("boom");
            }
            return new DatabaseConnectionSummary(
                    "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                    "ITS_USER",
                    "ITS_USER",
                    "XEPDB1");
        }
    }
}
