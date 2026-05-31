package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project service")
class ProjectServiceTest {

    private static final long PROJECT_ID = 10L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 1, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

    @Test
    @DisplayName("admin creates project")
    void adminCreatesProject() {
        var users = users(admin());
        var projects = new InMemoryProjectRepository();
        var service = service(projects, users);

        ProjectResult result = service.createProject(" Project C ", " Demo project ", "admin");

        assertEquals("Project C", result.name());
        assertEquals("Demo project", result.description());
        assertEquals("admin", result.managedByLoginId());
        assertTrue(projects.findByName("Project C").isPresent());
    }

    @Test
    @DisplayName("only admin manages projects")
    void onlyAdminManagesProjects() {
        var users = users(admin(), user("pl1", Role.PL, true));
        var service = service(new InMemoryProjectRepository(), users);

        assertThrows(SecurityException.class,
                () -> service.createProject("Project C", "Demo project", "pl1"));
    }

    @Test
    @DisplayName("project needs name and description")
    void projectNeedsNameAndDescription() {
        var service = service(new InMemoryProjectRepository(), users(admin()));

        assertThrows(IllegalArgumentException.class,
                () -> service.createProject(" ", "Demo project", "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createProject("Project C", "", "admin"));
    }

    @Test
    @DisplayName("project name stays unique")
    void projectNameStaysUnique() {
        var projects = new InMemoryProjectRepository(
                project(PROJECT_ID, "Project A"),
                project(20L, "Project B"));
        var service = service(projects, users(admin()));

        assertThrows(IllegalArgumentException.class,
                () -> service.createProject("Project A", "Another project", "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.renameProject(PROJECT_ID, "Project B", "admin"));
    }

    @Test
    @DisplayName("admin updates project info")
    void adminUpdatesProjectInfo() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"));
        var service = service(projects, users(admin()));

        ProjectResult renamed = service.renameProject(PROJECT_ID, "Project Alpha", "admin");
        ProjectResult described = service.changeProjectDescription(PROJECT_ID, "Updated description", "admin");

        assertEquals("Project Alpha", renamed.name());
        assertEquals("Updated description", described.description());
        assertEquals(NOW, projects.findById(PROJECT_ID).orElseThrow().getUpdatedAt());
    }

    @Test
    @DisplayName("participant opens project detail")
    void participantOpensProjectDetail() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "dev1");
        var service = service(projects, users(admin(), user("dev1", Role.DEV, true)));

        ProjectResult result = service.viewProjectNonAdminDetail(PROJECT_ID, "dev1");

        assertEquals("Project A", result.name());
        assertThrows(SecurityException.class,
                () -> service.viewProjectNonAdminDetail(PROJECT_ID, "admin"));
    }

    @Test
    @DisplayName("admin detail includes participants")
    void adminDetailIncludesParticipants() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "pl1")
                .withParticipant(PROJECT_ID, "dev1");
        var service = service(projects, users(admin(), user("pl1", Role.PL, true), user("dev1", Role.DEV, true)));

        ProjectAdminDetail detail = service.viewProjectAdminDetail(PROJECT_ID, "admin");

        assertEquals("Project A", detail.project().name());
        assertEquals(List.of("dev1", "pl1"), detail.participants().stream()
                .map(ProjectMemberResult::userId)
                .toList());
    }

    @Test
    @DisplayName("admin adds participant")
    void adminAddsParticipant() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"));
        var service = service(projects, users(admin(), user("dev1", Role.DEV, true)));

        service.addProjectParticipant(PROJECT_ID, "dev1", "admin");

        assertTrue(projects.participantIds(PROJECT_ID).contains("dev1"));
    }

    @Test
    @DisplayName("bad participants are not added")
    void badParticipantsAreNotAdded() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "dev1");
        var users = users(
                admin(),
                user("dev1", Role.DEV, true),
                user("dev2", Role.DEV, false),
                user("admin2", Role.ADMIN, true));
        var service = service(projects, users);

        assertThrows(IllegalArgumentException.class,
                () -> service.addProjectParticipant(PROJECT_ID, "dev1", "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.addProjectParticipant(PROJECT_ID, "dev2", "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.addProjectParticipant(PROJECT_ID, "admin2", "admin"));
    }

    @Test
    @DisplayName("project keeps one PL")
    void projectKeepsOnePl() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "pl1");
        var users = users(admin(), user("pl1", Role.PL, true), user("pl2", Role.PL, true))
                .withProjectMembers(PROJECT_ID, "pl1");
        var service = service(projects, users);

        assertThrows(IllegalArgumentException.class,
                () -> service.addProjectParticipant(PROJECT_ID, "pl2", "admin"));
    }

    @Test
    @DisplayName("admin removes participant")
    void adminRemovesParticipant() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "dev1");
        var service = service(projects, users(admin(), user("dev1", Role.DEV, true)));

        service.removeProjectParticipant(PROJECT_ID, "dev1", "admin");

        assertFalse(projects.participantIds(PROJECT_ID).contains("dev1"));
    }

    @Test
    @DisplayName("assigned user stays in project")
    void assignedUserStaysInProject() {
        User dev = user("dev1", Role.DEV, true);
        User tester = user("tester1", Role.TESTER, true);
        User pl = user("pl1", Role.PL, true);
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "dev1");
        var service = service(projects, users(admin(), dev, tester, pl), new InMemoryIssueRepository(
                assignedIssue(dev, tester, pl)));

        assertThrows(IllegalArgumentException.class,
                () -> service.removeProjectParticipant(PROJECT_ID, "dev1", "admin"));
    }

    @Test
    @DisplayName("admin deletes project")
    void adminDeletesProject() {
        var projects = new InMemoryProjectRepository(project(PROJECT_ID, "Project A"))
                .withParticipant(PROJECT_ID, "dev1");
        var service = service(projects, users(admin(), user("dev1", Role.DEV, true)));

        service.deleteProject(PROJECT_ID, "admin");

        assertEquals(PROJECT_ID, projects.lastDeletedProjectId());
        assertTrue(projects.findById(PROJECT_ID).isEmpty());
    }

    private static ProjectService service(InMemoryProjectRepository projects, InMemoryUserRepository users) {
        return service(projects, users, new InMemoryIssueRepository());
    }

    private static ProjectService service(
            InMemoryProjectRepository projects,
            InMemoryUserRepository users,
            InMemoryIssueRepository issues) {
        return new ProjectService(projects, issues, users, new PermissionPolicy(), () -> NOW);
    }

    private static InMemoryUserRepository users(User... users) {
        return new InMemoryUserRepository(users);
    }

    private static User admin() {
        return user("admin", Role.ADMIN, true);
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId.toUpperCase(), "hash", role, active, CREATED_AT, CREATED_AT);
    }

    private static Project project(long id, String name) {
        return Project.fromPersistence(id, name, "Old description", "admin", CREATED_AT, CREATED_AT);
    }

    private static Issue assignedIssue(User assignee, User verifier, User pl) {
        Issue issue = Issue.fromPersistence(Issue.persistedState(PROJECT_ID, "Login fails", "Cannot log in", verifier)
                .id(1L)
                .issueId("ISSUE-1")
                .reportedDate(CREATED_AT)
                .updatedAt(CREATED_AT));
        issue.assignFromNew(assignee, verifier, pl, CREATED_AT.plusMinutes(10));
        return issue;
    }
}
