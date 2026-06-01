package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project list presenter")
class ProjectListPresenterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads projects through dashboard controller")
    void loadsProjectsThroughDashboardController() {
        RecordingProjectListView view = new RecordingProjectListView();
        ProjectListPresenter presenter = new ProjectListPresenter(
                dashboardController(true, projectSnapshot(1L, "Alpha", 3, 7)),
                view);

        presenter.loadProjects();

        assertEquals(List.of("Alpha"), view.projectNames());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("shows controller errors without replacing current project list")
    void showsControllerErrorsWithoutReplacingCurrentProjectList() {
        RecordingProjectListView view = new RecordingProjectListView();
        view.showProjects(List.of(projectSummary(1L, "Alpha")));
        ProjectListPresenter presenter = new ProjectListPresenter(
                dashboardController(false, projectSnapshot(2L, "Beta", 1, 0)),
                view);

        presenter.loadProjects();

        assertEquals(List.of("Alpha"), view.projectNames());
        assertEquals("Login is required.", view.message());
    }

    private static DashboardController dashboardController(boolean loggedIn, DashboardProjectSnapshot... projects) {
        var users = new InMemoryUserRepository(user("dev1", Role.DEV, true));
        AuthenticationService authentication = new AuthenticationService(users, new AcceptingPasswordHashing(), new SessionStore());
        if (loggedIn) {
            authentication.login("dev1", "password");
        }
        return new DashboardController(
                authentication,
                new DashboardSummaryService(
                        new FixedDashboardSummaryRepository(List.of(projects)),
                        users,
                        new PermissionPolicy()));
    }

    private static DashboardProjectSnapshot projectSnapshot(
            long projectId,
            String projectName,
            int memberCount,
            int visibleIssueCount) {
        return new DashboardProjectSnapshot(
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

    private static DashboardProjectSummary projectSummary(long projectId, String projectName) {
        return new DashboardProjectSummary(
                projectId,
                projectName,
                "Demo project",
                1,
                0,
                1,
                0,
                0,
                Map.of(IssueStatus.NEW, 0));
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private record FixedDashboardSummaryRepository(
            List<DashboardProjectSnapshot> projects) implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return projects;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return projects;
        }
    }

    private static final class RecordingProjectListView implements ProjectListView {

        private List<DashboardProjectSummary> projects = List.of();
        private String message = " ";

        @Override
        public void showProjects(List<DashboardProjectSummary> projects) {
            this.projects = List.copyOf(projects);
            this.message = " ";
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
        }

        private List<String> projectNames() {
            return projects.stream()
                    .map(DashboardProjectSummary::projectName)
                    .toList();
        }

        private String message() {
            return message;
        }
    }

    private static final class AcceptingPasswordHashing implements PasswordHashing {

        @Override
        public String hash(String password) {
            return "hashed-" + password;
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
