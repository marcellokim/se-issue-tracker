package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.AssignmentOptions;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
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
    @DisplayName("statistics controller delegates report query after auth and range validation")
    void statisticsControllerDelegatesReportQuery() {
        AuthFixture auth = authenticated(Role.DEV);
        FakeStatisticsRepository statistics = new FakeStatisticsRepository();
        StatisticsReport expectedReport = report();
        statistics.report = expectedReport;

        StatisticsController controller = new StatisticsController(
                auth.service(),
                new PermissionPolicy(),
                statistics);

        StatisticsReport actualReport = controller.viewStatistics(
                PROJECT_ID,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6));

        assertSame(expectedReport, actualReport);
        assertEquals(PROJECT_ID, statistics.reportProjectId);
        assertEquals(LocalDate.of(2026, 5, 1), statistics.dailyFrom);
        assertEquals(YearMonth.of(2026, 6), statistics.monthlyTo);
    }

    @Test
    @DisplayName("statistics controller rejects missing login and reversed ranges")
    void statisticsControllerRejectsInvalidAccessOrRange() {
        StatisticsController anonymousController = new StatisticsController(
                anonymousAuth(),
                new PermissionPolicy(),
                new FakeStatisticsRepository());
        assertThrows(SecurityException.class, () -> anonymousController.viewStatistics(PROJECT_ID));

        StatisticsController controller = new StatisticsController(
                authenticated(Role.PL).service(),
                new PermissionPolicy(),
                new FakeStatisticsRepository());
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
        FakeIssueRepository issues = new FakeIssueRepository(activeIssue, deletedIssue);
        DeletedIssueController controller = new DeletedIssueController(
                auth.service(),
                new PermissionPolicy(),
                issues,
                new Clock());

        List<Issue> deletedIssues = controller.viewDeletedIssues(PROJECT_ID);
        Issue softDeleted = controller.deleteIssue(activeIssue.id(), "remove from demo");
        Issue restored = controller.restoreIssue(deletedIssue.id(), "restore for demo");
        int purged = controller.purgeOverflow(PROJECT_ID);

        assertEquals(List.of(deletedIssue), deletedIssues);
        assertEquals(IssueStatus.DELETED, softDeleted.status());
        assertEquals(IssueStatus.NEW, restored.status());
        assertEquals("pl", issues.lastChangedBy);
        assertEquals("restore for demo", issues.lastRestoreMessage);
        assertEquals(2, purged);
        assertEquals(30, issues.lastPurgeLimit);
    }

    @Test
    @DisplayName("deleted issue controller rejects anonymous, ADMIN, and missing issue paths")
    void deletedIssueControllerRejectsInvalidAccess() {
        FakeIssueRepository issues = new FakeIssueRepository(issue(101L, PROJECT_ID, IssueStatus.NEW));
        DeletedIssueController anonymousController = new DeletedIssueController(
                anonymousAuth(),
                new PermissionPolicy(),
                issues,
                new Clock());
        assertThrows(SecurityException.class, () -> anonymousController.viewDeletedIssues(PROJECT_ID));

        DeletedIssueController adminController = new DeletedIssueController(
                authenticated(Role.ADMIN).service(),
                new PermissionPolicy(),
                issues,
                new Clock());
        assertThrows(SecurityException.class, () -> adminController.deleteIssue(101L, "admin cannot delete"));

        DeletedIssueController plController = new DeletedIssueController(
                authenticated(Role.PL).service(),
                new PermissionPolicy(),
                issues,
                new Clock());
        assertThrows(IllegalArgumentException.class, () -> plController.restoreIssue(999L, "missing"));
    }

    @Test
    @DisplayName("project controller deletes an existing project only for ADMIN")
    void projectControllerDeletesProjectForAdmin() {
        FakeProjectRepository projects = new FakeProjectRepository(project(PROJECT_ID));
        ProjectController controller = new ProjectController(
                authenticated(Role.ADMIN).service(),
                new PermissionPolicy(),
                projects,
                new FakeUserRepository(),
                new Clock());

        controller.deleteProject(PROJECT_ID);

        assertEquals(PROJECT_ID, projects.deletedProjectId);
        assertFalse(projects.findById(PROJECT_ID).isPresent());
    }

    @Test
    @DisplayName("project controller rejects invalid id, missing project, and non-admin users")
    void projectControllerRejectsInvalidDeleteRequests() {
        FakeProjectRepository projects = new FakeProjectRepository();
        ProjectController adminController = new ProjectController(
                authenticated(Role.ADMIN).service(),
                new PermissionPolicy(),
                projects,
                new FakeUserRepository(),
                new Clock());
        assertThrows(IllegalArgumentException.class, () -> adminController.deleteProject(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.deleteProject(PROJECT_ID));

        ProjectController plController = new ProjectController(
                authenticated(Role.PL).service(),
                new PermissionPolicy(),
                new FakeProjectRepository(project(PROJECT_ID)),
                new FakeUserRepository(),
                new Clock());
        assertThrows(SecurityException.class, () -> plController.deleteProject(PROJECT_ID));
    }

    @Test
    @DisplayName("assignment controller loads issue and returns status-aware recommendation options")
    void assignmentControllerStartsAssignment() {
        AuthFixture auth = authenticated(Role.PL);
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        FakeAssignmentRecommendationRepository recommendations = new FakeAssignmentRecommendationRepository();
        recommendations.devCandidates = List.of(new AssignmentCandidate(user("dev1", Role.DEV), 5));
        recommendations.testerCandidates = List.of(new AssignmentCandidate(user("tester1", Role.TESTER), 3));
        PermissionPolicy policy = new PermissionPolicy();
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentRecommendationService recommendationService = new AssignmentRecommendationService(recommendations);
        AssignmentController controller = new AssignmentController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        issues,
                        auth.users(),
                        policy,
                        recommendationService,
                        new Clock()));

        AssignmentOptions options = controller.startAssignment(issue.id());

        assertEquals(1, options.devAssigneeCandidates().size());
        assertEquals(1, options.testerVerifierCandidates().size());
        assertEquals(PROJECT_ID, recommendations.lastDevProjectId);
        assertEquals(PROJECT_ID, recommendations.lastTesterProjectId);
    }

    @Test
    @DisplayName("assignment controller rejects anonymous and missing issue paths")
    void assignmentControllerRejectsInvalidStart() {
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentRecommendationService recommendations = new AssignmentRecommendationService(
                new FakeAssignmentRecommendationRepository());
        PermissionPolicy policy = new PermissionPolicy();

        AssignmentController anonymousController = new AssignmentController(
                anonymousAuth(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        issues,
                        new FakeUserRepository(),
                        policy,
                        recommendations,
                        new Clock()));
        assertThrows(SecurityException.class, () -> anonymousController.startAssignment(issue.id()));

        AssignmentController plController = new AssignmentController(
                authenticated(Role.PL).service(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        new FakeIssueRepository(),
                        new FakeUserRepository(),
                        policy,
                        recommendations,
                        new Clock()));
        assertThrows(IllegalArgumentException.class, () -> plController.startAssignment(issue.id()));
    }

    @Test
    @DisplayName("stub controllers keep DCD layer dependencies injectable")
    void stubControllersAcceptLayerDependencies() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeUserRepository users = auth.users();
        FakeProjectRepository projects = new FakeProjectRepository(project(PROJECT_ID));
        FakeIssueRepository issues = new FakeIssueRepository(issue(301L, PROJECT_ID, IssueStatus.NEW));
        PermissionPolicy policy = new PermissionPolicy();
        Clock clock = new Clock();

        assertDoesNotThrow(() -> new AccountController(auth.service(), policy, users, new PasswordHasher()));
        assertDoesNotThrow(() -> new IssueController(auth.service(), policy, projects, issues, users, clock));
        assertDoesNotThrow(() -> new IssueStateController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.IssueStateService(issues, users, policy, clock)));
    }

    private static AuthFixture authenticated(Role role) {
        User user = user(role.name().toLowerCase(Locale.ROOT), role);
        SessionStore sessionStore = new SessionStore();
        sessionStore.startSession(user);
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
        return User.create(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    private static Project project(long projectId) {
        return Project.create(projectId, "project-" + projectId, "demo project", "admin", NOW, NOW);
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
        return new StatisticsReport(
                statusCounts,
                priorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 19), 1)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 1)));
    }

    private record AuthFixture(AuthenticationService service, FakeUserRepository users, User user) {
    }

    private static final class FakeIssueRepository implements IssueRepository {

        private final Map<Long, Issue> issuesById = new LinkedHashMap<>();
        private String lastChangedBy;
        private String lastRestoreMessage;
        private int lastPurgeLimit;

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
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            lastPurgeLimit = maxDeletedIssues;
            return 2;
        }

        @Override
        public void purge(long issueId) {
            issuesById.remove(issueId);
        }
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projectsById = new LinkedHashMap<>();
        private long deletedProjectId;

        private FakeProjectRepository(Project... projects) {
            for (Project project : projects) {
                projectsById.put(project.id(), project);
            }
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return Optional.ofNullable(projectsById.get(projectId));
        }

        @Override
        public Optional<Project> findByName(String name) {
            return projectsById.values().stream()
                    .filter(project -> project.name().equals(name))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projectsById.values());
        }

        @Override
        public Project save(Project project) {
            projectsById.put(project.id(), project);
            return project;
        }

        @Override
        public void deleteById(long projectId) {
            deletedProjectId = projectId;
            projectsById.remove(projectId);
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return List.of();
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> usersByLoginId = new LinkedHashMap<>();

        private FakeUserRepository(User... users) {
            for (User user : users) {
                usersByLoginId.put(user.loginId(), user);
            }
        }

        @Override
        public Optional<User> findById(String userId) {
            return findByLoginId(userId);
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
        public List<User> findActiveByRole(long projectId, Role role) {
            return usersByLoginId.values().stream()
                    .filter(User::active)
                    .filter(user -> user.role() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            usersByLoginId.put(user.loginId(), user);
            return user;
        }

        @Override
        public void deactivate(String loginId) {
            findByLoginId(loginId).ifPresent(User::deactivate);
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
