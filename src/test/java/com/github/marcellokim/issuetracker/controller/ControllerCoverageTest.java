package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Controller layer")
class ControllerCoverageTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 10, 0);
    private static final long PROJECT_ID = 10L;

    @Test
    @DisplayName("authentication controller delegates login and logout")
    void authenticationControllerDelegatesLoginAndLogout() {
        PasswordHasher hasher = new PasswordHasher();
        User user = User.fromPersistence("dev", "dev", hasher.hash("secret"), Role.DEV, true, NOW, NOW);
        FakeUserRepository users = new FakeUserRepository(user);
        AuthenticationService authService = new AuthenticationService(users, hasher, new SessionStore());
        AuthenticationController controller = new AuthenticationController(authService);

        var result = controller.login(user.getLoginId(), "secret");
        controller.logout();

        assertTrue(result.success());
        assertEquals(Optional.empty(), authService.currentUser());
    }

    @Test
    @DisplayName("dashboard controller reads dashboard data through service after auth")
    void dashboardControllerDelegatesDashboardReads() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeDashboardSummaryRepository dashboardSummaries = new FakeDashboardSummaryRepository(
                List.of(new DashboardProjectSnapshot(
                        PROJECT_ID,
                        "project",
                        "description",
                        1,
                        0,
                        0,
                        0,
                        1,
                        0,
                        Map.of(IssueStatus.NEW, 1))),
                List.of());
        DashboardController controller = new DashboardController(
                auth.service(),
                new DashboardSummaryService(dashboardSummaries, auth.users(), new PermissionPolicy()));

        assertEquals(1, controller.viewProjects().size());
        assertEquals(List.of(auth.user().getLoginId()), controller.viewUsers().stream()
                .map(UserResult::loginId)
                .toList());
    }

    @Test
    @DisplayName("dashboard controller rejects anonymous users")
    void dashboardControllerRejectsAnonymousUsers() {
        DashboardController controller = new DashboardController(
                anonymousAuth(),
                new DashboardSummaryService(
                        new FakeDashboardSummaryRepository(List.of(), List.of()),
                        new FakeUserRepository(),
                        new PermissionPolicy()));

        assertThrows(SecurityException.class, controller::viewProjects);
    }

    @Test
    @DisplayName("statistics controller delegates report query after auth and range validation")
    void statisticsControllerDelegatesReportQuery() {
        AuthFixture auth = authenticated(Role.DEV);
        FakeStatisticsRepository statistics = new FakeStatisticsRepository();
        StatisticsReport expectedReport = report();
        statistics.report = expectedReport;

        StatisticsController controller = new StatisticsController(
                auth.service(),
                new StatisticsService(new PermissionPolicy(), statistics, auth.users()));

        StatisticsReportResult actualReport = controller.viewStatistics(
                PROJECT_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6));

        assertEquals(expectedReport.statusCounts(), actualReport.statusCounts());
        assertEquals(expectedReport.priorityCounts(), actualReport.priorityCounts());
        assertEquals(PROJECT_ID, statistics.reportProjectId);
        assertEquals(LocalDate.of(2026, 5, 1), statistics.dailyFrom);
        assertEquals(YearMonth.of(2026, 6), statistics.monthlyTo);
    }

    @Test
    @DisplayName("statistics controller rejects missing login and reversed ranges")
    void statisticsControllerRejectsInvalidAccessOrRange() {
        StatisticsController anonymousController = new StatisticsController(
                anonymousAuth(),
                new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(),
                        new FakeUserRepository()));
        assertThrows(SecurityException.class, () -> anonymousController.viewStatistics(PROJECT_ID));

        StatisticsController controller = new StatisticsController(
                authenticated(Role.PL).service(),
                new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(), new FakeUserRepository(
                        user("pl", Role.PL))));
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.viewStatistics(
                        PROJECT_ID,
                        LocalDate.of(2026, 5, 2),
                        LocalDate.of(2026, 5, 1),
                        null,
                        null));
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.viewStatistics(
                        PROJECT_ID,
                        null,
                        null,
                        YearMonth.of(2026, 6),
                        YearMonth.of(2026, 5)));
    }

    @Test
    @DisplayName("deleted issue controller lists, soft-deletes, restores, and purges for PL")
    void deletedIssueControllerManagesDeletedIssues() {
        AuthFixture auth = authenticated(Role.PL);
        Issue activeIssue = issue(101L, PROJECT_ID, IssueStatus.NEW);
        Issue deletedIssue = issue(102L, PROJECT_ID, IssueStatus.DELETED);
        Issue purgeTarget = issue(103L, PROJECT_ID, IssueStatus.DELETED);
        FakeIssueRepository issues = new FakeIssueRepository(activeIssue, deletedIssue, purgeTarget);
        DeletedIssueController controller = new DeletedIssueController(
                auth.service(),
                new DeletedIssueService(issues, auth.users(), new PermissionPolicy(), java.time.LocalDateTime::now));

        List<IssueSummary> deletedIssues = controller.viewDeletedIssues(PROJECT_ID);
        IssueSummary softDeleted = controller.deleteIssue(activeIssue.id(), "remove from demo");
        IssueSummary restored = controller.restoreIssue(deletedIssue.id(), "restore for demo");
        int purged = controller.purgeOverflow(PROJECT_ID);
        controller.purgeDeletedIssue(purgeTarget.id());

        assertEquals(List.of(deletedIssue.id(), purgeTarget.id()),
                deletedIssues.stream().map(IssueSummary::id).toList());
        assertEquals(IssueStatus.DELETED, softDeleted.status());
        assertEquals(IssueStatus.NEW, restored.status());
        assertEquals("pl", issues.lastChangedBy);
        assertEquals("restore for demo", issues.lastRestoreMessage);
        assertEquals(2, purged);
        assertEquals(30, issues.lastPurgeLimit);
        assertEquals(purgeTarget.id(), issues.lastPurgedIssueId);
        assertTrue(issues.findById(purgeTarget.id()).isEmpty());
    }

    @Test
    @DisplayName("deleted issue controller rejects anonymous, ADMIN, and missing issue paths")
    void deletedIssueControllerRejectsInvalidAccess() {
        FakeIssueRepository issues = new FakeIssueRepository(issue(101L, PROJECT_ID, IssueStatus.NEW));
        DeletedIssueController anonymousController = new DeletedIssueController(
                anonymousAuth(),
                new DeletedIssueService(issues, new FakeUserRepository(), new PermissionPolicy(),
                        java.time.LocalDateTime::now));
        assertThrows(SecurityException.class, () -> anonymousController.viewDeletedIssues(PROJECT_ID));

        DeletedIssueController adminController = new DeletedIssueController(
                authenticated(Role.ADMIN).service(),
                new DeletedIssueService(issues, new FakeUserRepository(), new PermissionPolicy(),
                        java.time.LocalDateTime::now));
        SecurityException adminFailure = assertThrows(SecurityException.class,
                () -> adminController.deleteIssue(101L, "admin cannot delete"));
        assertEquals("Only PL can manage deleted issues.", adminFailure.getMessage());

        DeletedIssueController plController = new DeletedIssueController(
                authenticated(Role.PL).service(),
                new DeletedIssueService(issues, new FakeUserRepository(), new PermissionPolicy(),
                        java.time.LocalDateTime::now));
        assertThrows(IllegalArgumentException.class, () -> plController.restoreIssue(999L, "missing"));
    }

    @Test
    @DisplayName("project controller deletes an existing project only for ADMIN")
    void projectControllerDeletesProjectForAdmin() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
        ProjectController controller = new ProjectController(
                auth.service(),
                new ProjectService(projects, new FakeIssueRepository(), auth.users(), new PermissionPolicy(),
                        java.time.LocalDateTime::now));

        controller.deleteProject(PROJECT_ID);

        assertEquals(PROJECT_ID, projects.lastDeletedProjectId());
        assertFalse(projects.findById(PROJECT_ID).isPresent());
    }

    @Test
    @DisplayName("project controller rejects invalid id, missing project, and non-admin users")
    void projectControllerRejectsInvalidDeleteRequests() {
        AuthFixture admin = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository();
        ProjectController adminController = new ProjectController(
                admin.service(),
                new ProjectService(projects, new FakeIssueRepository(), admin.users(), new PermissionPolicy(),
                        java.time.LocalDateTime::now));
        assertThrows(IllegalArgumentException.class, () -> adminController.deleteProject(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.deleteProject(PROJECT_ID));

        AuthFixture pl = authenticated(Role.PL);
        ProjectController plController = new ProjectController(
                pl.service(),
                new ProjectService(
                        new InMemoryProjectRepository(project(PROJECT_ID)),
                        new FakeIssueRepository(),
                        pl.users(),
                        new PermissionPolicy(),
                        java.time.LocalDateTime::now));
        assertThrows(SecurityException.class, () -> plController.deleteProject(PROJECT_ID));
    }

    @Test
    @DisplayName("assignment controller loads issue and returns status-aware recommendation options")
    void assignmentControllerStartsAssignment() {
        AuthFixture auth = authenticated(Role.PL);
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        PermissionPolicy policy = new PermissionPolicy();
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentRecommendationService recommendationService = new AssignmentRecommendationService(issues, auth.users(), new KNNAssignmentRecommendation());
        AssignmentController controller = new AssignmentController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        issues,
                        auth.users(),
                        policy,
                        recommendationService,
                        java.time.LocalDateTime::now));

        AssignmentOptionsResult options = controller.startAssignment(issue.id());

        assertNotNull(options.devAssigneeCandidates());
        assertNotNull(options.testerVerifierCandidates());
    }

    @Test
    @DisplayName("assignment controller rejects anonymous and missing issue paths")
    void assignmentControllerRejectsInvalidStart() {
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentRecommendationService recommendations = new AssignmentRecommendationService(
                issues, new FakeUserRepository(), new KNNAssignmentRecommendation());
        PermissionPolicy policy = new PermissionPolicy();

        AssignmentController anonymousController = new AssignmentController(
                anonymousAuth(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        issues,
                        new FakeUserRepository(),
                        policy,
                        recommendations,
                        java.time.LocalDateTime::now));
        assertThrows(SecurityException.class, () -> anonymousController.startAssignment(issue.id()));

        AssignmentController plController = new AssignmentController(
                authenticated(Role.PL).service(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        new FakeIssueRepository(),
                        new FakeUserRepository(),
                        policy,
                        recommendations,
                        java.time.LocalDateTime::now));
        assertThrows(IllegalArgumentException.class, () -> plController.startAssignment(issue.id()));
    }

    @Test
    @DisplayName("account controller creates account through service after auth")
    void accountControllerCreatesAccount() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository();
        FakeIssueRepository issues = new FakeIssueRepository();
        PasswordHasher hasher = new PasswordHasher();
        AccountController controller = new AccountController(
                auth.service(),
                new AccountService(new PermissionPolicy(), auth.users(), projects, issues, hasher,
                        java.time.LocalDateTime::now));

        UserResult result = controller.createAccount("newdev", "New Dev", "pass123", Role.DEV);

        assertEquals("newdev", result.loginId());
        assertEquals(Role.DEV, result.role());
        assertTrue(result.active());
    }

    @Test
    @DisplayName("account controller updates, renames, changes role, activates, and deactivates")
    void accountControllerManagesAccounts() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User target = user("target1", Role.DEV);
        auth.users().save(target);
        InMemoryProjectRepository projects = new InMemoryProjectRepository();
        FakeIssueRepository issues = new FakeIssueRepository();
        PasswordHasher hasher = new PasswordHasher();
        AccountController controller = new AccountController(
                auth.service(),
                new AccountService(new PermissionPolicy(), auth.users(), projects, issues, hasher,
                        java.time.LocalDateTime::now));

        UserResult renamed = controller.renameAccount("target1", "Renamed");
        assertEquals("Renamed", renamed.name());

        UserResult roleChanged = controller.changeAccountRole("target1", Role.TESTER);
        assertEquals(Role.TESTER, roleChanged.role());

        UserResult deactivated = controller.deactivateAccount("target1");
        assertFalse(deactivated.active());

        UserResult activated = controller.activateAccount("target1");
        assertTrue(activated.active());
    }

    @Test
    @DisplayName("account controller rejects anonymous users")
    void accountControllerRejectsAnonymous() {
        AccountController controller = new AccountController(
                anonymousAuth(),
                new AccountService(new PermissionPolicy(), new FakeUserRepository(),
                        new InMemoryProjectRepository(), new FakeIssueRepository(), new PasswordHasher(),
                        java.time.LocalDateTime::now));

        assertThrows(SecurityException.class,
                () -> controller.createAccount("x", "X", "pass", Role.DEV));
    }

    @Test
    @DisplayName("controllers keep DCD layer dependencies injectable")
    void controllersAcceptLayerDependencies() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeUserRepository users = auth.users();
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
        FakeIssueRepository issues = new FakeIssueRepository(issue(301L, PROJECT_ID, IssueStatus.NEW));
        PermissionPolicy policy = new PermissionPolicy();
        Clock clock = java.time.LocalDateTime::now;

        assertDoesNotThrow(() -> new AccountController(
                auth.service(),
                new AccountService(policy, users, projects, issues, new PasswordHasher(), clock)));
        assertDoesNotThrow(() -> new IssueController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.IssueService(
                        projects,
                        issues,
                        new FakeIssueDependencyRepository(),
                        new FakeCommentRepository(),
                        new FakeIssueHistoryRepository(),
                        users,
                        policy,
                        clock)));
        assertDoesNotThrow(() -> new IssueStateController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.IssueStateService(issues,
                        new FakeIssueDependencyRepository(), users, policy, clock,
                        ControllerCoverageTest::nextCommentId)));
    }

    private static String nextCommentId() {
        return "COMMENT-test-" + java.util.UUID.randomUUID();
    }

    private static AuthFixture authenticated(Role role) {
        User user = user(role.name().toLowerCase(Locale.ROOT), role);
        SessionStore sessionStore = new SessionStore();
        sessionStore.start(user.getLoginId());
        FakeUserRepository users = new FakeUserRepository(user);
        return new AuthFixture(
                new AuthenticationService(users, new PasswordHasher(), sessionStore),
                users,
                user);
    }

    private static AuthenticationService anonymousAuth() {
        return new AuthenticationService(new FakeUserRepository(), new PasswordHasher(), new SessionStore());
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private static Project project(long projectId) {
        return Project.fromPersistence(projectId, "project-" + projectId, "demo project", "admin", NOW, NOW);
    }

    private static Issue issue(long id, long projectId, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                projectId,
                "Issue " + id,
                "Controller test issue",
                user("reporter", Role.DEV))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .updatedAt(NOW)
                .priority(Priority.MAJOR)
                .status(status));
    }

    private static Issue copyWithStatus(Issue issue, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                issue.projectId(),
                issue.title(),
                issue.description(),
                user(issue.reporterId(), Role.DEV))
                .id(issue.id())
                .issueId(issue.getIssueId())
                .reportedDate(issue.reportedDate())
                .updatedAt(NOW.plusMinutes(1))
                .priority(issue.priority())
                .status(status));
    }

    private static StatisticsReport report() {
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 1);
        Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
        priorityCounts.put(Priority.MAJOR, 1);
        return StatisticsReportTestFactory.create(
                statusCounts,
                priorityCounts,
                List.of(DailyIssueCount.create(LocalDate.of(2026, 5, 19), 1)),
                List.of(MonthlyIssueCount.create(YearMonth.of(2026, 5), 1)));
    }

    private record AuthFixture(AuthenticationService service, FakeUserRepository users, User user) {
    }

    private static final class FakeIssueRepository implements IssueRepository {

        private final Map<Long, Issue> issuesById = new LinkedHashMap<>();
        private String lastChangedBy;
        private String lastRestoreMessage;
        private int lastPurgeLimit;
        private long lastPurgedIssueId;

        private FakeIssueRepository(Issue... issues) {
            for (Issue issue : issues) {
                issuesById.put(issue.id(), issue);
            }
        }

        @Override
        public Optional<Issue> findById(long issueId) {
            return Optional.ofNullable(issuesById.get(issueId));
        }

        @Override
        public List<Issue> findAllById(List<Long> issueIds) {
            return issueIds.stream()
                    .filter(issuesById::containsKey)
                    .map(issuesById::get)
                    .toList();
        }

        @Override
        public List<Issue> findByProject(long projectId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .toList();
        }

        @Override
        public List<Issue> findDeletedByProject(long projectId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .filter(issue -> issue.status() == IssueStatus.DELETED)
                    .toList();
        }

        @Override
        public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
            return new ArrayList<>(issuesById.values());
        }

        @Override
        public boolean existsByProjectIdAndTitle(long projectId, String title) {
            return issuesById.values().stream()
                    .anyMatch(issue -> issue.projectId() == projectId && issue.title().equals(title));
        }

        @Override
        public boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId) {
            return issuesById.values().stream()
                    .anyMatch(issue -> issue.id() != excludedIssueId
                            && issue.projectId() == projectId
                            && issue.title().equals(title));
        }

        @Override
        public boolean existsByResponsibleUser(String userLoginId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.status() == IssueStatus.ASSIGNED || issue.status() == IssueStatus.FIXED)
                    .anyMatch(issue -> userLoginId.equals(issue.assigneeId())
                            || userLoginId.equals(issue.verifierId()));
        }

        @Override
        public boolean existsActiveAssignmentByProjectAndUser(long projectId, String loginId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .filter(issue -> issue.status() == IssueStatus.ASSIGNED || issue.status() == IssueStatus.FIXED)
                    .anyMatch(issue -> loginId.equals(issue.assigneeId()) || loginId.equals(issue.verifierId()));
        }

        @Override
        public Issue save(Issue issue) {
            issuesById.put(issue.id(), issue);
            return issue;
        }

        @Override
        public Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
            lastChangedBy = changedById;
            Issue deleted = copyWithStatus(findById(issueId).orElseThrow(), IssueStatus.DELETED);
            issuesById.put(issueId, deleted);
            return deleted;
        }

        @Override
        public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
            lastChangedBy = changedById;
            lastRestoreMessage = message;
            Issue restored = copyWithStatus(findById(issueId).orElseThrow(), IssueStatus.NEW);
            issuesById.put(issueId, restored);
            return restored;
        }

        @Override
        public int purgeDeletedById(long issueId) {
            Issue issue = issuesById.get(issueId);
            if (issue == null || issue.status() != IssueStatus.DELETED) {
                return 0;
            }
            issuesById.remove(issueId);
            lastPurgedIssueId = issueId;
            return 1;
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            lastPurgeLimit = maxDeletedIssues;
            return 2;
        }

    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> usersByLoginId = new LinkedHashMap<>();

        private FakeUserRepository(User... users) {
            for (User user : users) {
                usersByLoginId.put(user.getLoginId(), user);
            }
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return Optional.ofNullable(usersByLoginId.get(loginId));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersByLoginId.values());
        }

        @Override
        public List<User> findByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public List<User> findActiveByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(User::isActive)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            usersByLoginId.put(user.getLoginId(), user);
            return user;
        }

        @Override
        public void activate(String loginId) {
            LocalDateTime now = LocalDateTime.now();
            findByLoginId(loginId).ifPresent(user -> user.activate(now));
        }

        @Override
        public void deactivate(String loginId) {
            LocalDateTime now = LocalDateTime.now();
            findByLoginId(loginId).ifPresent(user -> user.deactivate(now));
        }
    }

    private static final class FakeDashboardSummaryRepository implements DashboardSummaryRepository {

        private final List<DashboardProjectSnapshot> allProjectSummaries;
        private final List<DashboardProjectSnapshot> participantProjectSummaries;

        private FakeDashboardSummaryRepository(
                List<DashboardProjectSnapshot> allProjectSummaries,
                List<DashboardProjectSnapshot> participantProjectSummaries) {
            this.allProjectSummaries = List.copyOf(allProjectSummaries);
            this.participantProjectSummaries = List.copyOf(participantProjectSummaries);
        }

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return allProjectSummaries;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return participantProjectSummaries;
        }
    }

    private static final class FakeStatisticsRepository implements StatisticsRepository {

        private StatisticsReport report = report();
        private long reportProjectId;
        private LocalDate dailyFrom;
        private YearMonth monthlyTo;

        @Override
        public Map<IssueStatus, Integer> countByStatus(long projectId) {
            return report.statusCounts();
        }

        @Override
        public Map<Priority, Integer> countByPriority(long projectId) {
            return report.priorityCounts();
        }

        @Override
        public List<DailyIssueCount> countReportedIssuesByDay(long projectId) {
            return report.dailyCounts();
        }

        @Override
        public List<DailyIssueCount> countReportedIssuesByDay(
                long projectId,
                LocalDate fromInclusive,
                LocalDate toInclusive) {
            return report.dailyCounts();
        }

        @Override
        public List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId) {
            return report.monthlyCounts();
        }

        @Override
        public List<MonthlyIssueCount> countReportedIssuesByMonth(
                long projectId,
                YearMonth fromInclusive,
                YearMonth toInclusive) {
            return report.monthlyCounts();
        }

        @Override
        public StatisticsReport buildReport(
                long projectId,
                LocalDate dailyFromInclusive,
                LocalDate dailyToInclusive,
                YearMonth monthlyFromInclusive,
                YearMonth monthlyToInclusive) {
            reportProjectId = projectId;
            dailyFrom = dailyFromInclusive;
            monthlyTo = monthlyToInclusive;
            return report;
        }
    }

    private static final class FakeCommentRepository implements CommentRepository {

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.empty();
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return List.of();
        }

        @Override
        public Comment save(Comment comment) {
            return comment;
        }

        @Override
        public Comment saveAndRecordIssueChange(Comment comment, IssueHistory history) {
            return comment;
        }

        @Override
        public void deleteGeneralById(long issueId, long commentId, String writerLoginId) {
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
        }
    }

    private static final class FakeAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        private List<AssignmentCandidate> devCandidates = List.of();
        private List<AssignmentCandidate> testerCandidates = List.of();
        private long lastDevProjectId;
        private long lastTesterProjectId;

        @Override
        public List<AssignmentCandidate> findDevAssigneeCandidates(long projectId) {
            lastDevProjectId = projectId;
            return devCandidates;
        }

        @Override
        public List<AssignmentCandidate> findTesterVerifierCandidates(long projectId) {
            lastTesterProjectId = projectId;
            return testerCandidates;
        }
    }
}
