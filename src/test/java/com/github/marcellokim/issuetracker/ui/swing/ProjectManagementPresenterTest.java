package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardProjectSummary;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project management presenter")
class ProjectManagementPresenterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads projects through dashboard controller")
    void loadsProjects() {
        ControllerFixture fixture = controllers(project(1L, "Alpha", "Alpha project"));
        RecordingProjectManagementView view = new RecordingProjectManagementView();
        ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                fixture.dashboardController(),
                fixture.projectController(),
                view);

        presenter.loadProjects();

        assertEquals(List.of("Alpha"), view.projectNames());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("creates project and refreshes projects after success")
    void createsProjectAndRefreshesProjects() {
        ControllerFixture fixture = controllers();
        RecordingProjectManagementView view = new RecordingProjectManagementView();
        ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                fixture.dashboardController(),
                fixture.projectController(),
                view);

        presenter.createProject(new ProjectCreateRequest("Beta", "Beta project"));

        assertEquals(List.of("Beta"), view.projectNames());
        assertEquals("Project created: Beta", view.message());
    }

    @Test
    @DisplayName("renames and changes project description through controller")
    void updatesProjectFields() {
        ControllerFixture fixture = controllers(project(1L, "Alpha", "Alpha project"));
        RecordingProjectManagementView view = new RecordingProjectManagementView();
        ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                fixture.dashboardController(),
                fixture.projectController(),
                view);

        presenter.renameProject(1L, "Renamed");
        presenter.changeProjectDescription(1L, "Updated project");

        DashboardProjectSummary updated = view.projects().get(0);
        assertEquals("Renamed", updated.projectName());
        assertEquals("Updated project", updated.projectDescription());
        assertEquals("Project description changed: Renamed", view.message());
    }

    @Test
    @DisplayName("deletes project through controller and refreshes projects")
    void deletesProject() {
        ControllerFixture fixture = controllers(project(1L, "Alpha", "Alpha project"));
        RecordingProjectManagementView view = new RecordingProjectManagementView();
        ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                fixture.dashboardController(),
                fixture.projectController(),
                view);

        presenter.deleteProject(1L);

        assertEquals(List.of(), view.projectNames());
        assertEquals("Project deleted: 1", view.message());
    }

    @Test
    @DisplayName("shows controller errors without replacing the current project list")
    void showsControllerError() {
        ControllerFixture fixture = controllers(project(1L, "Alpha", "Alpha project"));
        RecordingProjectManagementView view = new RecordingProjectManagementView();
        ProjectManagementPresenter presenter = new ProjectManagementPresenter(
                fixture.dashboardController(),
                fixture.projectController(),
                view);
        presenter.loadProjects();

        presenter.renameProject(1L, "Alpha");

        assertEquals(List.of("Alpha"), view.projectNames());
        assertEquals("Project name is same as current name.", view.message());
    }

    private static ControllerFixture controllers(Project... projects) {
        InMemoryUserRepository users = new InMemoryUserRepository(user("admin", Role.ADMIN, true));
        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository(projects);
        AuthenticationService authentication = new AuthenticationService(
                users,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login("admin", "password");
        PermissionPolicy permissionPolicy = new PermissionPolicy();
        DashboardController dashboardController = new DashboardController(
                authentication,
                new DashboardSummaryService(
                        new ProjectBackedDashboardSummaryRepository(projectRepository),
                        users,
                        permissionPolicy));
        ProjectController projectController = new ProjectController(
                authentication,
                new ProjectService(
                        projectRepository,
                        new InMemoryIssueRepository(),
                        users,
                        permissionPolicy,
                        () -> NOW));
        return new ControllerFixture(dashboardController, projectController);
    }

    private static Project project(long id, String name, String description) {
        return Project.fromPersistence(id, name, description, "admin", NOW, NOW);
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private record ControllerFixture(
            DashboardController dashboardController,
            ProjectController projectController) {
    }

    private static final class RecordingProjectManagementView implements ProjectManagementView {

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

        private List<DashboardProjectSummary> projects() {
            return projects;
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

    private record ProjectBackedDashboardSummaryRepository(
            InMemoryProjectRepository projects) implements DashboardSummaryRepository {

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return projects.findAll().stream()
                    .map(this::snapshot)
                    .toList();
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return List.of();
        }

        private DashboardProjectSnapshot snapshot(Project project) {
            int memberCount = projects.findParticipants(project.getId()).size();
            return new DashboardProjectSnapshot(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    memberCount,
                    0,
                    0,
                    0,
                    0,
                    Map.of(IssueStatus.NEW, 0));
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
