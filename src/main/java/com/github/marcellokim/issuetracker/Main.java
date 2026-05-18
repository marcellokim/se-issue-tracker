package com.github.marcellokim.issuetracker;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.ui.IssueTrackerApplication;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Issue Tracker application started.");

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
            System.out.println("Oracle repository demo failed.");
            System.out.println("Cause: " + exception.getMessage());
            System.out.println("Check that Oracle XE is running and ITS_DB_URL points to XEPDB1.");
        }
    }

    private static boolean isCliDemo(String[] args) {
        return Arrays.asList(args).contains("--cli-demo");
    }

    private static boolean isLoginCheck(String[] args) {
        return args.length > 0 && "--login-check".equals(args[0]);
    }

    private static boolean hasDatabaseEnvironment() {
        return hasText(System.getenv("ITS_DB_USER")) && hasText(System.getenv("ITS_DB_PASSWORD"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void printDatabaseSetupGuide() {
        System.out.println("Oracle repository demo skipped.");
        System.out.println("Set database environment variables to print DB/repository status:");
        System.out.println("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
        System.out.println("  $env:ITS_DB_USER=\"ITS_USER\"");
        System.out.println("  $env:ITS_DB_PASSWORD=\"your_password\"");
        System.out.println("Then run:");
        System.out.println("  .\\gradlew.bat run --args=\"--cli-demo\"");
    }

    private static void runLoginCheck(String[] args) {
        if (args.length < 3) {
            System.out.println("Login check skipped.");
            System.out.println("Usage:");
            System.out.println("  .\\gradlew.bat run --args=\"--login-check dev8 DemoLocalDev8!\"");
            return;
        }

        if (!hasDatabaseEnvironment()) {
            printDatabaseSetupGuide();
            return;
        }

        String loginId = args[1];
        String password = args[2];

        try {
            var connectionProvider = DriverManagerConnectionProvider.fromEnvironment();
            DatabaseInitializer.initialize(connectionProvider);
            printConnectionContext(connectionProvider);

            var repositories = new JdbcRepositoryFactory(connectionProvider);
            var user = repositories.users().findByLoginId(loginId.trim());
            var result = new AuthenticationService(repositories.users()).login(loginId, password);

            System.out.println("Login ID: " + loginId.trim());
            user.ifPresentOrElse(
                    value -> {
                        System.out.println("Account: found");
                        System.out.println("Role: " + value.role());
                        System.out.println("Active: " + value.active());
                        System.out.println("Stored password length: " + value.password().length());
                    },
                    () -> System.out.println("Account: missing")
            );
            System.out.println("Login result: " + (result.success() ? "SUCCESS" : "FAILURE"));
            System.out.println("Message: " + result.message());
        } catch (IOException | SQLException | RuntimeException exception) {
            System.out.println("Login check failed.");
            System.out.println("Cause: " + exception.getMessage());
        }
    }

    private static void printConnectionContext(DriverManagerConnectionProvider connectionProvider) throws SQLException {
        System.out.println("DB URL: " + System.getenv().getOrDefault("ITS_DB_URL", ""));
        System.out.println("DB user: " + System.getenv().getOrDefault("ITS_DB_USER", ""));

        String sql = """
                select sys_context('USERENV', 'CURRENT_SCHEMA') as current_schema,
                       sys_context('USERENV', 'CON_NAME') as container_name
                from dual
                """;
        try (var connection = connectionProvider.getConnection();
             var statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                System.out.println("Current schema: " + resultSet.getString("current_schema"));
                System.out.println("Oracle container: " + resultSet.getString("container_name"));
            }
        }
    }

    private static void printRepositorySummary(JdbcRepositoryFactory repositories) {
        var admin = repositories.users().findByLoginId("admin");
        var project = repositories.projects().findByName("project1");

        System.out.println("Oracle repository demo ready.");
        admin.ifPresentOrElse(
                user -> System.out.println("Admin: " + user.loginId() + " / " + user.role() + " / active=" + user.active()),
                () -> System.out.println("Admin: missing")
        );

        project.ifPresentOrElse(
                value -> printProjectSummary(repositories, value.id(), value.name()),
                () -> System.out.println("Project: project1 missing")
        );
    }

    private static void printProjectSummary(JdbcRepositoryFactory repositories, long projectId, String projectName) {
        var issues = repositories.issues().findByProject(projectId);
        var statusCounts = repositories.statistics().countByStatus(projectId);
        var priorityCounts = repositories.statistics().countByPriority(projectId);
        var devCandidates = repositories.assignmentRecommendations().findDevAssigneeCandidates(projectId);
        var testerCandidates = repositories.assignmentRecommendations().findTesterVerifierCandidates(projectId);

        System.out.println("Project: " + projectName);
        System.out.println("Members: " + repositories.projects().findParticipants(projectId).size());
        System.out.println("Active devs: " + repositories.users().findActiveByRole(projectId, Role.DEV).size());
        System.out.println("Active testers: " + repositories.users().findActiveByRole(projectId, Role.TESTER).size());
        System.out.println("Issues: " + issues.size());
        System.out.println("Status counts: " + statusCounts);
        System.out.println("Priority counts: " + priorityCounts);
        System.out.println("Dev recommendation candidates: " + devCandidates.size());
        System.out.println("Tester recommendation candidates: " + testerCandidates.size());
    }
}
