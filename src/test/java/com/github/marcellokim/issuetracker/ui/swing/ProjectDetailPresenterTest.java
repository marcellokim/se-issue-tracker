package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing project detail presenter")
class ProjectDetailPresenterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("loads project detail and participants through project controller")
    void loadsProjectDetail() {
        ControllerFixture fixture = controllers(
                List.of(user("pl1", "Project Lead", Role.PL), user("dev1", "Developer", Role.DEV)),
                List.of("pl1", "dev1"));
        RecordingProjectDetailView view = new RecordingProjectDetailView();
        ProjectDetailPresenter presenter = new ProjectDetailPresenter(fixture.projectController(), view);

        presenter.loadProject(1L);

        assertEquals("Alpha", view.projectName());
        assertEquals(List.of("dev1", "pl1"), view.participantIds());
        assertEquals(" ", view.message());
    }

    @Test
    @DisplayName("updates project fields and refreshes detail after success")
    void updatesProjectFields() {
        ControllerFixture fixture = controllers(List.of(), List.of());
        RecordingProjectDetailView view = new RecordingProjectDetailView();
        ProjectDetailPresenter presenter = new ProjectDetailPresenter(fixture.projectController(), view);

        presenter.renameProject(1L, "Renamed");
        presenter.changeProjectDescription(1L, "Updated project");

        assertEquals("Renamed", view.projectName());
        assertEquals("Updated project", view.projectDescription());
        assertEquals("Project description changed: Renamed", view.message());
    }

    @Test
    @DisplayName("adds and removes participants with participant-only refresh")
    void updatesParticipants() {
        ControllerFixture fixture = controllers(
                List.of(user("dev1", "Developer One", Role.DEV), user("tester1", "Tester One", Role.TESTER)),
                List.of("dev1"));
        RecordingProjectDetailView view = new RecordingProjectDetailView();
        ProjectDetailPresenter presenter = new ProjectDetailPresenter(fixture.projectController(), view);
        presenter.loadProject(1L);

        presenter.addProjectParticipant(1L, "tester1");
        assertEquals(List.of("dev1", "tester1"), view.participantIds());
        assertEquals("Participant added: tester1", view.message());

        presenter.removeProjectParticipant(1L, "dev1");
        assertEquals(List.of("tester1"), view.participantIds());
        assertEquals("Participant removed: dev1", view.message());
        assertEquals("Alpha", view.projectName());
    }

    @Test
    @DisplayName("shows controller errors without replacing current detail")
    void showsControllerError() {
        ControllerFixture fixture = controllers(List.of(), List.of());
        RecordingProjectDetailView view = new RecordingProjectDetailView();
        ProjectDetailPresenter presenter = new ProjectDetailPresenter(fixture.projectController(), view);
        presenter.loadProject(1L);

        presenter.renameProject(1L, "Alpha");

        assertEquals("Alpha", view.projectName());
        assertEquals("Project name is same as current name.", view.message());
    }

    private static ControllerFixture controllers(List<User> participants, List<String> participantIds) {
        List<User> usersWithAdmin = java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(user("admin", "Admin", Role.ADMIN)),
                        participants.stream())
                .toList();
        InMemoryUserRepository users = new InMemoryUserRepository(usersWithAdmin.toArray(User[]::new));
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project());
        participantIds.forEach(loginId -> projects.withParticipant(1L, loginId));
        AuthenticationService authentication = new AuthenticationService(
                users,
                new AcceptingPasswordHashing(),
                new SessionStore());
        authentication.login("admin", "password");
        ProjectController projectController = new ProjectController(
                authentication,
                new ProjectService(
                        projects,
                        new InMemoryIssueRepository(),
                        users,
                        new PermissionPolicy(),
                        () -> NOW));
        return new ControllerFixture(projectController);
    }

    private static Project project() {
        return Project.fromPersistence(1L, "Alpha", "Alpha project", "admin", NOW, NOW);
    }

    private static User user(String loginId, String name, Role role) {
        return User.fromPersistence(loginId, name, "stored-password", role, true, NOW, NOW);
    }

    private record ControllerFixture(ProjectController projectController) {
    }

    private static final class RecordingProjectDetailView implements ProjectDetailView {

        private ProjectAdminDetail detail;
        private List<ProjectMemberResult> participants = List.of();
        private String message = " ";

        @Override
        public void showDetail(ProjectAdminDetail detail) {
            this.detail = detail;
            this.participants = detail.participants();
            this.message = " ";
        }

        @Override
        public void showParticipants(List<ProjectMemberResult> participants) {
            this.participants = List.copyOf(participants);
            this.message = " ";
        }

        @Override
        public void showMessage(String message, boolean error) {
            this.message = message;
        }

        private String projectName() {
            return detail.project().name();
        }

        private String projectDescription() {
            return detail.project().description();
        }

        private List<String> participantIds() {
            return participants.stream()
                    .map(ProjectMemberResult::userId)
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
