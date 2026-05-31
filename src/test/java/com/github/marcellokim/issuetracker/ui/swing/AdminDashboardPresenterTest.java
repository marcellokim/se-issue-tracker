package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing admin dashboard presenter")
class AdminDashboardPresenterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads projects and users through dashboard controller")
    void loadsProjectsAndUsers() {
        User admin = user("admin", Role.ADMIN, true);
        DashboardProjectSummary expectedProject = projectSummary(1L, "Alpha", 3, 7);
        UserResult expectedUser = UserResult.from(admin);
        DashboardController controller = dashboardController(
                admin,
                List.of(snapshot(expectedProject)),
                new InMemoryUserRepository(admin));
        RecordingAdminDashboardView view = new RecordingAdminDashboardView();
        AdminDashboardPresenter presenter = new AdminDashboardPresenter(controller, view);

        presenter.load();

        assertEquals(List.of(expectedProject), view.projects());
        assertEquals(List.of(expectedUser), view.users());
        assertNull(view.error());
    }

    @Test
    @DisplayName("shows controller failure without rethrowing")
    void showsControllerFailure() {
        DashboardController controller = dashboardControllerWithoutLogin();
        RecordingAdminDashboardView view = new RecordingAdminDashboardView();
        AdminDashboardPresenter presenter = new AdminDashboardPresenter(controller, view);

        presenter.load();

        assertEquals("Login is required.", view.error());
        assertTrue(view.projects().isEmpty());
        assertTrue(view.users().isEmpty());
    }

    private static DashboardController dashboardController(
            User currentUser,
            List<DashboardProjectSnapshot> projects,
            InMemoryUserRepository users) {
        AuthenticationService authentication = new AuthenticationService(
                users,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login(currentUser.getLoginId(), "password");
        return new DashboardController(
                authentication,
                new DashboardSummaryService(
                        new FakeDashboardSummaryRepository(projects),
                        users,
                        new PermissionPolicy()));
    }

    private static DashboardController dashboardControllerWithoutLogin() {
        InMemoryUserRepository users = new InMemoryUserRepository(user("admin", Role.ADMIN, true));
        return new DashboardController(
                new AuthenticationService(users, new AcceptingPasswordHashing(), new SessionStore()),
                new DashboardSummaryService(
                        new FakeDashboardSummaryRepository(List.of()),
                        users,
                        new PermissionPolicy()));
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private static DashboardProjectSummary projectSummary(
            long projectId,
            String projectName,
            int memberCount,
            int visibleIssueCount) {
        return new DashboardProjectSummary(
                projectId,
                projectName,
                "Demo project",
                memberCount,
                1,
                1,
                1,
                visibleIssueCount,
                Map.of(IssueStatus.NEW, visibleIssueCount));
    }

    private static DashboardProjectSnapshot snapshot(DashboardProjectSummary summary) {
        return new DashboardProjectSnapshot(
                summary.projectId(),
                summary.projectName(),
                summary.projectDescription(),
                summary.memberCount(),
                summary.projectLeaderCount(),
                summary.developerCount(),
                summary.testerCount(),
                summary.visibleIssueCount(),
                summary.statusCounts());
    }

    private static final class RecordingAdminDashboardView implements AdminDashboardView {

        private List<DashboardProjectSummary> projects = List.of();
        private List<UserResult> users = List.of();
        private String error;

        @Override
        public void showDashboard(List<DashboardProjectSummary> projects, List<UserResult> users) {
            this.projects = List.copyOf(projects);
            this.users = List.copyOf(users);
        }

        @Override
        public void showError(String message) {
            this.error = message;
        }

        private List<DashboardProjectSummary> projects() {
            return projects;
        }

        private List<UserResult> users() {
            return users;
        }

        private String error() {
            return error;
        }
    }

    private record FakeDashboardSummaryRepository(
            List<DashboardProjectSnapshot> projects) implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return projects;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return List.of();
        }
    }

    private static final class AcceptingPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return password;
        }

        @Override
        public boolean matches(String password, String storedCredential) {
            return true;
        }

        @Override
        public boolean isHashed(String storedCredential) {
            return true;
        }

        @Override
        public String saltOf(String storedCredential) {
            return "salt";
        }

        @Override
        public String hashOf(String storedCredential) {
            return storedCredential;
        }
    }
}
