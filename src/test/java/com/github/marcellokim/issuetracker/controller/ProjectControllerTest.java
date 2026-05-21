package com.github.marcellokim.issuetracker.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectDetail;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        assertThrows(IllegalArgumentException.class, () -> controller.viewProject(0L));
        assertThrows(IllegalArgumentException.class, () -> controller.viewProjectParticipants(0L));
        assertThrows(IllegalArgumentException.class, () -> controller.viewProjectDetail(0L));
        assertThrows(IllegalArgumentException.class, () -> controller.deleteProject(0L));
        assertThrows(IllegalArgumentException.class, () -> controller.addProjectParticipant(0L, "dev1"));
        assertThrows(IllegalArgumentException.class, () -> controller.removeProjectParticipant(0L, "dev1"));
    }

    @Test
    @DisplayName("ADMIN views projects, participants, and project detail")
    void adminViewsProjectsAndProjectParticipants() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository(
                project(1L, "project-one"),
                project(2L, "project-two"));
        projects.addParticipant(1L, "pl1");
        projects.addParticipant(1L, "dev1");
        FakeIssueRepository issues = new FakeIssueRepository(
                issue(101L, 1L, IssueStatus.NEW),
                issue(102L, 2L, IssueStatus.ASSIGNED));
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()), issues);

        List<Project> viewedProjects = controller.viewProjects();
        Project viewedProject = controller.viewProject(1L);
        List<ProjectMember> viewedParticipants = controller.viewProjectParticipants(1L);
        ProjectDetail viewedDetail = controller.viewProjectDetail(1L);

        assertEquals(2, viewedProjects.size());
        assertEquals("project-one", viewedProject.getName());
        assertEquals(List.of("pl1", "dev1"), viewedParticipants.stream()
                .map(ProjectMember::userId)
                .toList());
        assertEquals("project-one", viewedDetail.project().getName());
        assertEquals(List.of("pl1", "dev1"), viewedDetail.participants().stream()
                .map(ProjectMember::userId)
                .toList());
        assertEquals(List.of(101L), viewedDetail.issues().stream()
                .map(Issue::id)
                .toList());
        assertThrows(IllegalArgumentException.class, () -> controller.viewProject(404L));
        assertThrows(IllegalArgumentException.class, () -> controller.viewProjectParticipants(404L));
        assertThrows(IllegalArgumentException.class, () -> controller.viewProjectDetail(404L));
    }

    @Test
    @DisplayName("ADMIN creates projects")
    void adminCreatesProjects() {
        AuthFixture auth = authenticated(Role.ADMIN);
        FakeProjectRepository projects = new FakeProjectRepository();
        ProjectController controller = controller(auth, projects, new FakeUserRepository(auth.user()));

        Project created = controller.createProject(" project-alpha ", "first project");

        assertTrue(created.getId() > 0L);
        assertEquals("project-alpha", created.getName());
        assertEquals("first project", created.getDescription());
        assertEquals("admin", created.getManagedByLoginId());
        assertNotNull(created.getCreatedDate());
        assertNotNull(created.getUpdatedAt());
        assertTrue(projects.findById(created.getId()).isPresent());
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

            assertThrows(SecurityException.class, controller::viewProjects);
            assertThrows(SecurityException.class, () -> controller.viewProject(1L));
            assertThrows(SecurityException.class, () -> controller.viewProjectParticipants(1L));
            assertThrows(SecurityException.class, () -> controller.viewProjectDetail(1L));
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
        ProjectController controller = ProjectController.create(
                anonymousAuth(),
                service(projects, users));

        assertThrows(SecurityException.class, controller::viewProjects);
        assertThrows(SecurityException.class, () -> controller.viewProject(1L));
        assertThrows(SecurityException.class, () -> controller.viewProjectParticipants(1L));
        assertThrows(SecurityException.class, () -> controller.viewProjectDetail(1L));
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
    @DisplayName("participant add allows first PL per project")
    void participantAddAllowsFirstProjectLead() {
        AuthFixture auth = authenticated(Role.ADMIN);
        User pl1 = active("pl1", Role.PL);
        FakeProjectRepository projects = new FakeProjectRepository(project(1L, "project-one"));
        FakeUserRepository users = new FakeUserRepository(auth.user(), pl1);
        ProjectController controller = controller(auth, projects, users);

        controller.addProjectParticipant(1L, "pl1");

        assertEquals(List.of("pl1"), projects.participantIds(1L));
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
        return ProjectController.create(
                auth.service(),
                service(projects, users));
    }

    private static ProjectController controller(
            AuthFixture auth,
            FakeProjectRepository projects,
            FakeUserRepository users,
            FakeIssueRepository issues) {
        return ProjectController.create(
                auth.service(),
                service(projects, users, issues));
    }

    private static ProjectService service(
            FakeProjectRepository projects,
            FakeUserRepository users) {
        return service(projects, users, new FakeIssueRepository());
    }

    private static ProjectService service(
            FakeProjectRepository projects,
            FakeUserRepository users,
            FakeIssueRepository issues) {
        users.attachProjects(projects);
        return ProjectService.create(
                projects,
                issues,
                users,
                new PermissionPolicy(),
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
        return User.create(loginId, loginId, "stored-password", role, active, NOW, NOW);
    }

    private static Project project(long projectId, String name) {
        return Project.create(projectId, name, "description", "admin", NOW, NOW);
    }

    private static Issue issue(long id, long projectId, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                projectId,
                "Issue " + id,
                "Project controller test issue",
                user("reporter", Role.DEV, true))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .updatedAt(NOW)
                .priority(Priority.MAJOR)
                .status(status));
    }

    private record AuthFixture(AuthenticationService service, User user) {
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projectsById = new LinkedHashMap<>();
        private final Map<Long, List<ProjectMember>> membersByProjectId = new LinkedHashMap<>();
        private long nextProjectId = 100L;

        private FakeProjectRepository(Project... projects) {
            for (Project project : projects) {
                projectsById.put(project.getId(), project);
            }
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return Optional.ofNullable(projectsById.get(projectId));
        }

        @Override
        public Optional<Project> findByName(String name) {
            return projectsById.values().stream()
                    .filter(project -> project.getName().equals(name))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projectsById.values());
        }

        @Override
        public Project save(Project project) {
            Project persistedProject = project.getId() == 0L
                    ? Project.create(nextProjectId++, project.getName(), project.getDescription(),
                            project.getManagedByLoginId(),
                            project.getCreatedDate(), project.getUpdatedAt())
                    : project;
            projectsById.put(persistedProject.getId(), persistedProject);
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
                    .add(ProjectMember.create(projectId, userLoginId, NOW));
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
        private FakeProjectRepository projects;

        private FakeUserRepository(User... users) {
            for (User user : users) {
                usersByLoginId.put(user.getLoginId(), user);
            }
        }

        private void attachProjects(FakeProjectRepository projects) {
            this.projects = Objects.requireNonNull(projects, "projects");
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
        public User save(User user) {
            usersByLoginId.put(user.getLoginId(), user);
            return user;
        }

        @Override
        public void deactivate(String loginId) {
            LocalDateTime now = LocalDateTime.now();
            findByLoginId(loginId).ifPresent(user -> user.deactivate(now));
        }
    }

    private static final class FakeIssueRepository implements IssueRepository {

        private final Map<Long, Issue> issuesById = new LinkedHashMap<>();

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
            return List.of();
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
            throw new UnsupportedOperationException("softDelete is not needed by ProjectControllerTest.");
        }

        @Override
        public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("restore is not needed by ProjectControllerTest.");
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            return 0;
        }

        @Override
        public void purge(long issueId) {
            issuesById.remove(issueId);
        }
    }
}
