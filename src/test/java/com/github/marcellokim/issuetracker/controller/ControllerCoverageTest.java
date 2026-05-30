package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentResult;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentResult;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueResult;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueStateResult;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.StatisticsReportResult;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Controller layer")
class ControllerCoverageTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 10, 0);
    private static final long PROJECT_ID = 10L;

    @Test
    @DisplayName("controller coverage test tracks every public controller method")
    void controllerCoverageTracksEveryPublicControllerMethod() {
        Map<Class<?>, Set<String>> expected = Map.ofEntries(
                Map.entry(AccountController.class, Set.of(
                        "activateAccount(String)",
                        "changeAccountRole(String,Role)",
                        "createAccount(String,String,String,Role)",
                        "deactivateAccount(String)",
                        "renameAccount(String,String)")),
                Map.entry(AssignmentController.class, Set.of(
                        "assignIssue(long,String,String)",
                        "changeVerifier(long,String)",
                        "reassignIssue(long,String)",
                        "startAssignment(long)")),
                Map.entry(AuthenticationController.class, Set.of(
                        "login(String,String)",
                        "logout()")),
                Map.entry(DashboardController.class, Set.of(
                        "viewProjects()",
                        "viewUsers()")),
                Map.entry(DeletedIssueController.class, Set.of(
                        "deleteIssue(long,String)",
                        "purgeDeletedIssue(long)",
                        "purgeOverflow(long)",
                        "restoreIssue(long,String)",
                        "viewDeletedIssues(long)")),
                Map.entry(IssueController.class, Set.of(
                        "addComment(long,String)",
                        "addDependency(long,long)",
                        "canDeleteComment(long,long)",
                        "canRegisterIssue(long)",
                        "canUpdateComment(long,long)",
                        "changePriority(long,Priority)",
                        "deleteComment(long,long)",
                        "registerIssue(long,String,String,Priority)",
                        "removeDependency(long,long)",
                        "searchIssues(long,String,IssueStatus,Priority)",
                        "searchIssues(long,String,IssueStatus,Priority,String,String,String,LocalDateTime,LocalDateTime)",
                        "updateComment(long,long,String)",
                        "updateIssue(long,String,String)",
                        "viewAvailableActions(long)",
                        "viewComments(long)",
                        "viewIssueDetail(long)",
                        "viewProjectDependencies(long)",
                        "viewRelatedProjectIssues(long)")),
                Map.entry(IssueStateController.class, Set.of(
                        "changeStatus(long,IssueStatus,String)")),
                Map.entry(ProjectController.class, Set.of(
                        "addProjectParticipant(long,String)",
                        "changeProjectDescription(long,String)",
                        "createProject(String,String)",
                        "deleteProject(long)",
                        "removeProjectParticipant(long,String)",
                        "renameProject(long,String)",
                        "viewProjectAdminDetail(long)",
                        "viewProjectNonAdminDetail(long)",
                        "viewProjectParticipants(long)")),
                Map.entry(StatisticsController.class, Set.of(
                        "viewStatistics(long)",
                        "viewStatistics(long,LocalDate,LocalDate,YearMonth,YearMonth)")));

        expected.forEach((controllerType, signatures) ->
                assertEquals(signatures, publicMethodSignatures(controllerType), controllerType.getSimpleName()));
    }

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
        auth.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, auth.user().getLoginId()));
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

        StatisticsReportResult defaultRangeReport = controller.viewStatistics(PROJECT_ID);
        assertEquals(expectedReport.statusCounts(), defaultRangeReport.statusCounts());
        assertEquals(PROJECT_ID, statistics.reportProjectId);
        assertNull(statistics.dailyFrom);
        assertNull(statistics.monthlyTo);
    }

    @Test
    @DisplayName("statistics controller rejects missing login and reversed ranges")
    void statisticsControllerRejectsInvalidAccessOrRange() {
        StatisticsController anonymousController = new StatisticsController(
                anonymousAuth(),
                new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(),
                        new FakeUserRepository()));
        assertThrows(SecurityException.class, () -> anonymousController.viewStatistics(PROJECT_ID));

        AuthFixture pl = authenticated(Role.PL);
        pl.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, pl.user().getLoginId()));
        StatisticsController controller = new StatisticsController(
                pl.service(),
                new StatisticsService(new PermissionPolicy(), new FakeStatisticsRepository(), pl.users()));
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
        auth.users().attachProjects(new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, auth.user().getLoginId()));
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
    @DisplayName("issue controller registers, reads, searches, updates, and reprioritizes issues")
    void issueControllerCoversIssueReadAndWriteMethods() {
        User dev = user("dev1", Role.DEV);
        Issue issue = persistedIssue(1L, "ISSUE-1", dev);
        IssueController devController = issueController(dev, issue);

        IssueResult created = devController.registerIssue(PROJECT_ID, "New issue", "Registration path",
                Priority.MAJOR);
        assertNotNull(created.issueId());
        assertEquals(IssueStatus.NEW, created.status());
        assertEquals("New issue", created.title());
        assertTrue(devController.canRegisterIssue(PROJECT_ID));

        IssueDetailResult detail = devController.viewIssueDetail(issue.id());
        assertEquals(issue.id(), detail.id());
        assertTrue(detail.availableActions().contains("UPDATE_ISSUE"));

        List<IssueSummary> simpleSearch = devController.searchIssues(PROJECT_ID, null, null, null);
        assertEquals(List.of(issue.id(), created.id()), simpleSearch.stream().map(IssueSummary::id).toList());

        List<IssueSummary> filteredSearch = devController.searchIssues(
                PROJECT_ID,
                "Issue",
                IssueStatus.NEW,
                Priority.MAJOR,
                dev.getLoginId(),
                null,
                null,
                NOW.minusDays(1),
                NOW.plusDays(1));
        assertEquals(List.of(issue.id(), created.id()), filteredSearch.stream().map(IssueSummary::id).toList());

        List<IssueSummary> relatedIssues = devController.viewRelatedProjectIssues(PROJECT_ID);
        assertEquals(List.of(issue.id(), created.id()), relatedIssues.stream().map(IssueSummary::id).toList());

        IssueResult updated = devController.updateIssue(issue.id(), "Updated title", "Updated description");
        assertEquals("Updated title", updated.title());

        User pl = user("pl1", Role.PL);
        IssueController plController = issueController(pl, persistedIssue(2L, "ISSUE-2", dev));
        IssueResult priorityChanged = plController.changePriority(2L, Priority.CRITICAL);
        assertEquals(Priority.CRITICAL, priorityChanged.priority());
    }

    @Test
    @DisplayName("issue controller adds, views, updates, deletes, and checks comment actions")
    void issueControllerCoversCommentMethods() {
        User dev = user("dev1", Role.DEV);
        Issue issue = persistedIssue(1L, "ISSUE-1", dev);
        FakeCommentRepository comments = new FakeCommentRepository(Comment.fromPersistence(
                100L,
                issue.id(),
                dev.getLoginId(),
                "Original comment",
                CommentPurpose.GENERAL,
                NOW,
                NOW));
        IssueController controller = issueController(dev, comments, issue);

        CommentResult added = controller.addComment(issue.id(), "Confirmed this bug");
        assertEquals("Confirmed this bug", added.content());
        assertEquals(dev.getLoginId(), added.writerLoginId());

        List<CommentResult> viewed = controller.viewComments(issue.id());
        assertEquals(List.of("100", "101"), viewed.stream().map(CommentResult::commentId).toList());

        assertTrue(controller.canUpdateComment(issue.id(), 100L));
        assertTrue(controller.canDeleteComment(issue.id(), 100L));

        CommentResult updated = controller.updateComment(issue.id(), 100L, "Edited comment");
        assertEquals("Edited comment", updated.content());

        controller.deleteComment(issue.id(), 100L);
        assertFalse(comments.findById(100L).isPresent());
    }

    @Test
    @DisplayName("issue controller adds, views, removes dependencies and exposes workflow actions")
    void issueControllerCoversDependencyAndWorkflowMethods() {
        User pl = user("pl1", Role.PL);
        Issue blockingIssue = persistedIssue(1L, "ISSUE-1", user("reporter1", Role.DEV));
        Issue blockedIssue = persistedIssue(2L, "ISSUE-2", user("reporter2", Role.DEV));
        IssueController controller = issueController(pl, blockingIssue, blockedIssue);

        DependencyResult dependency = controller.addDependency(blockingIssue.id(), blockedIssue.id());
        assertNotNull(dependency.dependencyId());
        assertEquals(blockingIssue.id(), dependency.blockingIssueId());
        assertEquals(blockedIssue.id(), dependency.blockedIssueId());

        List<DependencyResult> dependencies = controller.viewProjectDependencies(PROJECT_ID);
        assertEquals(List.of(dependency.dependencyId()),
                dependencies.stream().map(DependencyResult::dependencyId).toList());

        assertTrue(controller.viewAvailableActions(blockedIssue.id()).canAddDependency());

        controller.removeDependency(blockingIssue.id(), blockedIssue.id());
        assertEquals(List.of(), controller.viewProjectDependencies(PROJECT_ID));
    }

    @Test
    @DisplayName("issue controller rejects anonymous write paths")
    void issueControllerRejectsAnonymousUsers() {
        IssueController controller = unauthenticatedIssueController(persistedIssue(1L, "ISSUE-1", user("dev1", Role.DEV)));

        assertThrows(SecurityException.class,
                () -> controller.registerIssue(PROJECT_ID, "Bug", "desc", Priority.MAJOR));
        assertThrows(SecurityException.class,
                () -> controller.addComment(1L, "comment"));
        assertThrows(SecurityException.class,
                () -> controller.deleteComment(1L, 100L));
    }

    @Test
    @DisplayName("project controller covers admin project reads and mutations")
    void projectControllerCoversAdminProjectMethods() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        projects.withParticipant(PROJECT_ID, "dev1");
        FakeUserRepository users = new FakeUserRepository(auth.user(), user("dev1", Role.DEV));
        ProjectController controller = projectController(auth, projects, users);

        List<ProjectMemberResult> participants = controller.viewProjectParticipants(PROJECT_ID);
        ProjectAdminDetail adminDetail = controller.viewProjectAdminDetail(PROJECT_ID);
        assertEquals(List.of("dev1"), participants.stream().map(ProjectMemberResult::userId).toList());
        assertEquals("project-one", adminDetail.project().name());
        assertThrows(NoSuchMethodException.class, () -> ProjectAdminDetail.class.getMethod("issues"));

        ProjectResult created = controller.createProject(" project-two ", "second project");
        assertEquals("project-two", created.name());
        assertEquals("second project", created.description());

        ProjectResult renamed = controller.renameProject(PROJECT_ID, " project-renamed ");
        assertEquals("project-renamed", renamed.name());

        ProjectResult changed = controller.changeProjectDescription(PROJECT_ID, " updated description ");
        assertEquals("updated description", changed.description());

        controller.deleteProject(PROJECT_ID);
        assertEquals(PROJECT_ID, projects.lastDeletedProjectId());
        assertFalse(projects.findById(PROJECT_ID).isPresent());
    }

    @Test
    @DisplayName("project controller covers non-admin detail and participant mutations")
    void projectControllerCoversNonAdminDetailAndParticipants() {
        AuthFixture admin = authenticated(Role.ADMIN);
        User dev = user("dev", Role.DEV);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        FakeUserRepository users = new FakeUserRepository(admin.user(), dev, user("tester1", Role.TESTER));
        ProjectController adminController = projectController(admin, projects, users);

        adminController.addProjectParticipant(PROJECT_ID, dev.getLoginId());
        assertEquals(List.of(dev.getLoginId()), projects.participantIds(PROJECT_ID));

        AuthFixture devAuth = authenticated(Role.DEV);
        ProjectController devController = projectController(devAuth, projects, new FakeUserRepository(devAuth.user()));
        ProjectResult nonAdminDetail = devController.viewProjectNonAdminDetail(PROJECT_ID);
        assertEquals("project-one", nonAdminDetail.name());

        adminController.removeProjectParticipant(PROJECT_ID, dev.getLoginId());
        assertEquals(List.of(), projects.participantIds(PROJECT_ID));
    }

    @Test
    @DisplayName("project controller rejects invalid ids and unauthenticated users")
    void projectControllerRejectsInvalidRequests() {
        AuthFixture admin = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        ProjectController adminController = projectController(admin, projects, new FakeUserRepository(admin.user()));

        assertThrows(IllegalArgumentException.class, () -> adminController.viewProjectNonAdminDetail(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.viewProjectParticipants(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.viewProjectAdminDetail(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.renameProject(0L, "renamed"));
        assertThrows(IllegalArgumentException.class, () -> adminController.changeProjectDescription(0L, "changed"));
        assertThrows(IllegalArgumentException.class, () -> adminController.deleteProject(0L));
        assertThrows(IllegalArgumentException.class, () -> adminController.addProjectParticipant(0L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> adminController.removeProjectParticipant(0L, "dev1"));

        ProjectController anonymousController = new ProjectController(
                anonymousAuth(),
                projectService(projects, new FakeUserRepository(user("dev1", Role.DEV))));
        assertThrows(SecurityException.class, () -> anonymousController.createProject("blocked", "blocked"));
    }

    @Test
    @DisplayName("project controller rejects non-admin management attempts")
    void projectControllerRejectsNonAdminManagement() {
        for (Role role : List.of(Role.PL, Role.DEV, Role.TESTER)) {
            AuthFixture auth = authenticated(role);
            InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
            FakeUserRepository users = new FakeUserRepository(auth.user(), user("dev1", Role.DEV));
            ProjectController controller = projectController(auth, projects, users);

            assertThrows(SecurityException.class, () -> controller.viewProjectNonAdminDetail(PROJECT_ID));
            assertThrows(SecurityException.class, () -> controller.viewProjectParticipants(PROJECT_ID));
            assertThrows(SecurityException.class, () -> controller.viewProjectAdminDetail(PROJECT_ID));
            assertThrows(SecurityException.class, () -> controller.createProject("new-project", "blocked"));
            assertThrows(SecurityException.class, () -> controller.renameProject(PROJECT_ID, "blocked-project"));
            assertThrows(SecurityException.class, () -> controller.changeProjectDescription(PROJECT_ID, "blocked"));
            assertThrows(SecurityException.class, () -> controller.deleteProject(PROJECT_ID));
            assertThrows(SecurityException.class, () -> controller.addProjectParticipant(PROJECT_ID, "dev1"));
            assertThrows(SecurityException.class, () -> controller.removeProjectParticipant(PROJECT_ID, "dev1"));
        }
    }

    @Test
    @DisplayName("project controller rejects invalid project data")
    void projectControllerRejectsInvalidProjectData() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(
                project(PROJECT_ID, "project-one"),
                project(PROJECT_ID + 1, "project-two"));
        ProjectController controller = projectController(auth, projects, new FakeUserRepository(auth.user()));

        assertThrows(IllegalArgumentException.class, () -> controller.createProject(" ", "blank"));
        assertThrows(IllegalArgumentException.class, () -> controller.renameProject(PROJECT_ID, " "));
        assertThrows(IllegalArgumentException.class, () -> controller.createProject("project-three", null));
        assertThrows(IllegalArgumentException.class, () -> controller.createProject("project-three", " "));
        assertThrows(IllegalArgumentException.class, () -> controller.changeProjectDescription(PROJECT_ID, null));
        assertThrows(IllegalArgumentException.class, () -> controller.changeProjectDescription(PROJECT_ID, " "));
        assertThrows(IllegalArgumentException.class, () -> controller.createProject("project-one", "duplicate"));
        assertThrows(IllegalArgumentException.class, () -> controller.renameProject(PROJECT_ID + 1, "project-one"));
    }

    @Test
    @DisplayName("project controller validates participant add rules")
    void projectControllerValidatesParticipantAddRules() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User activeDev = user("dev1", Role.DEV);
        User inactiveTester = inactiveUser("tester1", Role.TESTER);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        FakeUserRepository users = new FakeUserRepository(auth.user(), activeDev, inactiveTester);
        ProjectController controller = projectController(auth, projects, users);
        String activeDevLoginId = activeDev.getLoginId();
        String adminLoginId = auth.user().getLoginId();
        String inactiveTesterLoginId = inactiveTester.getLoginId();

        controller.addProjectParticipant(PROJECT_ID, activeDevLoginId);

        assertEquals(List.of(activeDevLoginId), projects.participantIds(PROJECT_ID));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addProjectParticipant(PROJECT_ID, activeDevLoginId));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addProjectParticipant(PROJECT_ID, adminLoginId));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addProjectParticipant(PROJECT_ID, inactiveTesterLoginId));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addProjectParticipant(PROJECT_ID, "missing"));
        assertThrows(IllegalArgumentException.class,
                () -> controller.addProjectParticipant(404L, activeDevLoginId));
    }

    @Test
    @DisplayName("project controller allows only one project lead")
    void projectControllerAllowsOnlyOneProjectLead() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User pl1 = user("pl1", Role.PL);
        User pl2 = user("pl2", Role.PL);
        User inactivePl = inactiveUser("pl3", Role.PL);

        InMemoryProjectRepository firstProject = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        ProjectController firstController = projectController(
                auth,
                firstProject,
                new FakeUserRepository(auth.user(), pl1, pl2));
        String pl1LoginId = pl1.getLoginId();
        String pl2LoginId = pl2.getLoginId();
        String inactivePlLoginId = inactivePl.getLoginId();

        firstController.addProjectParticipant(PROJECT_ID, pl1LoginId);
        assertThrows(IllegalArgumentException.class,
                () -> firstController.addProjectParticipant(PROJECT_ID, pl2LoginId));
        assertEquals(List.of(pl1LoginId), firstProject.participantIds(PROJECT_ID));

        InMemoryProjectRepository secondProject = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        secondProject.withParticipant(PROJECT_ID, inactivePlLoginId);
        ProjectController secondController = projectController(
                auth,
                secondProject,
                new FakeUserRepository(auth.user(), inactivePl, pl2));
        assertThrows(IllegalArgumentException.class,
                () -> secondController.addProjectParticipant(PROJECT_ID, pl2LoginId));
        assertEquals(List.of(inactivePlLoginId), secondProject.participantIds(PROJECT_ID));
    }

    @Test
    @DisplayName("project controller validates participant remove rules")
    void projectControllerValidatesParticipantRemoveRules() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User dev = user("dev1", Role.DEV);
        String devLoginId = dev.getLoginId();
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"))
                .withParticipant(PROJECT_ID, devLoginId);
        ProjectController controller = projectController(auth, projects, new FakeUserRepository(auth.user(), dev));

        controller.removeProjectParticipant(PROJECT_ID, devLoginId);

        assertEquals(List.of(), projects.participantIds(PROJECT_ID));
        assertThrows(IllegalArgumentException.class,
                () -> controller.removeProjectParticipant(PROJECT_ID, devLoginId));
        assertThrows(IllegalArgumentException.class,
                () -> controller.removeProjectParticipant(404L, devLoginId));
        assertThrows(IllegalArgumentException.class,
                () -> controller.removeProjectParticipant(PROJECT_ID, " "));
    }

    @Test
    @DisplayName("project controller blocks active assignee or verifier removal")
    void projectControllerBlocksActiveAssigneeOrVerifierRemoval() {
        assertParticipantRemovalBlockedFor(IssueStatus.ASSIGNED, user("dev1", Role.DEV), null);
        assertParticipantRemovalBlockedFor(IssueStatus.FIXED, user("dev1", Role.DEV), null);
        assertParticipantRemovalBlockedFor(IssueStatus.ASSIGNED, null, user("tester1", Role.TESTER));
        assertParticipantRemovalBlockedFor(IssueStatus.FIXED, null, user("tester1", Role.TESTER));
    }

    @Test
    @DisplayName("project controller allows resolved assignee and verifier removal")
    void projectControllerAllowsResolvedAssigneeAndVerifierRemoval() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User assignee = user("dev1", Role.DEV);
        User verifier = user("tester1", Role.TESTER);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"))
                .withParticipant(PROJECT_ID, assignee.getLoginId())
                .withParticipant(PROJECT_ID, verifier.getLoginId());
        InMemoryIssueRepository issues = new InMemoryIssueRepository(
                issueWithAssigneeAndVerifier(101L, PROJECT_ID, IssueStatus.RESOLVED, assignee, verifier));
        ProjectController controller = projectController(
                auth,
                projects,
                new FakeUserRepository(auth.user(), assignee, verifier),
                issues);

        controller.removeProjectParticipant(PROJECT_ID, assignee.getLoginId());
        controller.removeProjectParticipant(PROJECT_ID, verifier.getLoginId());

        assertEquals(List.of(), projects.participantIds(PROJECT_ID));
    }

    @Test
    @DisplayName("assignment controller loads issue and returns status-aware recommendation options")
    void assignmentControllerStartsAssignment() {
        AuthFixture auth = authenticated(Role.PL);
        User dev = user("dev1", Role.DEV);
        User tester = user("tester1", Role.TESTER);
        auth.users().save(dev);
        auth.users().save(tester);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, auth.user().getLoginId())
                .withParticipant(PROJECT_ID, dev.getLoginId())
                .withParticipant(PROJECT_ID, tester.getLoginId());
        auth.users().attachProjects(projects);
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        Issue completedIssue = issueWithCompletedOwners(202L, PROJECT_ID, dev, tester);
        PermissionPolicy policy = new PermissionPolicy();
        FakeIssueRepository issues = new FakeIssueRepository(issue, completedIssue);
        FakeAssignmentRecommendationRepository recommendations = new FakeAssignmentRecommendationRepository();
        recommendations.addResolvedIssue(new AssignmentRecommendationRepository.IssueRecommendationData(
                completedIssue.title(), completedIssue.description(), dev.getLoginId(), tester.getLoginId()));
        recommendations.addCandidate(dev);
        recommendations.addCandidate(tester);
        AssignmentRecommendationService recommendationService = new AssignmentRecommendationService(
                recommendations, new KNNAssignmentRecommendation());
        AssignmentController controller = new AssignmentController(
                auth.service(),
                new com.github.marcellokim.issuetracker.service.AssignmentService(
                        issues,
                        auth.users(),
                        policy,
                        recommendationService,
                        java.time.LocalDateTime::now));

        AssignmentOptionsResult options = controller.startAssignment(issue.id());

        assertEquals(List.of(dev.getLoginId()), options.devAssigneeCandidates().stream()
                .map(candidate -> candidate.loginId())
                .toList());
        assertEquals(List.of(tester.getLoginId()), options.testerVerifierCandidates().stream()
                .map(candidate -> candidate.loginId())
                .toList());
        assertEquals(List.of(dev.getLoginId()), options.allDevAssignees().stream()
                .map(candidate -> candidate.loginId())
                .toList());
        assertEquals(List.of(tester.getLoginId()), options.allTesterVerifiers().stream()
                .map(candidate -> candidate.loginId())
                .toList());
        assertEquals("recommended by similarity", options.devAssigneeCandidates().getFirst().reason());
        assertEquals(1, options.devAssigneeCandidates().getFirst().completedIssueCount());
    }

    @Test
    @DisplayName("assignment controller assigns, reassigns, and changes verifier")
    void assignmentControllerCoversAssignmentMutations() {
        AuthFixture auth = authenticated(Role.PL);
        User dev1 = user("dev1", Role.DEV);
        User dev2 = user("dev2", Role.DEV);
        User tester1 = user("tester1", Role.TESTER);
        User tester2 = user("tester2", Role.TESTER);
        auth.users().save(dev1);
        auth.users().save(dev2);
        auth.users().save(tester1);
        auth.users().save(tester2);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
        projects.withParticipant(PROJECT_ID, auth.user().getLoginId());
        projects.withParticipant(PROJECT_ID, dev1.getLoginId());
        projects.withParticipant(PROJECT_ID, dev2.getLoginId());
        projects.withParticipant(PROJECT_ID, tester1.getLoginId());
        projects.withParticipant(PROJECT_ID, tester2.getLoginId());
        auth.users().attachProjects(projects);
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentController controller = assignmentController(auth, issues);

        AssignmentResult assigned = controller.assignIssue(issue.id(), dev1.getLoginId(), tester1.getLoginId());
        assertEquals(IssueStatus.ASSIGNED, assigned.status());
        assertEquals(dev1.getLoginId(), assigned.assignee().loginId());
        assertEquals(tester1.getLoginId(), assigned.verifier().loginId());

        AssignmentResult reassigned = controller.reassignIssue(issue.id(), dev2.getLoginId());
        assertEquals(dev2.getLoginId(), reassigned.assignee().loginId());

        issues.save(issueWithAssigneeAndVerifier(issue.id(), PROJECT_ID, IssueStatus.FIXED, dev2, tester1));
        AssignmentResult verifierChanged = controller.changeVerifier(issue.id(), tester2.getLoginId());
        assertEquals(tester2.getLoginId(), verifierChanged.verifier().loginId());
    }

    @Test
    @DisplayName("assignment controller rejects anonymous and missing issue paths")
    void assignmentControllerRejectsInvalidStart() {
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        AssignmentRecommendationService recommendations = new AssignmentRecommendationService(
                new FakeAssignmentRecommendationRepository(), new KNNAssignmentRecommendation());
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
    @DisplayName("issue state controller changes issue status")
    void issueStateControllerChangesStatus() {
        AuthFixture auth = authenticated(Role.DEV);
        User tester = user("tester1", Role.TESTER);
        auth.users().save(tester);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID));
        projects.withParticipant(PROJECT_ID, auth.user().getLoginId());
        projects.withParticipant(PROJECT_ID, tester.getLoginId());
        auth.users().attachProjects(projects);
        Issue issue = issueWithAssigneeAndVerifier(301L, PROJECT_ID, IssueStatus.ASSIGNED, auth.user(), tester);
        FakeIssueRepository issues = new FakeIssueRepository(issue);
        IssueStateController controller = new IssueStateController(
                auth.service(),
                new IssueStateService(
                        issues,
                        new FakeIssueDependencyRepository(),
                        auth.users(),
                        new PermissionPolicy(),
                        () -> NOW,
                        ControllerCoverageTest::nextCommentId));

        IssueStateResult result = controller.changeStatus(issue.id(), IssueStatus.FIXED, "fixed in controller test");

        assertEquals(IssueStatus.FIXED, result.status());
        assertEquals(auth.user().getLoginId(), result.fixer().loginId());
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

    private static Set<String> publicMethodSignatures(Class<?> controllerType) {
        return Arrays.stream(controllerType.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.isBridge())
                .filter(method -> !method.isSynthetic())
                .map(ControllerCoverageTest::methodSignature)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String methodSignature(Method method) {
        return method.getName() + "(" + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",")) + ")";
    }

    private static IssueController issueController(User user, Issue... issues) {
        return issueController(user, new FakeCommentRepository(), issues);
    }

    private static IssueController issueController(User user, FakeCommentRepository comments, Issue... issues) {
        FakeUserRepository users = new FakeUserRepository(user);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, user.getLoginId());
        users.attachProjects(projects);
        SessionStore sessionStore = new SessionStore();
        sessionStore.start(user.getLoginId());
        AuthenticationService authService = new AuthenticationService(users, new PasswordHasher(), sessionStore);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        PermissionPolicy policy = new PermissionPolicy();
        IssueService issueService = new IssueService(
                projects,
                issueRepository,
                dependencies,
                comments,
                new FakeIssueHistoryRepository(),
                users,
                policy,
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issueRepository,
                dependencies,
                comments,
                users,
                policy);
        return new IssueController(authService, issueService, workflowService);
    }

    private static IssueController unauthenticatedIssueController(Issue... issues) {
        FakeUserRepository users = new FakeUserRepository(user("dev1", Role.DEV));
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, "dev1");
        users.attachProjects(projects);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        PermissionPolicy policy = new PermissionPolicy();
        IssueService issueService = new IssueService(
                projects,
                issueRepository,
                dependencies,
                new FakeCommentRepository(),
                new FakeIssueHistoryRepository(),
                users,
                policy,
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issueRepository,
                dependencies,
                new FakeCommentRepository(),
                users,
                policy);
        return new IssueController(anonymousAuth(), issueService, workflowService);
    }

    private static Issue persistedIssue(long id, String issueId, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(
                PROJECT_ID,
                "Issue " + id,
                "Controller test issue " + id,
                reporter)
                .id(id)
                .issueId(issueId)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(NOW));
    }

    private static ProjectController projectController(
            AuthFixture auth,
            InMemoryProjectRepository projects,
            FakeUserRepository users) {
        return projectController(auth, projects, users, new InMemoryIssueRepository());
    }

    private static ProjectController projectController(
            AuthFixture auth,
            InMemoryProjectRepository projects,
            FakeUserRepository users,
            InMemoryIssueRepository issues) {
        return new ProjectController(auth.service(), projectService(projects, users, issues));
    }

    private static ProjectService projectService(
            InMemoryProjectRepository projects,
            FakeUserRepository users) {
        return projectService(projects, users, new InMemoryIssueRepository());
    }

    private static ProjectService projectService(
            InMemoryProjectRepository projects,
            FakeUserRepository users,
            InMemoryIssueRepository issues) {
        users.attachProjects(projects);
        return new ProjectService(
                projects,
                issues,
                users,
                new PermissionPolicy(),
                () -> NOW);
    }

    private static Project project(long projectId, String name) {
        return Project.fromPersistence(projectId, name, "description", "admin", NOW, NOW);
    }

    private static AssignmentController assignmentController(AuthFixture auth, FakeIssueRepository issues) {
        return new AssignmentController(
                auth.service(),
                new AssignmentService(
                        issues,
                        auth.users(),
                        new PermissionPolicy(),
                        new AssignmentRecommendationService(new FakeAssignmentRecommendationRepository(), new KNNAssignmentRecommendation()),
                        () -> NOW));
    }

    private static Issue issueWithAssigneeAndVerifier(
            long id,
            long projectId,
            IssueStatus status,
            User assignee,
            User verifier) {
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
                .status(status)
                .assignee(assignee)
                .verifier(verifier));
    }

    private static Issue issueWithCompletedOwners(long id, long projectId, User fixer, User resolver) {
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
                .status(IssueStatus.RESOLVED)
                .fixer(fixer)
                .resolver(resolver));
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

    private static User inactiveUser(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, false, NOW, NOW);
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

    private static void assertParticipantRemovalBlockedFor(
            IssueStatus status,
            User assignee,
            User verifier) {
        AuthFixture auth = authenticated(Role.ADMIN);
        User participant = assignee == null ? verifier : assignee;
        String participantLoginId = participant.getLoginId();
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"))
                .withParticipant(PROJECT_ID, participantLoginId);
        InMemoryIssueRepository issues = new InMemoryIssueRepository(
                issueWithAssigneeAndVerifier(101L, PROJECT_ID, status, assignee, verifier));
        ProjectController controller = projectController(
                auth,
                projects,
                new FakeUserRepository(auth.user(), participant),
                issues);

        assertThrows(IllegalArgumentException.class,
                () -> controller.removeProjectParticipant(PROJECT_ID, participantLoginId));
        assertEquals(List.of(participantLoginId), projects.participantIds(PROJECT_ID));
    }

    private record AuthFixture(AuthenticationService service, FakeUserRepository users, User user) {
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
        public boolean hasCurrentIssueResponsibility(String userLoginId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.status() == IssueStatus.ASSIGNED || issue.status() == IssueStatus.FIXED)
                    .anyMatch(issue -> userLoginId.equals(issue.assigneeId())
                            || userLoginId.equals(issue.verifierId()));
        }

        @Override
        public boolean hasCurrentIssueResponsibility(long projectId, String loginId) {
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
        private InMemoryProjectRepository projects;

        private FakeUserRepository(User... users) {
            for (User user : users) {
                usersByLoginId.put(user.getLoginId(), user);
            }
        }

        private void attachProjects(InMemoryProjectRepository projects) {
            this.projects = Objects.requireNonNull(projects, "projects");
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
            if (projects == null) {
                return usersByLoginId.values().stream()
                        .filter(user -> user.getRole() == role)
                        .toList();
            }

            return projects.findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .map(usersByLoginId::get)
                    .filter(Objects::nonNull)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public List<User> findActiveByRole(long projectId, Role role) {
            if (projects == null) {
                return usersByLoginId.values().stream()
                        .filter(User::isActive)
                        .filter(user -> user.getRole() == role)
                        .toList();
            }

            return projects.findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .map(usersByLoginId::get)
                    .filter(Objects::nonNull)
                    .filter(User::isActive)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public boolean existsActiveProjectMember(long projectId, String loginId) {
            User user = usersByLoginId.get(loginId);
            if (user == null || !user.isActive()) {
                return false;
            }
            return projects == null || projects.findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .anyMatch(loginId::equals);
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

    private static final class FakeStatisticsRepository implements StatisticsRepository {

        private StatisticsReport report = report();
        private long reportProjectId;
        private LocalDate dailyFrom;
        private YearMonth monthlyTo;

        @Override
        public StatisticsReport calculateProjectStatistics(
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

        private final Map<Long, Comment> comments = new LinkedHashMap<>();
        private long nextId = 100L;

        private FakeCommentRepository(Comment... comments) {
            for (Comment comment : comments) {
                save(comment);
            }
        }

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.ofNullable(comments.get(commentId));
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return comments.values().stream()
                    .filter(comment -> comment.issueId() == issueId)
                    .toList();
        }

        @Override
        public Comment save(Comment comment) {
            if (comment.id() != 0L) {
                comments.put(comment.id(), comment);
                nextId = Math.max(nextId, comment.id() + 1L);
                return comment;
            }
            Comment saved = Comment.fromPersistence(
                    nextId++,
                    comment.issueId(),
                    comment.writerId(),
                    comment.content(),
                    comment.purpose(),
                    comment.createdDate(),
                    comment.updatedDate());
            comments.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
            return save(comment);
        }

        @Override
        public void deleteGeneralById(long issueId, long commentId, String writerLoginId) {
            Comment comment = comments.get(commentId);
            if (comment == null
                    || comment.issueId() != issueId
                    || !Objects.equals(comment.writerId(), writerLoginId)
                    || comment.purpose() != CommentPurpose.GENERAL) {
                throw new IllegalArgumentException(
                        "Comment was not deleted because it does not exist, is not owned by the writer, "
                                + "or is not a GENERAL comment.");
            }
            comments.remove(commentId);
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            deleteGeneralById(issueId, commentId, writerLoginId);
        }
    }

    private static final class FakeAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        private final List<IssueRecommendationData> resolvedIssues = new ArrayList<>();
        private final List<User> candidates = new ArrayList<>();

        void addResolvedIssue(IssueRecommendationData data) { resolvedIssues.add(data); }
        void addCandidate(User user) { candidates.add(user); }

        @Override
        public List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId) {
            return List.copyOf(resolvedIssues);
        }

        @Override
        public Optional<User> findCandidateByLoginId(String loginId) {
            return candidates.stream().filter(u -> u.getLoginId().equals(loginId)).findFirst();
        }

        @Override
        public List<User> findActiveDevCandidates(long projectId) {
            return candidates.stream().filter(u -> u.getRole() == Role.DEV && u.isActive()).toList();
        }

        @Override
        public List<User> findActiveTesterCandidates(long projectId) {
            return candidates.stream().filter(u -> u.getRole() == Role.TESTER && u.isActive()).toList();
        }
    }
}
