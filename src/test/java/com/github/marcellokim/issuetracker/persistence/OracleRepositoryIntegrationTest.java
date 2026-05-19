package com.github.marcellokim.issuetracker.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.ActionType;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueDependency;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.repository.RepositoryException;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        resetTestSchema();
        DatabaseInitializer.initialize(connectionProvider);
        DatabaseInitializer.initialize(connectionProvider);

        repositories = new JdbcRepositoryFactory(connectionProvider);
    }

    private static void resetTestSchema() throws SQLException {
        try (var connection = connectionProvider.getConnection();
                var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                for (String tableName : List.of(
                        "issue_dependencies",
                        "issue_history",
                        "comments",
                        "issues",
                        "project_members",
                        "projects",
                        "user_credentials",
                        "users")) {
                    statement.executeUpdate("delete from " + tableName);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @Test
    @DisplayName("repeatable seed keeps admin and demo projects stable")
    void repeatableSeedKeepsAdminAndProjectStable() {
        var admin = repositories.users().findById("admin").orElseThrow();
        var project1 = repositories.projects().findByName("project1").orElseThrow();
        var project2 = repositories.projects().findByName("project2").orElseThrow();

        assertEquals(Role.ADMIN, admin.role());
        assertTrue(admin.active());
        assertEquals("project1", project1.name());
        assertEquals("project2", project2.name());
        assertEquals(admin.loginId(), project1.managedById());
        assertEquals(admin.loginId(), project2.managedById());
    }

    @Test
    @DisplayName("project members and role candidates are queryable")
    void repositoryFindsProjectMembersAndRoleCandidates() {
        var project1 = repositories.projects().findByName("project1").orElseThrow();
        var project2 = repositories.projects().findByName("project2").orElseThrow();

        assertEquals(16, repositories.projects().findParticipants(project1.id()).size());
        assertEquals(1, repositories.users().findActiveByRole(project1.id(), Role.PL).size());
        assertEquals(10, repositories.users().findActiveByRole(project1.id(), Role.DEV).size());
        assertEquals(5, repositories.users().findActiveByRole(project1.id(), Role.TESTER).size());

        assertEquals(5, repositories.projects().findParticipants(project2.id()).size());
        assertEquals(1, repositories.users().findActiveByRole(project2.id(), Role.PL).size());
        assertEquals(2, repositories.users().findActiveByRole(project2.id(), Role.DEV).size());
        assertEquals(2, repositories.users().findActiveByRole(project2.id(), Role.TESTER).size());
    }

    @Test
    @DisplayName("issue, comment, history, and dependency associations are queryable")
    void repositoryReadsIssueAssociations() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        var issues = repositories.issues().findByCriteria(new IssueSearchCriteria(
                project.id(), null, null, null, null, null, null, null, null, true));
        Issue dependencyIssue = issues.stream()
                .filter(issue -> issue.title().equals("Dependency resolution flow blocked"))
                .findFirst()
                .orElseThrow();

        assertTrue(issues.size() >= 4);
        assertFalse(repositories.comments().findByIssueId(dependencyIssue.id()).isEmpty());
        assertFalse(repositories.issueHistory().findByIssueId(dependencyIssue.id()).isEmpty());
        assertFalse(repositories.issueDependencies().findByIssueId(dependencyIssue.id()).isEmpty());
    }

    @Test
    @DisplayName("statistics and recommendation query APIs return seed data")
    void queryApisReturnStatisticsAndRecommendations() {
        var project1 = repositories.projects().findByName("project1").orElseThrow();
        var project2 = repositories.projects().findByName("project2").orElseThrow();

        assertTrue(repositories.statistics().countByStatus(project1.id()).get(IssueStatus.CLOSED) >= 1);
        assertTrue(repositories.statistics().countByStatus(project1.id()).get(IssueStatus.RESOLVED) >= 1);
        assertTrue(repositories.statistics().countByStatus(project2.id()).get(IssueStatus.CLOSED) >= 1);
        assertTrue(repositories.statistics().countByStatus(project2.id()).get(IssueStatus.RESOLVED) >= 1);
        assertFalse(repositories.statistics().countReportedIssuesByDay(project1.id()).isEmpty());
        assertFalse(repositories.assignmentRecommendations().findDevAssigneeCandidates(project1.id()).isEmpty());
        assertFalse(repositories.assignmentRecommendations().findTesterVerifierCandidates(project1.id()).isEmpty());
    }

    @Test
    @DisplayName("CLOSED seed issues clear active assignee and verifier")
    void closedSeedIssuesClearActiveAssigneeAndVerifier() {
        var project1 = repositories.projects().findByName("project1").orElseThrow();
        var project2 = repositories.projects().findByName("project2").orElseThrow();

        Issue loginClosedIssue = findIssueByTitle(project1.id(), "Login fails on invalid credential");
        Issue statisticsClosedIssue = findIssueByTitle(project2.id(), "Dashboard statistics misses closed issues");

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

    @Test
    @DisplayName("RESOLVED to REOPENED seed history is changed by PL")
    void reopenSeedHistoryIsChangedByPl() {
        var project = repositories.projects().findByName("project2").orElseThrow();
        var pl2 = repositories.users().findById("pl2").orElseThrow();
        Issue reopenedIssue = findIssueByTitle(project.id(), "Reopened issue keeps old assignee");

        var reopenHistory = repositories.issueHistory().findByIssueId(reopenedIssue.id()).stream()
                .filter(history -> history.actionType() == ActionType.STATUS_CHANGED)
                .filter(history -> "RESOLVED".equals(history.previousValue()))
                .filter(history -> "REOPENED".equals(history.newValue()))
                .findFirst()
                .orElseThrow();

        assertEquals(pl2.loginId(), reopenHistory.changedById());
    }

    @Test
    @DisplayName("seed covers required status transition policy samples")
    void seedCoversRequiredStatusTransitionPolicySamples() {
        assertStatusTransition("project1", "Login fails on invalid credential", "dev1", "ASSIGNED", "FIXED");
        assertStatusTransition("project1", "Login fails on invalid credential", "tester2", "FIXED", "RESOLVED");
        assertStatusTransition("project1", "Login fails on invalid credential", "pl1", "RESOLVED", "CLOSED");
        assertStatusTransition("project1", "Verification rejection returns to assignee", "tester5", "FIXED",
                "ASSIGNED");
        assertStatusTransition("project2", "Reopened issue keeps old assignee", "pl2", "RESOLVED", "REOPENED");
        assertStatusTransition("project2", "Dashboard statistics misses closed issues", "pl2", "CLOSED", "REOPENED");
    }

    @Test
    @DisplayName("every seed status change has a matching required comment")
    void everySeedStatusChangeHasMatchingComment() {
        for (var projectName : List.of("project1", "project2")) {
            var project = repositories.projects().findByName(projectName).orElseThrow();
            var issues = repositories.issues().findByCriteria(new IssueSearchCriteria(
                    project.id(), null, null, null, null, null, null, null, null, true));

            for (Issue issue : issues) {
                var comments = repositories.comments().findByIssueId(issue.id());
                var statusHistories = repositories.issueHistory().findByIssueId(issue.id()).stream()
                        .filter(history -> history.actionType() == ActionType.STATUS_CHANGED)
                        .toList();

                for (var history : statusHistories) {
                    assertNotNull(history.message());
                    assertTrue(
                            comments.stream().anyMatch(comment -> comment.writerId().equals(history.changedById())
                                    && comment.content().equals(history.message())),
                            () -> "Missing comment for status history: " + issue.title()
                                    + " " + history.previousValue() + " -> " + history.newValue());
                }
            }
        }
    }

    @Test
    @DisplayName("normal issue lists and statistics exclude DELETED issues")
    void normalIssueQueriesAndStatisticsExcludeDeletedIssues() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        var admin = repositories.users().findById("admin").orElseThrow();
        purgeIssuesByTitle(project.id(), "Temporary deleted issue for repository policy test");

        Issue deletedIssue = repositories.issues().save(Issue.newForPersistence(Issue.persistedState(
                project.id(),
                "Temporary deleted issue for repository policy test",
                "Deleted issues should stay out of normal browse and statistics.",
                admin)
                .reportedDate(LocalDateTime.now())
                .priority(Priority.TRIVIAL)
                .status(IssueStatus.DELETED)
                .updatedAt(LocalDateTime.now())));

        try {
            assertFalse(repositories.issues().findByProject(project.id()).stream()
                    .anyMatch(issue -> issue.id() == deletedIssue.id()));
            assertTrue(repositories.issues().findDeletedByProject(project.id()).stream()
                    .anyMatch(issue -> issue.id() == deletedIssue.id()));
            assertFalse(repositories.statistics().countByStatus(project.id()).containsKey(IssueStatus.DELETED));
        } finally {
            repositories.issues().purge(deletedIssue.id());
            purgeIssuesByTitle(project.id(), "Temporary deleted issue for repository policy test");
        }
    }

    @Test
    @DisplayName("delete and restore basis uses IssueHistory query API")
    void deletedRestoreBasisUsesIssueHistoryQuery() {
        var project = repositories.projects().findByName("project1").orElseThrow();

        assertNotNull(repositories.issueHistory().findDeletedTransitionsByProject(project.id()));
    }

    @Test
    @DisplayName("Issue soft delete accepts only NEW or CLOSED status")
    void issueSoftDeleteAcceptsOnlyNewOrClosedStatus() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        Issue newIssue = null;
        Issue closedIssue = null;
        var rejectedIssues = new ArrayList<Issue>();

        try {
            newIssue = createIssue(project.id(), uniqueId("delete_new_issue"), IssueStatus.NEW);
            closedIssue = createIssue(project.id(), uniqueId("delete_closed_issue"), IssueStatus.CLOSED);

            assertEquals(IssueStatus.DELETED, repositories.issues()
                    .softDelete(newIssue.id(), "pl1", "Delete NEW issue.", LocalDateTime.now())
                    .status());
            assertEquals(IssueStatus.DELETED, repositories.issues()
                    .softDelete(closedIssue.id(), "pl1", "Delete CLOSED issue.", LocalDateTime.now())
                    .status());

            for (IssueStatus status : List.of(
                    IssueStatus.ASSIGNED,
                    IssueStatus.FIXED,
                    IssueStatus.RESOLVED,
                    IssueStatus.REOPENED,
                    IssueStatus.DELETED)) {
                Issue issue = createIssue(project.id(), uniqueId("delete_rejected_issue"), status);
                rejectedIssues.add(issue);

                assertThrows(RepositoryException.class,
                        () -> repositories.issues().softDelete(
                                issue.id(),
                                "pl1",
                                "Delete should be rejected.",
                                LocalDateTime.now()));
                assertEquals(status, repositories.issues().findById(issue.id()).orElseThrow().status());
            }
        } finally {
            if (newIssue != null) {
                repositories.issues().purge(newIssue.id());
            }
            if (closedIssue != null) {
                repositories.issues().purge(closedIssue.id());
            }
            for (Issue issue : rejectedIssues) {
                repositories.issues().purge(issue.id());
            }
        }
    }

    @Test
    @DisplayName("Issue restore accepts only NEW or CLOSED pre-delete history")
    void issueRestoreAcceptsOnlyNewOrClosedPreDeleteHistory() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        Issue deletedFromNew = null;
        Issue invalidDeleted = null;

        try {
            deletedFromNew = createIssue(project.id(), uniqueId("restore_new_issue"), IssueStatus.NEW);
            repositories.issues().softDelete(deletedFromNew.id(), "pl1", "Delete before restore.", LocalDateTime.now());

            assertEquals(IssueStatus.NEW, repositories.issues()
                    .restore(deletedFromNew.id(), "pl1", "Restore NEW issue.", LocalDateTime.now())
                    .status());

            invalidDeleted = createIssue(project.id(), uniqueId("restore_invalid_issue"), IssueStatus.DELETED);
            repositories.issueHistory().save(new IssueHistory(
                    0L,
                    invalidDeleted.id(),
                    "pl1",
                    ActionType.STATUS_CHANGED,
                    IssueStatus.ASSIGNED.name(),
                    IssueStatus.DELETED.name(),
                    "Invalid delete history.",
                    LocalDateTime.now()));

            Issue target = invalidDeleted;
            assertThrows(RepositoryException.class,
                    () -> repositories.issues().restore(
                            target.id(),
                            "pl1",
                            "Invalid restore should be rejected.",
                            LocalDateTime.now()));
            assertEquals(IssueStatus.DELETED, repositories.issues().findById(target.id()).orElseThrow().status());
        } finally {
            if (deletedFromNew != null) {
                repositories.issues().purge(deletedFromNew.id());
            }
            if (invalidDeleted != null) {
                repositories.issues().purge(invalidDeleted.id());
            }
        }
    }

    @Test
    @DisplayName("User repository saves, updates, finds, and deactivates users")
    void userRepositorySupportsCrudPolicy() {
        String loginId = uniqueId("crud_dev");
        LocalDateTime now = LocalDateTime.now();

        try {
            User created = repositories.users().save(new User(
                    loginId,
                    "Repository CRUD Dev",
                    "InitialPassword!",
                    Role.DEV,
                    true,
                    now,
                    now));

            assertEquals(loginId, created.loginId());
            assertEquals("Repository CRUD Dev", created.getName());
            assertEquals(Role.DEV, repositories.users().findByLoginId(loginId).orElseThrow().role());
            assertTrue(repositories.users().findAll().stream().anyMatch(user -> user.loginId().equals(loginId)));

            User updated = repositories.users().save(new User(
                    loginId,
                    "Repository CRUD Tester",
                    "UpdatedPassword!",
                    Role.TESTER,
                    true,
                    created.createdAt(),
                    LocalDateTime.now()));

            assertTrue(new PasswordHasher().matches("UpdatedPassword!", updated.password()));
            assertEquals("Repository CRUD Tester", repositories.users().findById(loginId).orElseThrow().getName());
            assertEquals(Role.TESTER, updated.role());

            repositories.users().deactivate(loginId);

            assertFalse(repositories.users().findById(loginId).orElseThrow().active());
        } finally {
            deleteUser(loginId);
        }
    }

    @Test
    @DisplayName("Project repository creates projects, manages participants, and deletes composition issues")
    void projectRepositorySupportsParticipantCrudAndCompositionDelete() {
        String projectName = uniqueId("crud_project");
        Project project = null;
        Issue issue = null;

        try {
            project = repositories.projects().save(new Project(
                    0L,
                    projectName,
                    "Repository CRUD test project.",
                    "admin",
                    LocalDateTime.now(),
                    LocalDateTime.now()));

            assertEquals(projectName, repositories.projects().findByName(projectName).orElseThrow().name());

            Project updated = repositories.projects().save(new Project(
                    project.id(),
                    project.name(),
                    "Updated repository CRUD test project.",
                    "admin",
                    project.createdDate(),
                    LocalDateTime.now()));

            assertEquals("Updated repository CRUD test project.", updated.description());

            repositories.projects().addParticipant(project.id(), "dev1");
            assertTrue(repositories.projects().findParticipants(project.id()).stream()
                    .anyMatch(member -> member.userId().equals("dev1")));

            repositories.projects().removeParticipant(project.id(), "dev1");
            assertFalse(repositories.projects().findParticipants(project.id()).stream()
                    .anyMatch(member -> member.userId().equals("dev1")));

            issue = repositories.issues().save(Issue.newForPersistence(Issue.persistedState(
                    project.id(),
                    uniqueId("crud_project_composition_issue"),
                    "Issue should be removed when its owning project is deleted.",
                    user("dev1"))
                    .reportedDate(LocalDateTime.now())
                    .priority(Priority.MINOR)
                    .status(IssueStatus.NEW)
                    .updatedAt(LocalDateTime.now())));

            long issueId = issue.id();
            repositories.projects().addParticipant(project.id(), "dev1");
            repositories.projects().deleteById(project.id());
            project = null;
            issue = null;

            assertTrue(repositories.projects().findById(updated.id()).isEmpty());
            assertTrue(repositories.projects().findParticipants(updated.id()).isEmpty());
            assertTrue(repositories.issues().findById(issueId).isEmpty());
        } finally {
            if (issue != null) {
                repositories.issues().purge(issue.id());
            }
            if (project != null) {
                repositories.projects().deleteById(project.id());
            }
        }
    }

    @Test
    @DisplayName("Issue repository saves, searches, hides deleted issues, and purges")
    void issueRepositorySupportsSaveSearchDeletedAndPurge() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        String title = uniqueId("crud_issue");
        Issue saved = null;

        try {
            saved = repositories.issues().save(Issue.newForPersistence(Issue.persistedState(
                    project.id(),
                    title,
                    "Issue repository CRUD test.",
                    user("dev1"))
                    .reportedDate(LocalDateTime.now())
                    .priority(Priority.MINOR)
                    .status(IssueStatus.NEW)
                    .updatedAt(LocalDateTime.now())));

            assertEquals(title, repositories.issues().findById(saved.id()).orElseThrow().title());
            long savedIssueId = saved.id();
            assertTrue(repositories.issues().findByCriteria(new IssueSearchCriteria(
                    project.id(), IssueStatus.NEW, Priority.MINOR, "dev1", null, null, "crud_issue",
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

            assertFalse(repositories.issues().findByProject(project.id()).stream()
                    .anyMatch(issue -> issue.id() == deleted.id()));
            assertTrue(repositories.issues().findDeletedByProject(project.id()).stream()
                    .anyMatch(issue -> issue.id() == deleted.id()));

            repositories.issues().purge(deleted.id());
            saved = null;

            assertTrue(repositories.issues().findById(deleted.id()).isEmpty());
        } finally {
            if (saved != null) {
                repositories.issues().purge(saved.id());
            }
            purgeIssuesByTitle(project.id(), title);
        }
    }

    @Test
    @DisplayName("Issue repository saves aggregate comments and histories atomically")
    void issueRepositoryPersistsAggregateCommentsAndHistories() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        String title = uniqueId("crud_issue_aggregate_audit");
        Issue issue = null;

        try {
            issue = repositories.issues().save(Issue.newForPersistence(Issue.persistedState(
                    project.id(),
                    title,
                    "Aggregate root save should persist audit children.",
                    user("dev1"))
                    .reportedDate(LocalDateTime.now())
                    .priority(Priority.MAJOR)
                    .status(IssueStatus.NEW)
                    .updatedAt(LocalDateTime.now())));

            issue.assignFromNew(user("dev2"), user("tester1"), user("pl1"), LocalDateTime.now());
            issue.addComment("aggregate-audit-comment", "Assignment audit comment.", user("pl1"), LocalDateTime.now());

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
                    .anyMatch(history -> history.actionType() == ActionType.COMMENTED));
        } finally {
            if (issue != null) {
                repositories.issues().purge(issue.id());
            }
            purgeIssuesByTitle(project.id(), title);
        }
    }

    @Test
    @DisplayName("Comment repository saves, updates, lists, and deletes comments")
    void commentRepositorySupportsCrud() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        Issue issue = createIssue(project.id(), uniqueId("crud_comment_issue"));
        Comment comment = null;

        try {
            comment = repositories.comments().save(new Comment(
                    0L,
                    issue.id(),
                    "tester1",
                    "Initial repository comment.",
                    LocalDateTime.now()));

            assertEquals("Initial repository comment.", repositories.comments().findById(comment.id())
                    .orElseThrow().content());
            long commentId = comment.id();
            assertTrue(repositories.comments().findByIssueId(issue.id()).stream()
                    .anyMatch(value -> value.id() == commentId));

            Comment updated = repositories.comments().save(new Comment(
                    comment.id(),
                    issue.id(),
                    "tester1",
                    "Updated repository comment.",
                    comment.createdDate()));

            assertEquals("Updated repository comment.", updated.content());

            repositories.comments().deleteById(comment.id());
            comment = null;

            assertTrue(repositories.comments().findById(updated.id()).isEmpty());
        } finally {
            if (comment != null) {
                repositories.comments().deleteById(comment.id());
            }
            repositories.issues().purge(issue.id());
        }
    }

    @Test
    @DisplayName("IssueHistory repository saves history and exposes delete/restore basis")
    void issueHistoryRepositorySupportsSaveAndRestoreBasisLookup() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        Issue issue = createIssue(project.id(), uniqueId("crud_history_issue"));

        try {
            IssueHistory history = repositories.issueHistory().save(new IssueHistory(
                    0L,
                    issue.id(),
                    "pl1",
                    ActionType.STATUS_CHANGED,
                    "ASSIGNED",
                    "DELETED",
                    "Deleted for repository CRUD test.",
                    LocalDateTime.now()));

            assertEquals(history.id(), repositories.issueHistory().findById(history.id()).orElseThrow().id());
            assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                    .anyMatch(value -> value.id() == history.id()));
            assertEquals(history.id(), repositories.issueHistory().findLatestStatusChangeToDeleted(issue.id())
                    .orElseThrow().id());
            assertTrue(repositories.issueHistory().findDeletedTransitionsByProject(project.id()).stream()
                    .anyMatch(value -> value.id() == history.id()));
        } finally {
            repositories.issues().purge(issue.id());
        }
    }

    @Test
    @DisplayName("IssueDependency repository saves, checks duplicates, and deletes dependencies")
    void issueDependencyRepositorySupportsCrudAndDuplicateChecks() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        Issue blocking = createIssue(project.id(), uniqueId("crud_blocking_issue"));
        Issue blocked = createIssue(project.id(), uniqueId("crud_blocked_issue"));

        try {
            IssueDependency dependency = repositories.issueDependencies().save(new IssueDependency(
                    0L,
                    blocking.id(),
                    blocked.id(),
                    LocalDateTime.now()));

            assertEquals(dependency.id(),
                    repositories.issueDependencies().findById(dependency.id()).orElseThrow().id());
            assertEquals(IssueDependency.dependencyIdFor(blocking.id(), blocked.id()), dependency.getDependencyId());
            assertTrue(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));
            assertTrue(repositories.issueDependencies().findByIssueId(blocking.id()).stream()
                    .anyMatch(value -> value.id() == dependency.id()));
            assertTrue(repositories.issueDependencies().findByBlockingIssueId(blocking.id()).stream()
                    .anyMatch(value -> value.id() == dependency.id()));
            assertTrue(repositories.issueDependencies().findByBlockedIssueId(blocked.id()).stream()
                    .anyMatch(value -> value.id() == dependency.id()));
            assertThrows(RepositoryException.class, () -> repositories.issueDependencies().save(new IssueDependency(
                    0L,
                    blocking.id(),
                    blocked.id(),
                    LocalDateTime.now())));

            repositories.issueDependencies().deleteById(dependency.id());
            assertFalse(repositories.issueDependencies().existsByPair(blocking.id(), blocked.id()));

            IssueDependency secondDependency = repositories.issueDependencies().save(new IssueDependency(
                    0L,
                    blocking.id(),
                    blocked.id(),
                    LocalDateTime.now()));
            repositories.issueDependencies().deleteByIssueId(blocking.id());

            assertTrue(repositories.issueDependencies().findById(secondDependency.id()).isEmpty());
        } finally {
            repositories.issues().purge(blocked.id());
            repositories.issues().purge(blocking.id());
        }
    }

    @Test
    @DisplayName("Statistics and recommendation repositories reflect saved resolved issues")
    void statisticsAndRecommendationRepositoriesReflectSavedResolvedIssues() {
        var project = repositories.projects().findByName("project1").orElseThrow();
        int resolvedBefore = repositories.statistics().countByStatus(project.id())
                .getOrDefault(IssueStatus.RESOLVED, 0);
        int criticalBefore = repositories.statistics().countByPriority(project.id())
                .getOrDefault(Priority.CRITICAL, 0);
        int dev10Before = completedIssueCountForDev("dev10", project.id());
        int tester5Before = completedIssueCountForTester("tester5", project.id());
        Issue issue = null;

        try {
            issue = repositories.issues().save(Issue.newForPersistence(Issue.persistedState(
                    project.id(),
                    uniqueId("crud_stats_issue"),
                    "Resolved issue for statistics and recommendation CRUD test.",
                    user("tester5"))
                    .reportedDate(LocalDateTime.now())
                    .priority(Priority.CRITICAL)
                    .status(IssueStatus.RESOLVED)
                    .fixer(user("dev10"))
                    .resolver(user("tester5"))
                    .updatedAt(LocalDateTime.now())));

            assertEquals(resolvedBefore + 1, repositories.statistics().countByStatus(project.id())
                    .getOrDefault(IssueStatus.RESOLVED, 0));
            assertEquals(criticalBefore + 1, repositories.statistics().countByPriority(project.id())
                    .getOrDefault(Priority.CRITICAL, 0));
            assertFalse(repositories.statistics().countReportedIssuesByDay(project.id()).isEmpty());
            assertFalse(repositories.statistics().countReportedIssuesByMonth(project.id()).isEmpty());
            assertEquals(dev10Before + 1, completedIssueCountForDev("dev10", project.id()));
            assertEquals(tester5Before + 1, completedIssueCountForTester("tester5", project.id()));
        } finally {
            if (issue != null) {
                repositories.issues().purge(issue.id());
            }
        }
    }

    private static Issue findIssueByTitle(long projectId, String title) {
        return repositories.issues().findByCriteria(new IssueSearchCriteria(
                projectId, null, null, null, null, null, title, null, null, true)).stream()
                .filter(issue -> issue.title().equals(title))
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
        var changedBy = repositories.users().findById(changedById).orElseThrow();
        var issue = findIssueByTitle(project.id(), issueTitle);

        assertTrue(repositories.issueHistory().findByIssueId(issue.id()).stream()
                .anyMatch(history -> history.actionType() == ActionType.STATUS_CHANGED
                        && history.changedById().equals(changedBy.loginId())
                        && previousValue.equals(history.previousValue())
                        && newValue.equals(history.newValue())),
                () -> "Missing transition: " + projectName + " / " + issueTitle
                        + " / " + previousValue + " -> " + newValue);
    }

    private static void purgeIssuesByTitle(long projectId, String title) {
        repositories.issues().findByCriteria(new IssueSearchCriteria(
                projectId, null, null, null, null, null, title, null, null, true)).stream()
                .filter(issue -> issue.title().equals(title))
                .forEach(issue -> repositories.issues().purge(issue.id()));
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

        return repositories.issues().save(Issue.newForPersistence(state));
    }

    private static User user(String loginId) {
        return repositories.users().findById(loginId).orElseThrow();
    }

    private static int completedIssueCountForDev(String loginId, long projectId) {
        return repositories.assignmentRecommendations().findDevAssigneeCandidates(projectId).stream()
                .filter(candidate -> candidate.user().loginId().equals(loginId))
                .findFirst()
                .orElseThrow()
                .completedIssueCount();
    }

    private static int completedIssueCountForTester(String loginId, long projectId) {
        return repositories.assignmentRecommendations().findTesterVerifierCandidates(projectId).stream()
                .filter(candidate -> candidate.user().loginId().equals(loginId))
                .findFirst()
                .orElseThrow()
                .completedIssueCount();
    }

    private static String uniqueId(String prefix) {
        return prefix + "_" + Long.toString(System.nanoTime(), 36);
    }

    private static void deleteUser(String loginId) {
        executeUpdate("delete from users where login_id = ?", statement -> statement.setString(1, loginId));
    }

    private static void executeUpdate(String sql, SqlStatementBinder binder) {
        try (var connection = connectionProvider.getConnection();
                var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Failed to clean up repository integration test data.", exception);
        }
    }

    @FunctionalInterface
    private interface SqlStatementBinder {

        void bind(PreparedStatement statement) throws SQLException;
    }
}
