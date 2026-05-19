package com.github.marcellokim.issuetracker;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.technical.ConsoleOutput;
import com.github.marcellokim.issuetracker.ui.IssueTrackerApplication;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        printLine("Issue Tracker application started.");

        if (isLoginCheck(args)) {
            runLoginCheck(args);
            return;
        }

        if (!isCliDemo(args)) {
            IssueTrackerApplication.launchApp(args);
            return;
        }

        if (!hasDatabaseEnvironment()) {
            printDatabaseSetupGuide();
            return;
        }

        try {
            var connectionProvider = DriverManagerConnectionProvider.fromEnvironment();
            DatabaseInitializer.initialize(connectionProvider);
            printRepositorySummary(new JdbcRepositoryFactory(connectionProvider));
        } catch (IOException | SQLException | RuntimeException exception) {
            printLine("Oracle repository demo failed.");
            printLine("Cause: " + exception.getMessage());
            printLine("Check that Oracle XE is running and ITS_DB_URL points to XEPDB1.");
        }
    }

    private static boolean isCliDemo(String[] args) {
        return Arrays.asList(args).contains("--cli-demo");
    }

    private static boolean isLoginCheck(String[] args) {
        return args.length > 0 && "--login-check".equals(args[0]);
    }

    private static boolean hasDatabaseEnvironment() {
        return hasText(System.getenv("ITS_DB_URL"))
                && hasText(System.getenv("ITS_DB_USER"))
                && hasText(System.getenv("ITS_DB_PASSWORD"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void printDatabaseSetupGuide() {
        printLine("Oracle repository demo skipped.");
        printLine("Set database environment variables to print DB/repository status:");
        printLine("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
        printLine("  $env:ITS_DB_USER=\"ITS_USER\"");
        printLine("  $env:ITS_DB_PASSWORD=\"your_password\"");
        printLine("Then run:");
        printLine("  .\\gradlew.bat run --args=\"--cli-demo\"");
    }

    private static void runLoginCheck(String[] args) {
        if (args.length < 3) {
            printLine("Login check skipped.");
            printLine("Usage:");
            printLine("  .\\gradlew.bat run --args=\"--login-check <loginId> <password>\"");
            return;
        }

        if (!hasDatabaseEnvironment()) {
            printDatabaseSetupGuide();
            return;
        }

        String loginId = args[1];
        String password = args[2];
        String normalizedLoginId = loginId.trim();

        try {
            var connectionProvider = DriverManagerConnectionProvider.fromEnvironment();
            DatabaseInitializer.initialize(connectionProvider);
            printConnectionContext(connectionProvider);

            var repositories = new JdbcRepositoryFactory(connectionProvider);
            var user = repositories.users().findByLoginId(normalizedLoginId);
            var result = new AuthenticationService(repositories.users()).login(normalizedLoginId, password);

            printLine("Login ID: " + normalizedLoginId);
            user.ifPresentOrElse(
                    value -> {
                        printLine("Account: found");
                        printLine("Role: " + value.role());
                        printLine("Active: " + value.active());
                    },
                    () -> printLine("Account: missing")
            );
            printLine("Login result: " + (result.success() ? "SUCCESS" : "FAILURE"));
            printLine("Message: " + result.message());
        } catch (IOException | SQLException | RuntimeException exception) {
            printLine("Login check failed.");
            printLine("Cause: " + exception.getMessage());
        }
    }

    private static void printConnectionContext(DriverManagerConnectionProvider connectionProvider) throws SQLException {
        printLine("DB URL: " + System.getenv().getOrDefault("ITS_DB_URL", ""));
        printLine("DB user: " + System.getenv().getOrDefault("ITS_DB_USER", ""));

        String sql = """
                select sys_context('USERENV', 'CURRENT_SCHEMA') as current_schema,
                       sys_context('USERENV', 'CON_NAME') as container_name
                from dual
                """;
        try (var connection = connectionProvider.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                printLine("Current schema: " + resultSet.getString("current_schema"));
                printLine("Oracle container: " + resultSet.getString("container_name"));
            }
        }
    }

    private static void printRepositorySummary(JdbcRepositoryFactory repositories) {
        var admin = repositories.users().findByLoginId("admin");
        var project = repositories.projects().findByName("project1");

        printLine("Oracle repository demo ready.");
        admin.ifPresentOrElse(
                user -> printLine("Admin: " + user.loginId() + " / " + user.role() + " / active=" + user.active()),
                () -> printLine("Admin: missing")
        );

        project.ifPresentOrElse(
                value -> printProjectSummary(repositories, value.id(), value.name()),
                () -> printLine("Project: project1 missing")
        );
    }

    private static void printProjectSummary(JdbcRepositoryFactory repositories, long projectId, String projectName) {
        var issues = repositories.issues().findByProject(projectId);
        var statusCounts = repositories.statistics().countByStatus(projectId);
        var priorityCounts = repositories.statistics().countByPriority(projectId);
        var devCandidates = repositories.assignmentRecommendations().findDevAssigneeCandidates(projectId);
        var testerCandidates = repositories.assignmentRecommendations().findTesterVerifierCandidates(projectId);

        printLine("Project: " + projectName);
        printLine("Members: " + repositories.projects().findParticipants(projectId).size());
        printLine("Active devs: " + repositories.users().findActiveByRole(projectId, Role.DEV).size());
        printLine("Active testers: " + repositories.users().findActiveByRole(projectId, Role.TESTER).size());
        printLine("Issues: " + issues.size());
        printLine("Status counts: " + statusCounts);
        printLine("Priority counts: " + priorityCounts);
        printLine("Dev recommendation candidates: " + devCandidates.size());
        printLine("Tester recommendation candidates: " + testerCandidates.size());
    }

    private static void printLine(String message) {
        ConsoleOutput.out(message);
    }
}
