package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@DisplayName("Oracle live data read-only smoke")
@EnabledIfEnvironmentVariable(named = "ITS_DB_LIVE_DATA_TESTS", matches = "true")
class OracleLiveDataReadOnlyTest {

    private static final List<String> CORE_TABLES = List.of(
            "users",
            "user_credentials",
            "projects",
            "project_members",
            "issues",
            "comments",
            "issue_history",
            "issue_dependencies"
    );

    private static DriverManagerConnectionProvider connectionProvider;
    private static JdbcRepositoryFactory repositories;

    private Map<String, Long> rowCountsBeforeTest;

    @BeforeAll
    static void connectToLiveDatabase() throws SQLException {
        connectionProvider = DriverManagerConnectionProvider.fromEnvironment();
        repositories = new JdbcRepositoryFactory(connectionProvider, new PasswordHasher());
        assertCoreTablesAreReadable();
    }

    @BeforeEach
    void captureRowCounts() throws SQLException {
        rowCountsBeforeTest = currentRowCounts();
    }

    @AfterEach
    void verifyReadOnlyTestDidNotMutateDatabase() throws SQLException {
        assertEquals(rowCountsBeforeTest, currentRowCounts());
    }

    @Test
    @DisplayName("current database contains readable users and projects")
    void currentDatabaseContainsReadableUsersAndProjects() {
        var users = repositories.users().findAll();
        var projects = repositories.dashboardSummaries().findAllProjectSummaries();

        assertFalse(users.isEmpty(), "live database should contain at least one user");
        assertFalse(projects.isEmpty(), "live database should contain at least one project");
        assertTrue(users.stream().anyMatch(user -> user.isActive()), "live database should contain an active user");
        assertTrue(projects.stream().allMatch(project -> project.projectId() > 0L));
    }

    @Test
    @DisplayName("current projects expose members and issues")
    void currentProjectsExposeMembersAndIssues() {
        List<DashboardProjectSnapshot> projects = repositories.dashboardSummaries().findAllProjectSummaries();
        long totalMembers = 0L;
        long totalIssues = 0L;

        for (DashboardProjectSnapshot project : projects) {
            var participants = repositories.projects().findParticipants(project.projectId());
            var issues = findAllIssues(project.projectId());
            assertNotNull(participants);
            assertNotNull(issues);
            totalMembers += participants.size();
            totalIssues += issues.size();
        }

        assertTrue(totalMembers > 0L, "live database should contain project membership rows");
        assertTrue(totalIssues > 0L, "live database should contain issue rows");
    }

    @Test
    @DisplayName("current issue associations are readable")
    void currentIssueAssociationsAreReadable() {
        List<Issue> issues = repositories.dashboardSummaries().findAllProjectSummaries().stream()
                .flatMap(project -> findAllIssues(project.projectId()).stream())
                .toList();

        assertFalse(issues.isEmpty(), "live database should contain issues");

        boolean hasHistory = false;
        for (Issue issue : issues) {
            var comments = repositories.comments().findByIssueId(issue.id());
            var histories = repositories.issueHistory().findByIssueId(issue.id());
            var dependencies = repositories.issueDependencies().findByIssueId(issue.id());

            assertTrue(comments.stream().allMatch(comment -> comment.issueId() == issue.id()));
            assertTrue(histories.stream().allMatch(history -> history.issueId() == issue.id()));
            assertTrue(dependencies.stream().allMatch(dependency ->
                    dependency.blockingIssueId() == issue.id() || dependency.blockedIssueId() == issue.id()));
            hasHistory = hasHistory || !histories.isEmpty();
        }

        assertTrue(hasHistory, "live database should contain issue history rows");
    }

    @Test
    @DisplayName("current project statistics are readable")
    void currentProjectStatisticsAreReadable() {
        for (DashboardProjectSnapshot project : repositories.dashboardSummaries().findAllProjectSummaries()) {
            var report = repositories.statistics().calculateProjectStatistics(
                    project.projectId(),
                    null,
                    null,
                    null,
                    null
            );

            assertNotNull(report.statusCounts());
            assertNotNull(report.priorityCounts());
            assertNotNull(report.dailyCounts());
            assertNotNull(report.monthlyCounts());
        }
    }

    private static List<Issue> findAllIssues(long projectId) {
        return repositories.issues().findByCriteria(IssueSearchCriteria.create(
                projectId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
        ));
    }

    private static void assertCoreTablesAreReadable() throws SQLException {
        currentRowCounts();
    }

    private static Map<String, Long> currentRowCounts() throws SQLException {
        Map<String, Long> rowCounts = new LinkedHashMap<>();
        for (String table : CORE_TABLES) {
            rowCounts.put(table, countRows(table));
        }
        return rowCounts;
    }

    private static long countRows(String tableName) throws SQLException {
        String sql = "select count(*) from " + tableName;
        try (var connection = connectionProvider.getConnection();
                var statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
