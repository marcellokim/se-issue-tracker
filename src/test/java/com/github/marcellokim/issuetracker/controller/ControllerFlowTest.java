package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.NOW;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.dashboardProject;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.issue;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.persistedIssue;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeAssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeCommentRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeDashboardSummaryRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeIssueRepository;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeUserRepository;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentResult;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueStateResult;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.support.SequentialIssueIdProvider;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Controller flow")
class ControllerFlowTest {

    @Test
    @DisplayName("admin logs in and lands on dashboard data")
    void adminDashboardFlow() {
        PasswordHasher hasher = new PasswordHasher();
        User admin = storedUser("admin", Role.ADMIN, hasher);
        var users = new FakeUserRepository(admin);
        AuthenticationService auth = new AuthenticationService(users, hasher, new SessionStore());
        AuthenticationController authentication = new AuthenticationController(auth);
        DashboardController dashboard = new DashboardController(
                auth,
                new DashboardSummaryService(
                        new FakeDashboardSummaryRepository(
                                List.of(dashboardProject(PROJECT_ID, "project-one", 2)),
                                List.of()),
                        users,
                        new PermissionPolicy()));

        assertTrue(authentication.login("admin", "secret").success());

        assertEquals(List.of("project-one"), dashboard.viewProjects().stream()
                .map(project -> project.projectName())
                .toList());
        assertEquals(List.of("admin"), dashboard.viewUsers().stream()
                .map(UserResult::loginId)
                .toList());
    }

    @Test
    @DisplayName("dev opens a project and sees project issues")
    void devProjectFlow() {
        PasswordHasher hasher = new PasswordHasher();
        User dev = storedUser("dev1", Role.DEV, hasher);
        var users = new FakeUserRepository(dev);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, dev.getLoginId());
        users.attachProjects(projects);
        Issue issue = persistedIssue(1L, "ISSUE-1", dev);
        InMemoryIssueRepository issues = new InMemoryIssueRepository(issue);
        AuthenticationService auth = new AuthenticationService(users, hasher, new SessionStore());
        new AuthenticationController(auth).login(dev.getLoginId(), "secret");
        PermissionPolicy policy = new PermissionPolicy();
        ProjectController projectController = new ProjectController(
                auth,
                new ProjectService(projects, issues, users, policy, () -> NOW));
        IssueController issueController = issueController(auth, projects, issues, users, policy);

        ProjectResult detail = projectController.viewProjectNonAdminDetail(PROJECT_ID);
        List<IssueSummary> projectIssues = issueController.viewProjectIssues(PROJECT_ID);

        assertEquals("project-" + PROJECT_ID, detail.name());
        assertEquals(List.of(issue.id()), projectIssues.stream().map(IssueSummary::id).toList());
    }

    @Test
    @DisplayName("PL assigns an issue and the dev marks it fixed")
    void assignThenFixFlow() {
        PasswordHasher hasher = new PasswordHasher();
        User pl = storedUser("pl", Role.PL, hasher);
        User dev = storedUser("dev1", Role.DEV, hasher);
        User tester = storedUser("tester1", Role.TESTER, hasher);
        var users = new FakeUserRepository(pl, dev, tester);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, pl.getLoginId())
                .withParticipant(PROJECT_ID, dev.getLoginId())
                .withParticipant(PROJECT_ID, tester.getLoginId());
        users.attachProjects(projects);
        Issue issue = issue(201L, PROJECT_ID, IssueStatus.NEW);
        var issues = new FakeIssueRepository(issue);
        PermissionPolicy policy = new PermissionPolicy();

        AuthenticationService plAuth = loggedIn(pl, users, hasher);
        AssignmentController assignment = new AssignmentController(
                plAuth,
                new AssignmentService(
                        issues,
                        users,
                        policy,
                        new AssignmentRecommendationService(
                                new FakeAssignmentRecommendationRepository(),
                                new KNNAssignmentRecommendation()),
                        () -> NOW));

        AssignmentResult assigned = assignment.assignIssue(issue.id(), dev.getLoginId(), tester.getLoginId());

        AuthenticationService devAuth = loggedIn(dev, users, hasher);
        IssueStateController state = new IssueStateController(
                devAuth,
                new IssueStateService(
                        issues,
                        new FakeIssueDependencyRepository(),
                        users,
                        policy,
                        () -> NOW,
                        ControllerTestSupport::nextCommentId));
        IssueStateResult fixed = state.changeStatus(issue.id(), IssueStatus.FIXED, "fixed on my side");

        assertEquals(IssueStatus.ASSIGNED, assigned.status());
        assertEquals(dev.getLoginId(), assigned.assignee().loginId());
        assertEquals(IssueStatus.FIXED, fixed.status());
        assertEquals(dev.getLoginId(), fixed.fixer().loginId());
    }

    private static IssueController issueController(
            AuthenticationService auth,
            InMemoryProjectRepository projects,
            InMemoryIssueRepository issues,
            FakeUserRepository users,
            PermissionPolicy policy) {
        var dependencies = new FakeIssueDependencyRepository();
        IssueService issueService = new IssueService(
                projects,
                issues,
                dependencies,
                new FakeCommentRepository(),
                new FakeIssueHistoryRepository(),
                users,
                policy,
                new SequentialIssueIdProvider(),
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issues,
                dependencies,
                new FakeCommentRepository(),
                users,
                policy);
        return new IssueController(auth, issueService, workflowService);
    }

    private static User storedUser(String loginId, Role role, PasswordHasher hasher) {
        return User.fromPersistence(loginId, loginId, hasher.hash("secret"), role, true, NOW, NOW);
    }

    private static AuthenticationService loggedIn(
            User user,
            FakeUserRepository users,
            PasswordHasher hasher) {
        SessionStore session = new SessionStore();
        session.start(user.getLoginId());
        return new AuthenticationService(users, hasher, session);
    }
}
