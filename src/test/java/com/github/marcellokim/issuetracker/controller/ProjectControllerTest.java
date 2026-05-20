package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project controller")
class ProjectControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("invalid project ids are rejected")
    void invalidProjectIdsAreRejected() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        FakeUserRepository users = new FakeUserRepository(auth.user(), active("dev1", Role.DEV));
        ProjectController controller = controller(auth, projects, users);

        assertThrows(IllegalArgumentException.class, () -> controller.deleteProject(0L));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(0L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> controller.removeProjectParticipant(0L, "dev1"));
    }

    @Test
    @DisplayName("ADMIN creates projects")
    void adminCreatesProjects() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository();
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()));

        Project created = controller.createProject(" project-alpha ", "first project");

        assertTrue(created.id() > 0L);
        assertEquals("project-alpha", created.name());
        assertEquals("first project", created.description());
        assertEquals("admin", created.managedById());
        assertNotNull(created.createdDate());
        assertNotNull(created.updatedAt());
        assertTrue(projects.findById(created.id()).isPresent());
    }

    @Test
    @DisplayName("ADMIN deletes projects")
    void adminDeletesProjects() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()));

        controller.deleteProject(1L);

        assertFalse(projects.findById(1L).isPresent());
    }

    @Test
    @DisplayName("only ADMIN can manage projects")
    void onlyAdminCanManageProjects() {
        for (Role role : List.of(Role.PL, Role.DEV, Role.TESTER)) {
            AuthFixture auth = authenticated(role);
            FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
            FakeUserRepository users = new FakeUserRepository(auth.user(), active("dev1", Role.DEV));
            ProjectController controller = controller(auth, projects, users);

            assertThrows(SecurityException.class, () -> controller.createProject("new-project", "blocked"));
            assertThrows(SecurityException.class, () -> controller.deleteProject(1L));
            assertThrows(SecurityException.class, () -> controller.addProjectParticipant(1L, "dev1"));
            assertThrows(SecurityException.class, () -> controller.removeProjectParticipant(1L, "dev1"));
        }
    }

    @Test
    @DisplayName("anonymous users cannot manage projects")
    void anonymousUsersCannotManageProjects() {
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        FakeUserRepository users = new FakeUserRepository(active("dev1", Role.DEV));
        ProjectController controller = new ProjectController(
                anonymousAuth(),
                new PermissionPolicy(),
                projects,
                users,
                new Clock());

        assertThrows(SecurityException.class, () -> controller.createProject("new-project", "blocked"));
        assertThrows(SecurityException.class, () -> controller.deleteProject(1L));
        assertThrows(SecurityException.class, () -> controller.addProjectParticipant(1L, "dev1"));
        assertThrows(SecurityException.class, () -> controller.removeProjectParticipant(1L, "dev1"));
    }

    @Test
    @DisplayName("blank project names are rejected")
    void blankProjectNamesAreRejected() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository();
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()));

        assertThrows(IllegalArgumentException.class, () -> controller.createProject(" ", "blank"));
    }

    @Test
    @DisplayName("duplicate project names are rejected")
    void duplicateProjectNamesAreRejected() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()));

        assertThrows(IllegalArgumentException.class, () -> controller.createProject("project-one", "duplicate"));
    }

    @Test
    @DisplayName("participant add requires an existing project, active user, and no duplicate membership")
    void participantAddRequiresValidActiveNonDuplicateUser() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User activeDeveloper = active("dev1", Role.DEV);
        User inactiveTester = inactive("tester1", Role.TESTER);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        FakeUserRepository users = new FakeUserRepository(auth.user(), activeDeveloper, inactiveTester);
        ProjectController controller = controller(auth, projects, users);

        controller.addProjectParticipant(1L, "dev1");

        assertEquals(List.of("dev1"), projects.participantIds(1L));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(1L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(1L, "admin"));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(1L, "tester1"));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(1L, "missing"));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(404L, "dev1"));
    }

    @Test
    @DisplayName("participant add allows only one PL per project")
    void participantAddRejectsSecondProjectLead() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User pl1 = active("pl1", Role.PL);
        User pl2 = active("pl2", Role.PL);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        projects.addParticipant(1L, "pl1");
        FakeUserRepository users = new FakeUserRepository(auth.user(), pl1, pl2);
        ProjectController controller = controller(auth, projects, users);

        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(1L, "pl2"));
        assertEquals(List.of("pl1"), projects.participantIds(1L));
    }

    @Test
    @DisplayName("participant remove requires current membership")
    void participantRemoveRequiresCurrentMembership() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        projects.addParticipant(1L, "dev1");
        FakeUserRepository users = new FakeUserRepository(auth.user(), active("dev1", Role.DEV));
        ProjectController controller = controller(auth, projects, users);

        controller.removeProjectParticipant(1L, "dev1");

        assertEquals(List.of(), projects.participantIds(1L));
        assertThrows(IllegalArgumentException.class, () -> controller.removeProjectParticipant(1L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> controller.removeProjectParticipant(404L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> controller.removeProjectParticipant(1L, " "));
    }

    private static ProjectController controller(
            AuthFixture auth,
            FakeProjectRepository projects,
            FakeUserRepository users) {
        return new ProjectController(
                auth.service(),
                new PermissionPolicy(),
                projects,
                users,
                new Clock());
    }

    private static AuthFixture authenticated(Role role) {
        String loginId = role.name().toLowerCase();
        User user = active(loginId, role);
        SessionStore sessionStore = new SessionStore();
        sessionStore.startSession(user);
        FakeUserRepository users = new FakeUserRepository(user);
        return new AuthFixture(
                new AuthenticationService(users, new PasswordHasher(), sessionStore),
                user);
    }

    private static AuthenticationService anonymousAuth() {
        return new AuthenticationService(new FakeUserRepository(), new PasswordHasher(), new SessionStore());
    }

    private static User active(String loginId, Role role) {
        return user(loginId, role, true);
    }

    private static User inactive(String loginId, Role role) {
        return user(loginId, role, false);
    }

    private static User user(String loginId, Role role, boolean active) {
        return new User(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private static Project project(long projectId, String name) {
        return new Project(projectId, name, "description", "admin", NOW, NOW);
    }

    private record AuthFixture(AuthenticationService service, User user) {
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projectsById = new LinkedHashMap<>();
        private final Map<Long, List<ProjectMember>> membersByProjectId = new LinkedHashMap<>();
        private long nextProjectId = 100L;

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
            Project persistedProject = project.id() == 0L
                    ? new Project(nextProjectId++, project.name(), project.description(), project.managedById(),
                            project.createdDate(), project.updatedAt())
                    : project;
            projectsById.put(persistedProject.id(), persistedProject);
            return persistedProject;
        }

        @Override
        public void deleteById(long projectId) {
            projectsById.remove(projectId);
            membersByProjectId.remove(projectId);
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
            membersByProjectId.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                    .add(new ProjectMember(projectId, userLoginId, NOW));
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
            membersByProjectId.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                    .removeIf(member -> member.userId().equals(userLoginId));
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return List.copyOf(membersByProjectId.getOrDefault(projectId, List.of()));
        }

        private List<String> participantIds(long projectId) {
            return findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .toList();
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
}
