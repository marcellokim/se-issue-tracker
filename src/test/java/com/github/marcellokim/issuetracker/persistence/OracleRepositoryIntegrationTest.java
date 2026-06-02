package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.repository.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@DisplayName("Oracle JDBC repository integration")
@EnabledIfEnvironmentVariable(named = "ITS_DB_INTEGRATION_TESTS", matches = "true")
class OracleRepositoryIntegrationTest {

        private static DriverManagerConnectionProvider connectionProvider;
        private static JdbcRepositoryFactory repositories;

        @BeforeAll
        static void initializeDatabase() throws SQLException, IOException {
                connectionProvider = DriverManagerConnectionProvider.fromIntegrationTestEnvironment();

                assertTestSchema();
                DatabaseInitializer.resetWithFixedSeed(connectionProvider);
                DatabaseInitializer.initializeApplication(connectionProvider);

                repositories = new JdbcRepositoryFactory(connectionProvider, new PasswordHasher());
        }

        private static void assertTestSchema() throws SQLException {
                String sql = "select sys_context('USERENV', 'CURRENT_SCHEMA') from dual";
                try (var connection = connectionProvider.getConnection();
                                var statement = connection.prepareStatement(sql);
                                var resultSet = statement.executeQuery()) {
                        resultSet.next();
                        String currentSchema = resultSet.getString(1);
                        if (currentSchema == null || !currentSchema.toUpperCase(Locale.ROOT).contains("TEST")) {
                                throw new IllegalStateException(
                                                "Oracle integration tests must run against a TEST schema. Current schema: "
                                                                + currentSchema);
                        }
                }
        }

        private static void purgeTestIssue(long issueId) {
                String sql = "delete from issues where id = ?";
                try (var connection = connectionProvider.getConnection();
                                var statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, issueId);
                        statement.executeUpdate();
                } catch (SQLException exception) {
                        throw new RepositoryException("Failed to purge test issue.", exception);
                }
        }

        // Repositories: UserRepository.findByLoginId, ProjectRepository.findByName
        @Test
        @DisplayName("seed data includes admin account and demo projects")
        void seedDataIncludesAdminAccountAndDemoProjects() {
                var admin = repositories.users().findByLoginId("admin").orElseThrow();
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();

                assertEquals(Role.ADMIN, admin.getRole());
                assertTrue(admin.isActive());
                assertEquals("Project A", project1.getName());
                assertEquals("Project B", project2.getName());
                assertEquals(admin.getLoginId(), project1.getManagedByLoginId());
                assertEquals(admin.getLoginId(), project2.getManagedByLoginId());
        }

        // Repositories: ProjectRepository.findParticipants,
        // UserRepository.existsActiveProjectMember,
        // AssignmentRecommendationRepository.findActiveDevCandidates/findActiveTesterCandidates
        @Test
        @DisplayName("project members and role candidates are queryable")
        void repositoryFindsProjectMembersAndRoleCandidates() {
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();

                assertEquals(10, repositories.projects().findParticipants(project1.getId()).size());
                assertTrue(repositories.users().existsActiveProjectMember(project1.getId(), "pl1"));
                assertEquals(5, repositories.assignmentRecommendations().findActiveDevCandidates(project1.getId())
                                .size());
                assertEquals(4, repositories.assignmentRecommendations().findActiveTesterCandidates(project1.getId())
                                .size());

                assertEquals(10, repositories.projects().findParticipants(project2.getId()).size());
                assertTrue(repositories.users().existsActiveProjectMember(project2.getId(), "pl2"));
                assertEquals(5, repositories.assignmentRecommendations().findActiveDevCandidates(project2.getId())
                                .size());
                assertEquals(4, repositories.assignmentRecommendations().findActiveTesterCandidates(project2.getId())
                                .size());
        }

        // Repositories: IssueRepository.findByCriteria,
        // CommentRepository.findByIssueId,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("issue, comment, history, and dependency associations are queryable")
        void repositoryReadsIssueAssociations() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var issues = repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                project.getId(), null, null, null, null, null, null, null, null, true));
                Issue dependencyIssue = issues.stream()
                                .filter(issue -> issue.title().equals("Dependency resolution flow blocked"))
                                .findFirst()
                                .orElseThrow();

                assertTrue(issues.size() >= 4);
                assertFalse(repositories.comments().findByIssueId(dependencyIssue.id()).isEmpty());
                assertFalse(repositories.issueHistory().findByIssueId(dependencyIssue.id()).isEmpty());
        }

        // Repositories: IssueRepository.findByCriteria
        @Test
        @DisplayName("seed issue ids use database ids")
        void seedIssueIdsUseDatabaseIds() {
                for (var projectName : List.of("Project A", "Project B")) {
                        var project = repositories.projects().findByName(projectName).orElseThrow();
                        var issues = repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                        project.getId(), null, null, null, null, null, null, null, null, true));

                        assertFalse(issues.isEmpty());
                        issues.forEach(OracleRepositoryIntegrationTest::assertDatabaseBackedIssueId);
                }
        }

        // Repositories: StatisticsRepository.calculateProjectStatistics,
        // AssignmentRecommendationRepository.findActiveDevCandidates/findActiveTesterCandidates
        @Test
        @DisplayName("statistics and recommendation query APIs return seed data")
        void queryApisReturnStatisticsAndRecommendations() {
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();
                var project1Report = repositories.statistics().calculateProjectStatistics(
                                project1.getId(), null, null, null, null);
                var project2Report = repositories.statistics().calculateProjectStatistics(
                                project2.getId(), null, null, null, null);

                assertTrue(project1Report.statusCounts().get(IssueStatus.CLOSED) >= 1);
                assertTrue(project1Report.statusCounts().get(IssueStatus.RESOLVED) >= 1);
                assertTrue(project2Report.statusCounts().get(IssueStatus.CLOSED) >= 1);
                assertTrue(project2Report.statusCounts().get(IssueStatus.RESOLVED) >= 1);
                assertFalse(project1Report.dailyCounts().isEmpty());
                assertFalse(project1Report.dailyStatusChangeCounts().isEmpty());
                assertFalse(project1Report.dailyCommentCounts().isEmpty());
                assertFalse(project1Report.monthlyStatusChangeCounts().isEmpty());
                assertFalse(project1Report.monthlyCommentCounts().isEmpty());
                assertFalse(repositories.assignmentRecommendations().findActiveDevCandidates(project1.getId())
                                .isEmpty());
                assertFalse(repositories.assignmentRecommendations().findActiveTesterCandidates(project1.getId())
                                .isEmpty());
        }

        // Repositories: DashboardSummaryRepository.findAllProjectSummaries,
        // IssueRepository.findByCriteria
        @Test
        @DisplayName("dashboard summary keeps the card counts")
        void dashboardSummaryKeepsCardCounts() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var summary = repositories.dashboardSummaries().findAllProjectSummaries().stream()
                                .filter(value -> value.projectId() == project.getId())
                                .findFirst()
                                .orElseThrow();

                assertEquals(project.getName(), summary.projectName());
                assertEquals(10, summary.memberCount());
                assertEquals(1, summary.projectLeaderCount());
                assertEquals(5, summary.developerCount());
                assertEquals(4, summary.testerCount());
                assertEquals(repositories.issues().findByCriteria(activeIssueCriteria(project.getId())).size(),
                                summary.visibleIssueCount());
                assertFalse(summary.statusCounts().containsKey(IssueStatus.DELETED));
        }

        // Repositories: DashboardSummaryRepository.findProjectSummariesByParticipant
        @Test
        @DisplayName("dashboard summary follows the member")
        void dashboardSummaryFollowsMember() {
                var project = repositories.projects().findByName("Project A").orElseThrow();

                var summaries = repositories.dashboardSummaries().findProjectSummariesByParticipant("pl1");

                assertEquals(List.of(project.getId()), summaries.stream()
                                .map(summary -> summary.projectId())
                                .toList());
        }

        // Repositories: IssueRepository.findByCriteria
        @Test
        @DisplayName("CLOSED seed issues clear active assignee and verifier")
        void closedSeedIssuesClearActiveAssigneeAndVerifier() {
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();

                Issue loginClosedIssue = findIssueByTitle(project1.getId(), "Login fails on invalid credential");
                Issue statisticsClosedIssue = findIssueByTitle(project2.getId(),
                                "Dashboard statistics misses closed issues");

                assertEquals(IssueStatus.CLOSED, loginClosedIssue.status());
                assertNull(loginClosedIssue.assigneeId());
                assertNull(loginClosedIssue.verifierId());
                assertNotNull(loginClosedIssue.fixerId());
                assertNotNull(loginClosedIssue.resolverId());

                assertEquals(IssueStatus.CLOSED, statisticsClosedIssue.status());
                assertNull(statisticsClosedIssue.assigneeId());
                assertNull(statisticsClosedIssue.verifierId());
                assertNotNull(statisticsClosedIssue.fixerId());
                assertNotNull(statisticsClosedIssue.resolverId());
        }

        // Repositories: IssueRepository.findByCriteria
        @Test
        @DisplayName("RESOLVED seed issues clear active assignee and verifier")
        void resolvedSeedIssuesClearActiveAssigneeAndVerifier() {
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();

                List<Issue> resolvedIssues = List.of(
                                findIssueByTitle(project1.getId(), "Search result filter returns stale status"),
                                findIssueByTitle(project1.getId(), "Verification rejection returns to assignee"),
                                findIssueByTitle(project2.getId(), "Reopened issue keeps old assignee"));

                for (Issue issue : resolvedIssues) {
                        assertEquals(IssueStatus.RESOLVED, issue.status());
                        assertNull(issue.assigneeId());
                        assertNull(issue.verifierId());
                        assertNotNull(issue.fixerId());
                        assertNotNull(issue.resolverId());
                }
        }

        // Repositories: IssueRepository.findByCriteria
        @Test
        @DisplayName("seed covers FIXED and DELETED issue states")
        void seedCoversFixedAndDeletedIssueStates() {
                var project1 = repositories.projects().findByName("Project A").orElseThrow();
                var project2 = repositories.projects().findByName("Project B").orElseThrow();

                Issue fixedIssue = findIssueByTitle(project2.getId(), "Report export fails after generation");
                Issue deletedFromNew = findIssueByTitleIncludingDeleted(project1.getId(),
                                "Duplicate mobile login report");
                Issue deletedFromClosed = findIssueByTitleIncludingDeleted(project2.getId(),
                                "Retired browser support checklist");

                assertEquals(IssueStatus.FIXED, fixedIssue.status());
                assertNotNull(fixedIssue.assigneeId());
                assertNotNull(fixedIssue.verifierId());
                assertNotNull(fixedIssue.fixerId());
                assertNull(fixedIssue.resolverId());

                assertEquals(IssueStatus.DELETED, deletedFromNew.status());
                assertEquals("NEW", latestDeleteHistory(deletedFromNew).previousValue());

                assertEquals(IssueStatus.DELETED, deletedFromClosed.status());
                assertEquals("CLOSED", latestDeleteHistory(deletedFromClosed).previousValue());
                assertNotNull(deletedFromClosed.fixerId());
                assertNotNull(deletedFromClosed.resolverId());
        }

        // Repositories: IssueRepository.findByCriteria,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("RESOLVED to REOPENED seed history is changed by PL")
        void reopenSeedHistoryIsChangedByPl() {
                var project = repositories.projects().findByName("Project B").orElseThrow();
                var pl2 = repositories.users().findByLoginId("pl2").orElseThrow();
                Issue reopenedIssue = findIssueByTitle(project.getId(), "Reopened issue keeps old assignee");

                var reopenHistory = repositories.issueHistory().findByIssueId(reopenedIssue.id()).stream()
                                .filter(history -> history.actionType() == ActionType.STATUS_CHANGED)
                                .filter(history -> "RESOLVED".equals(history.previousValue()))
                                .filter(history -> "REOPENED".equals(history.newValue()))
                                .findFirst()
                                .orElseThrow();

                assertEquals(pl2.getLoginId(), reopenHistory.changedById());
        }

        // Repositories: IssueRepository.findByCriteria,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("seed covers required status transition policy samples")
        void seedCoversRequiredStatusTransitionPolicySamples() {
                assertStatusTransition("Project A", "Login fails on invalid credential", "dev1", "ASSIGNED", "FIXED");
                assertStatusTransition("Project A", "Login fails on invalid credential", "tester2", "FIXED",
                                "RESOLVED");
                assertStatusTransition("Project A", "Login fails on invalid credential", "pl1", "RESOLVED", "CLOSED");
                assertStatusTransition("Project A", "Verification rejection returns to assignee", "tester2", "FIXED",
                                "ASSIGNED");
                assertStatusTransition("Project B", "Reopened issue keeps old assignee", "pl2", "RESOLVED", "REOPENED");
                assertStatusTransition("Project B", "Dashboard statistics misses closed issues", "pl2", "CLOSED",
                                "REOPENED");
                assertStatusTransition("Project B", "Report export fails after generation", "dev7", "ASSIGNED",
                                "FIXED");
                assertStatusTransition("Project A", "Duplicate mobile login report", "pl1", "NEW", "DELETED");
                assertStatusTransition("Project B", "Retired browser support checklist", "pl2", "CLOSED", "DELETED");
        }

        // Repositories: IssueRepository.findByCriteria,
        // CommentRepository.findByIssueId,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("every seed status change has a matching required comment")
        void everySeedStatusChangeHasMatchingComment() {
                for (var projectName : List.of("Project A", "Project B")) {
                        var project = repositories.projects().findByName(projectName).orElseThrow();
                        var issues = repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                        project.getId(), null, null, null, null, null, null, null, null, true));

                        for (Issue issue : issues) {
                                var comments = repositories.comments().findByIssueId(issue.id());
                                var statusHistories = repositories.issueHistory().findByIssueId(issue.id()).stream()
                                                .filter(history -> history.actionType() == ActionType.STATUS_CHANGED)
                                                .filter(OracleRepositoryIntegrationTest::needsStatusChangeComment)
                                                .toList();

                                for (var history : statusHistories) {
                                        assertNotNull(history.message());
                                        assertTrue(
                                                        comments.stream().anyMatch(comment -> comment.writerId()
                                                                        .equals(history.changedById())
                                                                        && comment.content().equals(history.message())),
                                                        () -> "Missing comment for status history: " + issue.title()
                                                                        + " " + history.previousValue() + " -> "
                                                                        + history.newValue());
                                }
                        }
                }
        }

        private static boolean needsStatusChangeComment(IssueHistory history) {
                return !isAssignmentStatusHistory(history);
        }

        private static boolean isAssignmentStatusHistory(IssueHistory history) {
                return IssueStatus.ASSIGNED.name().equals(history.newValue())
                                && ("Issue assigned from NEW".equals(history.message())
                                                || "Issue assigned from REOPENED".equals(history.message()));
        }

        // Repositories: IssueRepository.save/findByCriteria,
        // DeletedIssueRepository.findDeletedByProject,
        // StatisticsRepository.calculateProjectStatistics
        @Test
        @DisplayName("normal issue lists and statistics hide DELETED issues")
        void normalIssueQueriesAndStatisticsHideDeletedIssues() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var admin = repositories.users().findByLoginId("admin").orElseThrow();
                purgeIssuesByTitle(project.getId(), "Temporary deleted issue for repository policy test");
                LocalDateTime reportedAt = LocalDateTime.now();
                YearMonth reportedMonth = YearMonth.from(reportedAt);
                var beforeReport = repositories.statistics().calculateProjectStatistics(
                                project.getId(),
                                reportedAt.toLocalDate(),
                                reportedAt.toLocalDate(),
                                reportedMonth,
                                reportedMonth);
                int deletedStatusBefore = beforeReport.statusCounts().getOrDefault(IssueStatus.DELETED, 0);
                int trivialPriorityBefore = beforeReport.priorityCounts().getOrDefault(Priority.TRIVIAL, 0);
                int reportedDayBefore = countForDay(beforeReport.dailyCounts(), reportedAt);
                int reportedMonthBefore = countForMonth(beforeReport.monthlyCounts(), reportedMonth);
                int monthlyTrivialPriorityBefore = beforeReport.monthlyPriorityCounts()
                                .getOrDefault(reportedMonth, Map.of())
                                .getOrDefault(Priority.TRIVIAL, 0);

                Issue deletedIssue = repositories.issues().save(Issue.create(Issue.persistedState(
                                project.getId(),
                                "Temporary deleted issue for repository policy test",
                                "Deleted issues should stay out of normal browse and statistics.",
                                admin)
                                .issueId(temporaryIssueId("deleted_policy"))
                                .reportedDate(reportedAt)
                                .priority(Priority.TRIVIAL)
                                .status(IssueStatus.DELETED)
                                .updatedAt(reportedAt)));

                try {
                        var report = repositories.statistics().calculateProjectStatistics(
                                        project.getId(),
                                        reportedAt.toLocalDate(),
                                        reportedAt.toLocalDate(),
                                        reportedMonth,
                                        reportedMonth);

                        assertFalse(repositories.issues().findByCriteria(activeIssueCriteria(project.getId())).stream()
                                        .anyMatch(issue -> issue.id() == deletedIssue.id()));
                        assertTrue(repositories.deletedIssues().findDeletedByProject(project.getId()).stream()
                                        .anyMatch(issue -> issue.id() == deletedIssue.id()));
                        assertEquals(deletedStatusBefore, report.statusCounts().getOrDefault(IssueStatus.DELETED, 0));
                        assertEquals(trivialPriorityBefore, report.priorityCounts().getOrDefault(Priority.TRIVIAL, 0));
                        assertEquals(reportedDayBefore, countForDay(report.dailyCounts(), reportedAt));
                        assertEquals(reportedMonthBefore, countForMonth(report.monthlyCounts(), reportedMonth));
                        assertEquals(monthlyTrivialPriorityBefore,
                                        report.monthlyPriorityCounts()
                                                        .getOrDefault(reportedMonth, Map.of())
                                                        .getOrDefault(Priority.TRIVIAL, 0));
                } finally {
                        purgeTestIssue(deletedIssue.id());
                        purgeIssuesByTitle(project.getId(), "Temporary deleted issue for repository policy test");
                }
        }

        // Repositories: IssueRepository.save, DeletedIssueRepository.softDelete,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("soft delete records restore basis in issue history")
        void softDeleteRecordsRestoreBasisInIssueHistory() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue issue = null;

                try {
                        issue = createIssue(project.getId(), uniqueId("delete_history_basis_issue"), IssueStatus.NEW);
                        repositories.deletedIssues().softDelete(
                                        issue,
                                        "pl1",
                                        "Delete for restore basis.",
                                        LocalDateTime.now());

                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                                                        && IssueStatus.NEW.name().equals(history.previousValue())
                                                        && IssueStatus.DELETED.name().equals(history.newValue())));
                } finally {
                        if (issue != null) {
                                purgeTestIssue(issue.id());
                        }
                }
        }

        // Repositories: IssueRepository.save, DeletedIssueRepository.softDelete
        @Test
        @DisplayName("Issue soft delete accepts only NEW or CLOSED status")
        void issueSoftDeleteAcceptsOnlyNewOrClosedStatus() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue newIssue = null;
                Issue closedIssue = null;
                try {
                        newIssue = createIssue(project.getId(), uniqueId("delete_new_issue"), IssueStatus.NEW);
                        closedIssue = createIssue(project.getId(), uniqueId("delete_closed_issue"), IssueStatus.CLOSED);

                        assertEquals(IssueStatus.DELETED, repositories.deletedIssues()
                                        .softDelete(newIssue, "pl1", "Delete NEW issue.", LocalDateTime.now())
                                        .status());
                        assertEquals(IssueStatus.DELETED, repositories.deletedIssues()
                                        .softDelete(closedIssue, "pl1", "Delete CLOSED issue.",
                                                        LocalDateTime.now())
                                        .status());
                } finally {
                        if (newIssue != null) {
                                purgeTestIssue(newIssue.id());
                        }
                        if (closedIssue != null) {
                                purgeTestIssue(closedIssue.id());
                        }
                }
        }

        // Repositories: IssueDependencyRepository.recordDependencyAdded/existsByPair,
        // DeletedIssueRepository.softDelete, IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("Issue soft delete records removed dependency history on blocked issue")
        void issueSoftDeleteRecordsRemovedDependencyHistoryOnBlockedIssue() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue blocking = null;
                Issue blocked = null;
                IssueDependency dependency = null;

                try {
                        blocking = createIssue(project.getId(), uniqueId("soft_delete_dependency_blocking"),
                                        IssueStatus.NEW);
                        blocked = createIssue(project.getId(), uniqueId("soft_delete_dependency_blocked"),
                                        IssueStatus.NEW);
                        User projectLead = repositories.users().findByLoginId("pl1").orElseThrow();
                        dependency = repositories.issueDependencies().recordDependencyAdded(
                                        blocked.addDependency(
                                                        IssueDependency.dependencyIdFor(blocking.id(), blocked.id()),
                                                        blocking,
                                                        projectLead,
                                                        LocalDateTime.now()),
                                        blocked);

                        repositories.deletedIssues().softDelete(
                                        blocking,
                                        projectLead.getLoginId(),
                                        "Delete blocking issue.",
                                        LocalDateTime.now().plusSeconds(1));

                        String dependencyId = dependency.getDependencyId();
                        assertFalse(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));
                        assertTrue(repositories.issueHistory().findByIssueId(blocked.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependencyId.equals(history.previousValue())
                                                        && history.newValue() == null
                                                        && "Dependency removed".equals(history.message())));
                } finally {
                        if (dependency != null) {
                                String dependencyId = dependency.getDependencyId();
                                executeUpdate(
                                                "delete from issue_dependencies where dependency_id = ?",
                                                statement -> statement.setString(1, dependencyId));
                        }
                        if (blocked != null) {
                                purgeTestIssue(blocked.id());
                        }
                        if (blocking != null) {
                                purgeTestIssue(blocking.id());
                        }
                }
        }

        // Repositories:
        // DeletedIssueRepository.softDelete/findDeletedByProject/purgeDeletedById,
        // IssueRepository.findById, IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("purge removes one deleted issue")
        void purgeRemovesOneDeletedIssue() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue issue = null;

                try {
                        issue = createIssue(project.getId(), uniqueId("single_purge_issue"), IssueStatus.NEW);
                        Issue deleted = repositories.deletedIssues().softDelete(
                                        issue,
                                        "pl1",
                                        "Delete before single purge.",
                                        LocalDateTime.now());

                        assertTrue(repositories.deletedIssues().findDeletedByProject(project.getId()).stream()
                                        .anyMatch(value -> value.id() == deleted.id()));

                        int removed = repositories.deletedIssues().purgeDeletedById(deleted.id());
                        issue = null;

                        assertEquals(1, removed);
                        assertTrue(repositories.issues().findById(deleted.id()).isEmpty());
                        assertTrue(repositories.issueHistory().findByIssueId(deleted.id()).isEmpty());
                } finally {
                        if (issue != null) {
                                purgeTestIssue(issue.id());
                        }
                }
        }

        // Repositories: IssueRepository.save/findById,
        // DeletedIssueRepository.softDelete/restore
        @Test
        @DisplayName("Issue restore accepts only NEW or CLOSED pre-delete history")
        void issueRestoreAcceptsOnlyNewOrClosedPreDeleteHistory() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue deletedFromNew = null;
                Issue invalidDeleted = null;

                try {
                        deletedFromNew = createIssue(project.getId(), uniqueId("restore_new_issue"), IssueStatus.NEW);
                        repositories.deletedIssues().softDelete(deletedFromNew, "pl1", "Delete before restore.",
                                        LocalDateTime.now());

                        Issue deletedFromNewReloaded = repositories.issues().findById(deletedFromNew.id())
                                        .orElseThrow();
                        assertEquals(IssueStatus.NEW, repositories.deletedIssues()
                                        .restore(deletedFromNewReloaded, "pl1", "Restore NEW issue.",
                                                        LocalDateTime.now())
                                        .status());

                        invalidDeleted = createIssue(project.getId(), uniqueId("restore_invalid_issue"),
                                        IssueStatus.DELETED);
                        insertIssueHistory(
                                        invalidDeleted.id(),
                                        "pl1",
                                        ActionType.STATUS_CHANGED,
                                        IssueStatus.ASSIGNED.name(),
                                        IssueStatus.DELETED.name(),
                                        "Invalid delete history.",
                                        LocalDateTime.now());

                        Issue target = invalidDeleted;
                        assertThrows(RepositoryException.class,
                                        () -> repositories.deletedIssues().restore(
                                                        target,
                                                        "pl1",
                                                        "Invalid restore should be rejected.",
                                                        LocalDateTime.now()));
                        assertEquals(IssueStatus.DELETED,
                                        repositories.issues().findById(target.id()).orElseThrow().status());
                } finally {
                        if (deletedFromNew != null) {
                                purgeTestIssue(deletedFromNew.id());
                        }
                        if (invalidDeleted != null) {
                                purgeTestIssue(invalidDeleted.id());
                        }
                }
        }

        // Repositories: UserRepository.save/findByLoginId/findAll
        @Test
        @DisplayName("User repository saves, updates, finds, deactivates, and activates users")
        void userRepositorySupportsCrudPolicy() {
                String loginId = uniqueId("crud_dev");
                LocalDateTime now = LocalDateTime.now();

                try {
                        User created = repositories.users().save(User.create(
                                        loginId,
                                        "Repository CRUD Dev",
                                        "InitialPassword!",
                                        Role.DEV,
                                        now));

                        assertEquals(loginId, created.getLoginId());
                        assertEquals("Repository CRUD Dev", created.getName());
                        assertEquals(Role.DEV, repositories.users().findByLoginId(loginId).orElseThrow().getRole());
                        assertTrue(repositories.users().findAll().stream()
                                        .anyMatch(user -> user.getLoginId().equals(loginId)));

                        User updated = repositories.users().save(User.fromPersistence(
                                        loginId,
                                        "Repository CRUD Tester",
                                        "UpdatedPassword!",
                                        Role.TESTER,
                                        true,
                                        created.getCreatedAt(),
                                        LocalDateTime.now()));

                        assertTrue(new PasswordHasher().matches("InitialPassword!", updated.getPasswordHash()));
                        assertEquals("Repository CRUD Tester",
                                        repositories.users().findByLoginId(loginId).orElseThrow().getName());
                        assertEquals(Role.TESTER, updated.getRole());

                        updated.deactivate(LocalDateTime.now());
                        repositories.users().save(updated);

                        assertFalse(repositories.users().findByLoginId(loginId).orElseThrow().isActive());
                        updated.activate(LocalDateTime.now());
                        repositories.users().save(updated);

                        assertTrue(repositories.users().findByLoginId(loginId).orElseThrow().isActive());
                } finally {
                        deleteUser(loginId);
                }
        }

        // Repositories:
        // ProjectRepository.save/findByName/addParticipant/removeParticipant/deleteById,
        // IssueRepository.save/findById
        @Test
        @DisplayName("Project repository creates projects, manages participants, and deletes composition issues")
        void projectRepositorySupportsParticipantCrudAndCompositionDelete() {
                String projectName = uniqueId("crud_project");
                Project project = null;
                Issue issue = null;

                try {
                        project = repositories.projects().save(Project.create(
                                        projectName,
                                        "Repository CRUD test project.",
                                        "admin",
                                        LocalDateTime.now()));

                        assertEquals(projectName,
                                        repositories.projects().findByName(projectName).orElseThrow().getName());

                        Project updated = repositories.projects().save(Project.fromPersistence(
                                        project.getId(),
                                        project.getName(),
                                        "Updated repository CRUD test project.",
                                        "admin",
                                        project.getCreatedDate(),
                                        LocalDateTime.now()));

                        assertEquals("Updated repository CRUD test project.", updated.getDescription());

                        repositories.projects().addParticipant(project.getId(), "dev1");
                        assertTrue(repositories.projects().findParticipants(project.getId()).stream()
                                        .anyMatch(member -> member.userId().equals("dev1")));

                        repositories.projects().removeParticipant(project.getId(), "dev1");
                        assertFalse(repositories.projects().findParticipants(project.getId()).stream()
                                        .anyMatch(member -> member.userId().equals("dev1")));

                        issue = repositories.issues().save(Issue.create(Issue.persistedState(
                                        project.getId(),
                                        uniqueId("crud_project_composition_issue"),
                                        "Issue should be removed when its owning project is deleted.",
                                        user("dev1"))
                                        .issueId(temporaryIssueId("project_composition"))
                                        .reportedDate(LocalDateTime.now())
                                        .priority(Priority.MINOR)
                                        .status(IssueStatus.NEW)
                                        .updatedAt(LocalDateTime.now())));

                        long issueId = issue.id();
                        repositories.projects().addParticipant(project.getId(), "dev1");
                        repositories.projects().deleteById(project.getId());
                        project = null;
                        issue = null;

                        assertTrue(repositories.projects().findById(updated.getId()).isEmpty());
                        assertTrue(repositories.projects().findParticipants(updated.getId()).isEmpty());
                        assertTrue(repositories.issues().findById(issueId).isEmpty());
                } finally {
                        if (issue != null) {
                                purgeTestIssue(issue.id());
                        }
                        if (project != null) {
                                repositories.projects().deleteById(project.getId());
                        }
                }
        }

        // Repositories:
        // IssueRepository.save/findById/findByCriteria/existsByProjectIdAndTitle,
        // DeletedIssueRepository.findDeletedByProject
        @Test
        @DisplayName("Issue repository saves, searches, hides deleted issues, and purges")
        void issueRepositorySupportsSaveSearchDeletedAndPurge() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                String title = uniqueId("crud_issue");
                Issue saved = null;

                try {
                        saved = repositories.issues().save(Issue.create(Issue.persistedState(
                                        project.getId(),
                                        title,
                                        "Issue repository CRUD test.",
                                        user("dev1"))
                                        .issueId(temporaryIssueId("crud_issue"))
                                        .reportedDate(LocalDateTime.now())
                                        .priority(Priority.MINOR)
                                        .status(IssueStatus.NEW)
                                        .updatedAt(LocalDateTime.now())));

                        assertDatabaseBackedIssueId(saved);
                        assertEquals(title, repositories.issues().findById(saved.id()).orElseThrow().title());
                        assertTrue(repositories.issues().existsByProjectIdAndTitle(project.getId(), title));
                        long savedIssueId = saved.id();
                        assertTrue(repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                        project.getId(), IssueStatus.NEW, Priority.MINOR, "dev1", null, null,
                                        "crud_issue",
                                        null, null, false)).stream().anyMatch(issue -> issue.id() == savedIssueId));

                        Issue assigned = repositories.issues().save(Issue.fromPersistence(Issue.persistedState(
                                        saved.projectId(),
                                        saved.title(),
                                        "Issue repository CRUD test updated.",
                                        saved.getReporter())
                                        .id(saved.id())
                                        .issueId(saved.getIssueId())
                                        .reportedDate(saved.reportedDate())
                                        .priority(Priority.MAJOR)
                                        .status(IssueStatus.ASSIGNED)
                                        .assignee(user("dev2"))
                                        .verifier(user("tester1"))
                                        .updatedAt(LocalDateTime.now())));

                        assertEquals(IssueStatus.ASSIGNED, assigned.status());
                        assertEquals("dev2", assigned.assigneeId());
                        assertDatabaseBackedIssueId(assigned);

                        Issue deleted = repositories.issues().save(Issue.fromPersistence(Issue.persistedState(
                                        assigned.projectId(),
                                        assigned.title(),
                                        assigned.description(),
                                        assigned.getReporter())
                                        .id(assigned.id())
                                        .issueId(assigned.getIssueId())
                                        .reportedDate(assigned.reportedDate())
                                        .priority(assigned.priority())
                                        .status(IssueStatus.DELETED)
                                        .assignee(assigned.getAssignee())
                                        .verifier(assigned.getVerifier())
                                        .fixer(assigned.getFixer())
                                        .resolver(assigned.getResolver())
                                        .updatedAt(LocalDateTime.now())));

                        assertFalse(repositories.issues().findByCriteria(activeIssueCriteria(project.getId())).stream()
                                        .anyMatch(issue -> issue.id() == deleted.id()));
                        assertDatabaseBackedIssueId(deleted);
                        assertTrue(repositories.deletedIssues().findDeletedByProject(project.getId()).stream()
                                        .anyMatch(issue -> issue.id() == deleted.id()));
                        assertTrue(repositories.issues().existsByProjectIdAndTitle(project.getId(), title));

                        purgeTestIssue(deleted.id());
                        saved = null;

                        assertTrue(repositories.issues().findById(deleted.id()).isEmpty());
                } finally {
                        if (saved != null) {
                                purgeTestIssue(saved.id());
                        }
                        purgeIssuesByTitle(project.getId(), title);
                }
        }

        // Repositories: IssueRepository.findAllById
        @Test
        @DisplayName("issue batch lookup finds seed rows")
        void issueBatchLookupFindsSeedRows() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue loginIssue = findIssueByTitle(project.getId(), "Login fails on invalid credential");
                Issue searchIssue = findIssueByTitle(project.getId(), "Search result filter returns stale status");

                var foundIds = repositories.issues().findAllById(List.of(loginIssue.id(), searchIssue.id(), -1L))
                                .stream()
                                .map(Issue::id)
                                .toList();

                assertEquals(2, foundIds.size());
                assertTrue(foundIds.contains(loginIssue.id()));
                assertTrue(foundIds.contains(searchIssue.id()));
                assertFalse(foundIds.contains(-1L));
        }

        // Repositories: IssueRepository.save, CommentRepository.findByIssueId,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("Issue repository saves aggregate comments and histories atomically")
        void issueRepositoryPersistsAggregateCommentsAndHistories() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                String title = uniqueId("crud_issue_aggregate_audit");
                Issue issue = null;

                try {
                        issue = repositories.issues().save(Issue.create(Issue.persistedState(
                                        project.getId(),
                                        title,
                                        "Aggregate root save should persist audit children.",
                                        user("dev1"))
                                        .issueId(temporaryIssueId("aggregate_audit"))
                                        .reportedDate(LocalDateTime.now())
                                        .priority(Priority.MAJOR)
                                        .status(IssueStatus.NEW)
                                        .updatedAt(LocalDateTime.now())));

                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.CREATED
                                                        && history.previousValue() == null
                                                        && IssueStatus.NEW.name().equals(history.newValue())));

                        issue.assignFromNew(user("dev2"), user("tester1"), user("pl1"), LocalDateTime.now());
                        issue.addComment("aggregate-audit-comment", "Assignment audit comment.", user("pl1"),
                                        LocalDateTime.now());

                        repositories.issues().save(issue);

                        assertTrue(repositories.comments().findByIssueId(issue.id()).stream()
                                        .anyMatch(comment -> comment.content().equals("Assignment audit comment.")));
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.ASSIGNMENT_CHANGED));
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                                                        && IssueStatus.NEW.name().equals(history.previousValue())
                                                        && IssueStatus.ASSIGNED.name().equals(history.newValue())));
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && history.previousValue() == null
                                                        && "Assignment audit comment.".equals(history.newValue())
                                                        && "comment added".equals(history.message())));
                } finally {
                        if (issue != null) {
                                purgeTestIssue(issue.id());
                        }
                        purgeIssuesByTitle(project.getId(), title);
                }
        }

        // Repositories:
        // CommentRepository.saveCommentAndRecordHistory/findById/findByIssueId,
        // CommentRepository.deleteGeneralByIdAndRecordIssueChange
        @Test
        @DisplayName("Comment repository saves, updates, lists, and deletes comments with issue history")
        void commentRepositorySupportsCrud() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue issue = createIssue(project.getId(), uniqueId("crud_comment_issue"));

                try {
                        LocalDateTime createdAt = LocalDateTime.now();
                        Comment comment = repositories.comments().saveCommentAndRecordHistory(Comment.fromPersistence(
                                        0L,
                                        issue.id(),
                                        "tester1",
                                        "Initial repository comment.",
                                        CommentPurpose.GENERAL,
                                        createdAt,
                                        createdAt),
                                        IssueHistory.newForPersistence(
                                                        issue.id(),
                                                        "tester1",
                                                        ActionType.COMMENTED,
                                                        null,
                                                        "Initial repository comment.",
                                                        "comment added",
                                                        createdAt));

                        assertEquals("Initial repository comment.", repositories.comments().findById(comment.id())
                                        .orElseThrow().content());
                        long commentId = comment.id();
                        assertTrue(repositories.comments().findByIssueId(issue.id()).stream()
                                        .anyMatch(value -> value.id() == commentId));

                        LocalDateTime updatedAt = comment.createdDate().plusMinutes(10);
                        comment.changeContent("Updated repository comment.", updatedAt);
                        Comment updated = repositories.comments().saveCommentAndRecordHistory(Comment.fromPersistence(
                                        comment.id(),
                                        issue.id(),
                                        "tester1",
                                        comment.content(),
                                        CommentPurpose.GENERAL,
                                        comment.createdDate(),
                                        comment.updatedDate()),
                                        IssueHistory.newForPersistence(
                                                        issue.id(),
                                                        "tester1",
                                                        ActionType.COMMENTED,
                                                        "Initial repository comment.",
                                                        "Updated repository comment.",
                                                        "comment updated",
                                                        updatedAt));

                        assertEquals("Updated repository comment.", updated.content());
                        assertEquals(updatedAt, updated.updatedDate());

                        Comment statusChangeComment = repositories.comments()
                                        .saveCommentAndRecordHistory(Comment.fromPersistence(
                                                        0L,
                                                        issue.id(),
                                                        "tester1",
                                                        "Status-change repository comment.",
                                                        CommentPurpose.STATUS_CHANGE,
                                                        createdAt,
                                                        createdAt),
                                                        IssueHistory.newForPersistence(
                                                                        issue.id(),
                                                                        "tester1",
                                                                        ActionType.STATUS_CHANGED,
                                                                        IssueStatus.NEW.name(),
                                                                        IssueStatus.ASSIGNED.name(),
                                                                        "Status-change repository comment.",
                                                                        createdAt));
                        assertThrows(IllegalArgumentException.class,
                                        () -> repositories.comments().deleteGeneralByIdAndRecordIssueChange(
                                                        issue.id(),
                                                        statusChangeComment.id(),
                                                        "tester1",
                                                        IssueHistory.newForPersistence(
                                                                        issue.id(),
                                                                        "tester1",
                                                                        ActionType.COMMENTED,
                                                                        "Status-change repository comment.",
                                                                        null,
                                                                        "comment deleted",
                                                                        createdAt.plusMinutes(20))));
                        assertTrue(repositories.comments().findById(statusChangeComment.id()).isPresent());

                        repositories.comments().deleteGeneralByIdAndRecordIssueChange(
                                        issue.id(),
                                        comment.id(),
                                        "tester1",
                                        IssueHistory.newForPersistence(
                                                        issue.id(),
                                                        "tester1",
                                                        ActionType.COMMENTED,
                                                        "Updated repository comment.",
                                                        null,
                                                        "comment deleted",
                                                        createdAt.plusMinutes(30)));

                        assertTrue(repositories.comments().findById(updated.id()).isEmpty());
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && "comment added".equals(history.message())));
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && "comment updated".equals(history.message())));
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && "comment deleted".equals(history.message())));
                } finally {
                        purgeTestIssue(issue.id());
                }
        }

        // Repositories: DeletedIssueRepository.softDelete,
        // IssueHistoryRepository.findByIssueId
        @Test
        @DisplayName("IssueHistory repository finds histories recorded by workflows")
        void issueHistoryRepositoryFindsWorkflowHistories() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue issue = createIssue(project.getId(), uniqueId("crud_history_issue"), IssueStatus.NEW);

                try {
                        repositories.deletedIssues().softDelete(
                                        issue,
                                        "pl1",
                                        "Deleted for repository query test.",
                                        LocalDateTime.now());

                        IssueHistory history = repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .filter(value -> value.actionType() == ActionType.STATUS_CHANGED
                                                        && IssueStatus.DELETED.name().equals(value.newValue()))
                                        .findFirst()
                                        .orElseThrow();
                        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                        .anyMatch(value -> value.id() == history.id()));
                } finally {
                        purgeTestIssue(issue.id());
                }
        }

        // Repositories:
        // IssueDependencyRepository.recordDependencyAdded/findByDependencyId/existsByPair,
        // IssueDependencyRepository.findDependenciesBlockingIssue/recordDependencyRemoved
        @Test
        @DisplayName("IssueDependency repository saves, checks duplicates, and deletes dependencies")
        void issueDependencyRepositorySupportsCrudAndDuplicateChecks() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue blocking = createIssue(project.getId(), uniqueId("crud_blocking_issue"));
                Issue blocked = createIssue(project.getId(), uniqueId("crud_blocked_issue"));

                try {
                        User projectLead = repositories.users().findByLoginId("pl1").orElseThrow();
                        IssueDependency newDependency = blocked.addDependency(
                                        IssueDependency.dependencyIdFor(blocking.id(), blocked.id()),
                                        blocking,
                                        projectLead,
                                        LocalDateTime.now());
                        IssueDependency dependency = repositories.issueDependencies().recordDependencyAdded(
                                        newDependency,
                                        blocked);

                        assertEquals(dependency.id(),
                                        repositories.issueDependencies()
                                                        .findByDependencyId(dependency.getDependencyId()).orElseThrow()
                                                        .id());
                        assertEquals(IssueDependency.dependencyIdFor(blocking.id(), blocked.id()),
                                        dependency.getDependencyId());
                        assertTrue(repositories.issueHistory().findByIssueId(blocked.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependency.getDependencyId().equals(history.newValue())));
                        assertTrue(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));
                        assertTrue(repositories.issueDependencies().findDependenciesBlockingIssue(blocked.id()).stream()
                                        .anyMatch(value -> value.id() == dependency.id()));
                        assertThrows(RepositoryException.class,
                                        () -> repositories.issueDependencies().recordDependencyAdded(
                                                        IssueDependency.fromPersistence(
                                                                        0L,
                                                                        blocking.id(),
                                                                        blocked.id(),
                                                                        LocalDateTime.now()),
                                                        blocked));

                        LocalDateTime previousUpdatedAt = repositories.issues().findById(blocked.id()).orElseThrow()
                                        .updatedAt();
                        Issue blockedForRemoval = repositories.issues().findById(blocked.id()).orElseThrow();
                        blockedForRemoval.removeDependency(dependency, projectLead, previousUpdatedAt.plusSeconds(1));
                        repositories.issueDependencies().recordDependencyRemoved(
                                        dependency.getDependencyId(), blockedForRemoval);
                        assertFalse(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));
                        assertTrue(repositories.issueDependencies().findByDependencyId(dependency.getDependencyId())
                                        .isEmpty());
                        assertTrue(repositories.issueHistory().findByIssueId(blocked.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependency.getDependencyId().equals(history.previousValue())
                                                        && history.newValue() == null));
                        assertTrue(
                                        repositories.issues().findById(blocked.id()).orElseThrow().updatedAt()
                                                        .isAfter(previousUpdatedAt));

                } finally {
                        purgeTestIssue(blocked.id());
                        purgeTestIssue(blocking.id());
                }
        }

        // Repositories:
        // IssueDependencyRepository.recordDependencyAdded/recordDependencyRemoved/findByDependencyId
        @Test
        @DisplayName("IssueDependency repository deletes by dependency id")
        void issueDependencyRepositoryDeletesByDependencyId() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                Issue blocking = createIssue(project.getId(), uniqueId("dependency_id_delete_blocking"));
                Issue blocked = createIssue(project.getId(), uniqueId("dependency_id_delete_blocked"));

                try {
                        User projectLead = repositories.users().findByLoginId("pl1").orElseThrow();
                        IssueDependency dependency = repositories.issueDependencies().recordDependencyAdded(
                                        blocked.addDependency(
                                                        IssueDependency.dependencyIdFor(blocking.id(), blocked.id()),
                                                        blocking,
                                                        projectLead,
                                                        LocalDateTime.now()),
                                        blocked);
                        String dependencyId = dependency.getDependencyId();
                        LocalDateTime previousUpdatedAt = repositories.issues().findById(blocked.id()).orElseThrow()
                                        .updatedAt();

                        Issue blockedForRemoval = repositories.issues().findById(blocked.id()).orElseThrow();
                        blockedForRemoval.removeDependency(dependency, projectLead, previousUpdatedAt.plusSeconds(1));
                        repositories.issueDependencies().recordDependencyRemoved(dependencyId,
                                        blockedForRemoval);

                        assertTrue(repositories.issueDependencies().findByDependencyId(dependencyId).isEmpty());
                        assertFalse(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));
                        assertTrue(repositories.issueHistory().findByIssueId(blocked.id()).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependencyId.equals(history.previousValue())
                                                        && history.newValue() == null));
                        assertTrue(
                                        repositories.issues().findById(blocked.id()).orElseThrow().updatedAt()
                                                        .isAfter(previousUpdatedAt));
                } finally {
                        purgeTestIssue(blocked.id());
                        purgeTestIssue(blocking.id());
                }
        }

        // Repositories: IssueRepository.save,
        // StatisticsRepository.calculateProjectStatistics,
        // AssignmentRecommendationRepository.findResolvedIssuesForRecommendation
        @Test
        @DisplayName("Statistics and recommendation repositories reflect saved resolved issues")
        void statisticsAndRecommendationRepositoriesReflectSavedResolvedIssues() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var beforeReport = repositories.statistics().calculateProjectStatistics(
                                project.getId(), null, null, null, null);
                int resolvedBefore = beforeReport.statusCounts().getOrDefault(IssueStatus.RESOLVED, 0);
                int criticalBefore = beforeReport.priorityCounts().getOrDefault(Priority.CRITICAL, 0);
                int dev5Before = completedIssueCountForDev("dev5", project.getId());
                int tester4Before = completedIssueCountForTester("tester4", project.getId());
                Issue issue = null;

                try {
                        issue = repositories.issues().save(Issue.create(Issue.persistedState(
                                        project.getId(),
                                        uniqueId("crud_stats_issue"),
                                        "Resolved issue for statistics and recommendation CRUD test.",
                                        user("tester4"))
                                        .issueId(temporaryIssueId("stats"))
                                        .reportedDate(LocalDateTime.now())
                                        .priority(Priority.CRITICAL)
                                        .status(IssueStatus.RESOLVED)
                                        .fixer(user("dev5"))
                                        .resolver(user("tester4"))
                                        .updatedAt(LocalDateTime.now())));

                        var afterReport = repositories.statistics().calculateProjectStatistics(
                                        project.getId(), null, null, null, null);
                        assertEquals(resolvedBefore + 1,
                                        afterReport.statusCounts().getOrDefault(IssueStatus.RESOLVED, 0));
                        assertEquals(criticalBefore + 1,
                                        afterReport.priorityCounts().getOrDefault(Priority.CRITICAL, 0));
                        assertFalse(afterReport.dailyCounts().isEmpty());
                        assertFalse(afterReport.monthlyCounts().isEmpty());
                        assertEquals(dev5Before + 1, completedIssueCountForDev("dev5", project.getId()));
                        assertEquals(tester4Before + 1, completedIssueCountForTester("tester4", project.getId()));
                } finally {
                        if (issue != null) {
                                purgeTestIssue(issue.id());
                        }
                }
        }

        // Repositories via AccountService: UserRepository, ProjectRepository,
        // IssueRepository
        @Test
        @DisplayName("Account service validates and persists account creation")
        void accountServiceValidatesAndPersistsAccountCreation() {
                var service = accountService();
                var admin = user("admin");
                String loginId = uniqueId("oracle_account").toLowerCase(Locale.ROOT);

                try {
                        var created = service.createAccount(
                                        " " + loginId + " ",
                                        "Oracle Account",
                                        "TempPassword1!",
                                        Role.DEV,
                                        admin);

                        assertEquals(loginId, created.loginId());
                        User stored = repositories.users().findByLoginId(loginId).orElseThrow();
                        assertEquals("Oracle Account", stored.getName());
                        assertEquals(Role.DEV, stored.getRole());
                        assertTrue(new PasswordHasher().matches("TempPassword1!", stored.getPasswordHash()));
                        String blankPasswordLoginId = uniqueId("blank_password");
                        assertThrows(IllegalArgumentException.class,
                                        () -> service.createAccount(loginId, "Duplicate", "TempPassword1!", Role.DEV,
                                                        admin));
                        assertThrows(IllegalArgumentException.class,
                                        () -> service.createAccount(blankPasswordLoginId, "Blank Password", " ",
                                                        Role.DEV, admin));
                        assertThrows(IllegalArgumentException.class,
                                        () -> service.deactivateAccount("dev1", admin));
                } finally {
                        deleteUser(loginId);
                }
        }

        // Repositories via ProjectService: ProjectRepository, IssueRepository,
        // UserRepository
        @Test
        @DisplayName("Project service blocks removing active assignees or verifiers")
        void projectServiceBlocksRemovingActiveAssignmentParticipants() {
                var service = projectService();
                String projectName = uniqueId("oracle_project");
                long projectId = 0L;

                try {
                        var project = service.createProject(projectName, "Project member guard integration test.",
                                        "admin");
                        long createdProjectId = project.id();
                        projectId = createdProjectId;
                        service.addProjectParticipant(createdProjectId, "dev5", "admin");
                        service.addProjectParticipant(createdProjectId, "dev6", "admin");

                        Issue assignedIssue = repositories.issues().save(Issue.create(Issue.persistedState(
                                        createdProjectId,
                                        uniqueId("participant_guard_issue"),
                                        "Assigned issue for participant removal guard.",
                                        user("tester1"))
                                        .issueId(temporaryIssueId("participant_guard"))
                                        .reportedDate(LocalDateTime.now())
                                        .priority(Priority.MAJOR)
                                        .status(IssueStatus.ASSIGNED)
                                        .assignee(user("dev5"))
                                        .verifier(user("tester4"))
                                        .updatedAt(LocalDateTime.now())));

                        assertThrows(IllegalArgumentException.class,
                                        () -> service.removeProjectParticipant(createdProjectId, "dev5", "admin"));

                        service.removeProjectParticipant(createdProjectId, "dev6", "admin");
                        assertFalse(repositories.projects().findParticipants(createdProjectId).stream()
                                        .anyMatch(member -> member.userId().equals("dev6")));
                        assertEquals(
                                        IssueStatus.ASSIGNED,
                                        repositories.issues().findById(assignedIssue.id()).orElseThrow().status());
                } finally {
                        if (projectId > 0L) {
                                repositories.projects().deleteById(projectId);
                        }
                }
        }

        // Repositories via workflow services: IssueRepository, CommentRepository,
        // IssueHistoryRepository, AssignmentRecommendationRepository
        @Test
        @DisplayName("Assignment and issue state services persist workflow through Oracle")
        void assignmentAndIssueStateServicesPersistWorkflowThroughOracle() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var issueService = issueService();
                var assignmentService = assignmentService();
                var stateService = issueStateService();
                String title = uniqueId("oracle_workflow_issue");
                long issueId = 0L;

                try {
                        var registered = issueService.registerIssue(
                                        project.getId(),
                                        title,
                                        "Issue registered through service path for Oracle workflow test.",
                                        Priority.MAJOR,
                                        "tester1");
                        long registeredIssueId = registered.id();
                        issueId = registeredIssueId;
                        assertEquals("issue-" + registeredIssueId, registered.issueId());
                        assertDatabaseBackedIssueId(repositories.issues().findById(registeredIssueId).orElseThrow());

                        var options = assignmentService.startAssignment(registeredIssueId, "pl1");
                        assertFalse(options.devAssigneeCandidates().isEmpty());
                        assertFalse(options.testerVerifierCandidates().isEmpty());
                        assertThrows(SecurityException.class,
                                        () -> assignmentService.assignIssue(registeredIssueId, "dev6", "tester1",
                                                        "pl1"));

                        assignmentService.assignIssue(registeredIssueId, "dev1", "tester1", "pl1");
                        assertEquals(
                                        IssueStatus.ASSIGNED,
                                        repositories.issues().findById(registeredIssueId).orElseThrow().status());

                        stateService.changeStatus(
                                        registeredIssueId,
                                        IssueStatus.FIXED,
                                        "Fixed through Oracle workflow test.",
                                        "dev1");
                        stateService.changeStatus(
                                        registeredIssueId,
                                        IssueStatus.RESOLVED,
                                        "Resolved through Oracle workflow test.",
                                        "tester1");

                        Issue resolved = repositories.issues().findById(registeredIssueId).orElseThrow();
                        assertEquals(IssueStatus.RESOLVED, resolved.status());
                        assertNull(resolved.assigneeId());
                        assertNull(resolved.verifierId());
                        assertTrue(repositories.comments().findByIssueId(registeredIssueId).stream()
                                        .anyMatch(comment -> comment.purpose() == CommentPurpose.STATUS_CHANGE
                                                        && comment.content().equals(
                                                                        "Fixed through Oracle workflow test.")));
                        assertTrue(repositories.issueHistory().findByIssueId(registeredIssueId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                                                        && IssueStatus.ASSIGNED.name().equals(history.previousValue())
                                                        && IssueStatus.FIXED.name().equals(history.newValue())));
                        assertTrue(repositories.issueHistory().findByIssueId(registeredIssueId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                                                        && IssueStatus.FIXED.name().equals(history.previousValue())
                                                        && IssueStatus.RESOLVED.name().equals(history.newValue())));
                } finally {
                        if (issueId > 0L) {
                                purgeTestIssue(issueId);
                        }
                }
        }

        // Repositories via IssueService: IssueRepository, CommentRepository,
        // IssueHistoryRepository
        @Test
        @DisplayName("Issue service records comment update and delete audit history")
        void issueServiceRecordsCommentUpdateAndDeleteAuditHistory() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var service = issueService();
                String title = uniqueId("oracle_comment_audit_issue");
                long issueId = 0L;

                try {
                        var issue = service.registerIssue(
                                        project.getId(),
                                        title,
                                        "Issue for comment audit integration test.",
                                        Priority.MINOR,
                                        "tester1");
                        issueId = issue.id();

                        service.addComment(issueId, "Initial general comment.", "tester1");
                        Comment comment = repositories.comments().findByIssueId(issueId).stream()
                                        .filter(candidate -> candidate.content().equals("Initial general comment."))
                                        .findFirst()
                                        .orElseThrow();

                        service.updateComment(issueId, comment.id(), "Updated general comment.", "tester1");
                        assertEquals("Updated general comment.",
                                        repositories.comments().findById(comment.id()).orElseThrow().content());
                        assertTrue(repositories.issueHistory().findByIssueId(issueId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && "Initial general comment.".equals(history.previousValue())
                                                        && "Updated general comment.".equals(history.newValue())));

                        service.deleteComment(issueId, comment.id(), "tester1");
                        assertTrue(repositories.comments().findById(comment.id()).isEmpty());
                        assertTrue(repositories.issueHistory().findByIssueId(issueId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.COMMENTED
                                                        && "Updated general comment.".equals(history.previousValue())
                                                        && history.newValue() == null
                                                        && "comment deleted".equals(history.message())));
                } finally {
                        if (issueId > 0L) {
                                purgeTestIssue(issueId);
                        }
                }
        }

        // Repositories via IssueService: IssueRepository, IssueDependencyRepository,
        // IssueHistoryRepository
        @Test
        @DisplayName("Issue service enforces dependency project and cycle policies")
        void issueServiceEnforcesDependencyProjectAndCyclePolicies() {
                var projectA = repositories.projects().findByName("Project A").orElseThrow();
                var projectB = repositories.projects().findByName("Project B").orElseThrow();
                var service = issueService();
                List<Long> issueIds = new ArrayList<>();

                try {
                        long blockingId = service.registerIssue(projectA.getId(), uniqueId("dependency_blocking"),
                                        "Project A blocking issue.", Priority.MAJOR, "tester1").id();
                        long blockedId = service.registerIssue(projectA.getId(), uniqueId("dependency_blocked"),
                                        "Project A blocked issue.", Priority.MAJOR, "tester1").id();
                        long thirdId = service.registerIssue(projectA.getId(), uniqueId("dependency_third"),
                                        "Project A third issue.", Priority.MAJOR, "tester1").id();
                        long otherProjectIssueId = service
                                        .registerIssue(projectB.getId(), uniqueId("dependency_other_project"),
                                                        "Project B issue for cross-project dependency rejection.",
                                                        Priority.MAJOR, "tester2")
                                        .id();
                        issueIds.add(blockingId);
                        issueIds.add(blockedId);
                        issueIds.add(thirdId);
                        issueIds.add(otherProjectIssueId);

                        var dependency = service.addDependency(blockingId, blockedId, "pl1");
                        assertTrue(repositories.issueDependencies().findByProjectId(projectA.getId()).stream()
                                        .anyMatch(saved -> saved.getDependencyId().equals(dependency.dependencyId())));
                        assertTrue(repositories.issueHistory().findByIssueId(blockedId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependency.dependencyId().equals(history.newValue())));

                        assertThrows(IllegalArgumentException.class,
                                        () -> service.addDependency(blockedId, blockingId, "pl1"));
                        assertThrows(IllegalArgumentException.class,
                                        () -> service.addDependency(otherProjectIssueId, thirdId, "pl1"));

                        service.removeDependency(blockingId, blockedId, "pl1");
                        assertTrue(repositories.issueDependencies().findByDependencyId(dependency.dependencyId())
                                        .isEmpty());
                        assertTrue(repositories.issueHistory().findByIssueId(blockedId).stream()
                                        .anyMatch(history -> history.actionType() == ActionType.DEPENDENCY_CHANGED
                                                        && dependency.dependencyId().equals(history.previousValue())
                                                        && history.newValue() == null));
                } finally {
                        issueIds.forEach(id -> purgeTestIssue(id));
                }
        }

        // Repositories via services: DeletedIssueRepository, StatisticsRepository,
        // IssueRepository
        @Test
        @DisplayName("Deleted issue and statistics services enforce project scope")
        void deletedIssueAndStatisticsServicesEnforceProjectScope() {
                var project = repositories.projects().findByName("Project A").orElseThrow();
                var issueService = issueService();
                var deletedIssueService = deletedIssueService();
                var statisticsService = statisticsService();
                long issueId = 0L;

                try {
                        var report = statisticsService.viewStatistics(
                                        project.getId(),
                                        null,
                                        null,
                                        YearMonth.of(2026, 3),
                                        YearMonth.of(2026, 5),
                                        user("pl1"));
                        assertFalse(report.monthlyCounts().isEmpty());
                        assertFalse(report.monthlyStatusCounts().isEmpty());
                        long projectId = project.getId();
                        User outsideDeveloper = user("dev10");
                        assertThrows(SecurityException.class,
                                        () -> statisticsService.viewStatistics(projectId, null, null, null, null,
                                                        outsideDeveloper));

                        var issue = issueService.registerIssue(
                                        project.getId(),
                                        uniqueId("oracle_deleted_issue"),
                                        "Issue for deleted workflow service integration test.",
                                        Priority.MINOR,
                                        "tester1");
                        long registeredIssueId = issue.id();
                        issueId = registeredIssueId;

                        var deleted = deletedIssueService.deleteIssue(
                                        registeredIssueId,
                                        "Delete through Oracle service test.",
                                        user("pl1"));
                        assertEquals(IssueStatus.DELETED, deleted.status());
                        assertThrows(SecurityException.class,
                                        () -> issueService.viewIssueDetail(registeredIssueId, "pl1"));
                        assertTrue(deletedIssueService.viewDeletedIssues(project.getId(), user("pl1")).stream()
                                        .anyMatch(summary -> summary.id() == registeredIssueId));

                        var restored = deletedIssueService.restoreIssue(
                                        registeredIssueId,
                                        "Restore through Oracle service test.",
                                        user("pl1"));
                        assertEquals(IssueStatus.NEW, restored.status());
                } finally {
                        if (issueId > 0L) {
                                purgeTestIssue(issueId);
                        }
                }
        }

        private static AccountService accountService() {
                return new AccountService(
                                permissionPolicy(),
                                repositories.users(),
                                repositories.projects(),
                                repositories.issues(),
                                new PasswordHasher(),
                                LocalDateTime::now);
        }

        private static ProjectService projectService() {
                return new ProjectService(
                                repositories.projects(),
                                repositories.issues(),
                                repositories.users(),
                                permissionPolicy(),
                                LocalDateTime::now);
        }

        private static IssueService issueService() {
                return new IssueService(
                                repositories.projects(),
                                repositories.issues(),
                                repositories.issueDependencies(),
                                repositories.comments(),
                                repositories.issueHistory(),
                                repositories.users(),
                                permissionPolicy(),
                                OracleRepositoryIntegrationTest::nextIssueId,
                                LocalDateTime::now);
        }

        private static AssignmentService assignmentService() {
                return new AssignmentService(
                                repositories.issues(),
                                repositories.users(),
                                permissionPolicy(),
                                new AssignmentRecommendationService(repositories.assignmentRecommendations(),
                                                new KNNAssignmentRecommendation()),
                                LocalDateTime::now);
        }

        private static IssueStateService issueStateService() {
                return new IssueStateService(
                                repositories.issues(),
                                repositories.issueDependencies(),
                                repositories.users(),
                                permissionPolicy(),
                                LocalDateTime::now,
                                OracleRepositoryIntegrationTest::nextCommentId);
        }

        private static String nextCommentId() {
                return "COMMENT-test-" + UUID.randomUUID();
        }

        private static String nextIssueId() {
                return temporaryIssueId("service");
        }

        private static DeletedIssueService deletedIssueService() {
                return new DeletedIssueService(
                                repositories.issues(),
                                repositories.deletedIssues(),
                                repositories.users(),
                                permissionPolicy(),
                                LocalDateTime::now);
        }

        private static StatisticsService statisticsService() {
                return new StatisticsService(
                                permissionPolicy(),
                                repositories.statistics(),
                                repositories.users());
        }

        private static PermissionPolicy permissionPolicy() {
                return new PermissionPolicy();
        }

        private static Issue findIssueByTitle(long projectId, String title) {
                return repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                projectId, null, null, null, null, null, title, null, null, true)).stream()
                                .filter(issue -> issue.title().equals(title))
                                .findFirst()
                                .orElseThrow();
        }

        private static IssueSearchCriteria activeIssueCriteria(long projectId) {
                return IssueSearchCriteria.create(projectId, null, null, null, null, null, null, null, null, false);
        }

        private static Issue findIssueByTitleIncludingDeleted(long projectId, String title) {
                return findIssueByTitle(projectId, title);
        }

        private static IssueHistory latestDeleteHistory(Issue issue) {
                return repositories.issueHistory().findByIssueId(issue.id()).stream()
                                .filter(history -> history.actionType() == ActionType.STATUS_CHANGED)
                                .filter(history -> IssueStatus.DELETED.name().equals(history.newValue()))
                                .findFirst()
                                .orElseThrow();
        }

        private static void assertStatusTransition(
                        String projectName,
                        String issueTitle,
                        String changedById,
                        String previousValue,
                        String newValue) {
                var project = repositories.projects().findByName(projectName).orElseThrow();
                var changedBy = repositories.users().findByLoginId(changedById).orElseThrow();
                var issue = findIssueByTitle(project.getId(), issueTitle);

                assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                                .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                                                && history.changedById().equals(changedBy.getLoginId())
                                                && previousValue.equals(history.previousValue())
                                                && newValue.equals(history.newValue())),
                                () -> "Missing transition: " + projectName + " / " + issueTitle
                                                + " / " + previousValue + " -> " + newValue);
        }

        private static void purgeIssuesByTitle(long projectId, String title) {
                repositories.issues().findByCriteria(IssueSearchCriteria.create(
                                projectId, null, null, null, null, null, title, null, null, true)).stream()
                                .filter(issue -> issue.title().equals(title))
                                .forEach(issue -> purgeTestIssue(issue.id()));
        }

        private static Issue createIssue(long projectId, String title) {
                return createIssue(projectId, title, IssueStatus.ASSIGNED);
        }

        private static Issue createIssue(long projectId, String title, IssueStatus status) {
                var state = Issue.persistedState(
                                projectId,
                                title,
                                "Repository CRUD support issue.",
                                user("dev1"))
                                .issueId(temporaryIssueId("support"))
                                .reportedDate(LocalDateTime.now())
                                .priority(Priority.MINOR)
                                .status(status)
                                .updatedAt(LocalDateTime.now());

                if (status == IssueStatus.ASSIGNED || status == IssueStatus.FIXED || status == IssueStatus.RESOLVED) {
                        state.assignee(user("dev1"));
                        state.verifier(user("tester1"));
                }
                if (status == IssueStatus.FIXED || status == IssueStatus.RESOLVED || status == IssueStatus.CLOSED
                                || status == IssueStatus.REOPENED) {
                        state.fixer(user("dev1"));
                }
                if (status == IssueStatus.RESOLVED || status == IssueStatus.CLOSED || status == IssueStatus.REOPENED) {
                        state.resolver(user("tester1"));
                }

                return repositories.issues().save(Issue.create(state));
        }

        private static User user(String loginId) {
                return repositories.users().findByLoginId(loginId).orElseThrow();
        }

        private static int completedIssueCountForDev(String loginId, long projectId) {
                return (int) repositories.assignmentRecommendations()
                                .findResolvedIssuesForRecommendation(projectId).stream()
                                .filter(data -> loginId.equals(data.fixerLoginId()))
                                .count();
        }

        private static int completedIssueCountForTester(String loginId, long projectId) {
                return (int) repositories.assignmentRecommendations()
                                .findResolvedIssuesForRecommendation(projectId).stream()
                                .filter(data -> loginId.equals(data.resolverLoginId()))
                                .count();
        }

        private static int countForDay(List<DailyIssueCount> counts, LocalDateTime reportedAt) {
                return counts.stream()
                                .filter(count -> count.date().equals(reportedAt.toLocalDate()))
                                .mapToInt(DailyIssueCount::count)
                                .sum();
        }

        private static int countForMonth(List<MonthlyIssueCount> counts, YearMonth month) {
                return counts.stream()
                                .filter(count -> count.month().equals(month))
                                .mapToInt(MonthlyIssueCount::count)
                                .sum();
        }

        private static void assertDatabaseBackedIssueId(Issue issue) {
                assertEquals("issue-" + issue.id(), issue.getIssueId());
        }

        private static String temporaryIssueId(String label) {
                return uniqueId("pending_issue_" + label);
        }

        private static String uniqueId(String prefix) {
                return prefix + "_" + Long.toString(System.nanoTime(), 36);
        }

        private static void deleteUser(String loginId) {
                executeUpdate("delete from users where login_id = ?", statement -> statement.setString(1, loginId));
        }

        private static void insertIssueHistory(
                        long issueId,
                        String changedById,
                        ActionType actionType,
                        String previousValue,
                        String newValue,
                        String message,
                        LocalDateTime changedAt) {
                String sql = """
                                insert into issue_history (
                                  issue_id, changed_by_login_id, action_type, previous_value, new_value, message, changed_at
                                )
                                values (?, ?, ?, ?, ?, ?, ?)
                                """;
                executeUpdate(sql, statement -> {
                        statement.setLong(1, issueId);
                        statement.setString(2, changedById);
                        statement.setString(3, actionType.name());
                        statement.setString(4, previousValue);
                        statement.setString(5, newValue);
                        statement.setString(6, message);
                        statement.setObject(7, changedAt);
                });
        }

        private static void executeUpdate(String sql, SqlStatementBinder binder) {
                try (var connection = connectionProvider.getConnection();
                                var statement = connection.prepareStatement(sql)) {
                        binder.bind(statement);
                        statement.executeUpdate();
                } catch (SQLException exception) {
                        throw new RepositoryException("Failed to clean up repository integration test data.",
                                        exception);
                }
        }

        @FunctionalInterface
        private interface SqlStatementBinder {

                void bind(PreparedStatement statement) throws SQLException;
        }
}
