package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Account service")
class AccountServiceTest {

    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();

    @Test
    @DisplayName("admin creates an active account with hashed credentials")
    void adminCreatesAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(admin());
        AccountService service = service(users);

        UserResult created = service.createAccount("dev11", "Dev 11", "TempPassword1!", Role.DEV, actor(users, "admin"));

        assertEquals("dev11", created.loginId());
        assertEquals("Dev 11", created.name());
        assertEquals(Role.DEV, created.role());
        assertTrue(created.active());
        assertTrue(PASSWORD_HASHER.isHashed(users.findByLoginId("dev11").orElseThrow().getPasswordHash()));
        assertTrue(users.findByLoginId("dev11").isPresent());
    }

    @Test
    @DisplayName("non admin cannot manage accounts")
    void nonAdminCannotManageAccounts() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("pl1", Role.PL, true));
        AccountService service = service(users);

        assertThrows(SecurityException.class,
                () -> service.createAccount("dev11", "Dev 11", "TempPassword1!", Role.DEV, actor(users, "pl1")));
    }

    @Test
    @DisplayName("admin updates account name and role")
    void adminUpdatesAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        UserResult updated = service.updateAccount("dev1", "Tester 1", Role.TESTER, actor(users, "admin"));

        assertEquals("Tester 1", updated.name());
        assertEquals(Role.TESTER, updated.role());
        assertTrue(updated.active());
    }

    @Test
    @DisplayName("admin renames account without changing role")
    void adminRenamesAccountOnly() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        UserResult updated = service.renameAccount("dev1", "Dev One", actor(users, "admin"));

        assertEquals("Dev One", updated.name());
        assertEquals(Role.DEV, updated.role());
    }

    @Test
    @DisplayName("admin changes account role without changing name")
    void adminChangesAccountRoleOnly() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        UserResult updated = service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin"));

        assertEquals("DEV1", updated.name());
        assertEquals(Role.TESTER, updated.role());
    }

    @Test
    @DisplayName("account role change requires no project membership")
    void accountRoleChangeRequiresNoProjectMembership() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        FakeProjectRepository projects = new FakeProjectRepository()
                .withProject(project(1L, "project1"))
                .withParticipant(1L, "dev1");
        AccountService service = service(users, projects, new InMemoryIssueRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin")));
    }

    @Test
    @DisplayName("account role change requires no assigned issue responsibility")
    void accountRoleChangeRequiresNoAssignedIssueResponsibility() {
        User dev = user("dev1", Role.DEV, true);
        User tester = user("tester1", Role.TESTER, true);
        InMemoryUserRepository users = new InMemoryUserRepository(admin(), dev, tester);
        FakeProjectRepository projects = new FakeProjectRepository()
                .withProject(project(1L, "project1"));
        InMemoryIssueRepository issues = new InMemoryIssueRepository(assignedIssue(dev, tester));
        AccountService service = service(users, projects, issues);

        assertThrows(IllegalArgumentException.class,
                () -> service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin")));
    }

    @Test
    @DisplayName("admin activates and deactivates accounts")
    void adminActivatesAndDeactivatesAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        UserResult inactive = service.deactivateAccount("dev1", actor(users, "admin"));
        assertFalse(inactive.active());

        UserResult active = service.activateAccount("dev1", actor(users, "admin"));
        assertTrue(active.active());
    }

    @Test
    @DisplayName("admin cannot create duplicate login id")
    void adminCannotCreateDuplicateAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount("dev1", "Other Dev", "TempPassword1!", Role.DEV, actor(users, "admin")));
    }

    @Test
    @DisplayName("admin cannot deactivate own account")
    void adminCannotDeactivateOwnAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(admin());
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.deactivateAccount("admin", actor(users, "admin")));
    }

    @Test
    @DisplayName("admin role cannot be created or assigned")
    void adminRoleCannotBeCreatedOrAssigned() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount("second-admin", "Second Admin", "TempPassword1!", Role.ADMIN,
                        actor(users, "admin")));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(" admin ", "Admin Clone", "TempPassword1!", Role.DEV,
                        actor(users, "admin")));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateAccount("dev1", "Promoted Admin", Role.ADMIN, actor(users, "admin")));
    }

    @Test
    @DisplayName("account service requires project and issue repositories for policy checks")
    void accountServiceRequiresPolicyRepositories() {
        InMemoryUserRepository users = new InMemoryUserRepository(admin());
        PermissionPolicy policy = new PermissionPolicy();

        assertThrows(NullPointerException.class,
                () -> new AccountService(policy, users, null, new InMemoryIssueRepository(), PASSWORD_HASHER));
        assertThrows(NullPointerException.class,
                () -> new AccountService(policy, users, new FakeProjectRepository(), null, PASSWORD_HASHER));
        assertNotNull(new AccountService(
                policy,
                users,
                new FakeProjectRepository(),
                new InMemoryIssueRepository(),
                PASSWORD_HASHER));
    }

    private static AccountService service(InMemoryUserRepository users) {
        return service(users, new FakeProjectRepository(), new InMemoryIssueRepository());
    }

    private static AccountService service(
            InMemoryUserRepository users,
            ProjectRepository projects,
            InMemoryIssueRepository issues) {
        return new AccountService(new PermissionPolicy(), users, projects, issues, PASSWORD_HASHER);
    }

    private static User actor(InMemoryUserRepository users, String loginId) {
        return users.findByLoginId(loginId).orElseThrow();
    }

    private static User admin() {
        return user("admin", Role.ADMIN, true);
    }

    private static User user(String loginId, Role role, boolean active) {
        User user = User.create(
                loginId,
                loginId.toUpperCase(),
                PASSWORD_HASHER.hash("password"),
                role,
                LocalDateTime.of(2026, 5, 1, 0, 0));
        if (!active) {
            user.deactivate(LocalDateTime.of(2026, 5, 1, 0, 1));
        }
        return user;
    }

    private static Project project(long id, String name) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 0, 0);
        return Project.fromPersistence(id, name, "description", "admin", now, now);
    }

    private static Issue assignedIssue(User assignee, User verifier) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 2, 0, 0);
        return Issue.fromPersistence(Issue.persistedState(1L, "Assigned issue", "description", verifier)
                .id(1L)
                .issueId("ISSUE-1")
                .reportedDate(now)
                .priority(Priority.MAJOR)
                .status(IssueStatus.ASSIGNED)
                .assignee(assignee)
                .verifier(verifier)
                .updatedAt(now));
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Map<Long, Project> projects = new LinkedHashMap<>();
        private final Map<Long, List<ProjectMember>> participants = new LinkedHashMap<>();

        FakeProjectRepository withProject(Project project) {
            projects.put(project.getId(), project);
            return this;
        }

        FakeProjectRepository withParticipant(long projectId, String loginId) {
            participants.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                    .add(ProjectMember.create(projectId, loginId, LocalDateTime.of(2026, 5, 1, 0, 0)));
            return this;
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return Optional.ofNullable(projects.get(projectId));
        }

        @Override
        public Optional<Project> findByName(String name) {
            return projects.values().stream()
                    .filter(project -> project.getName().equals(name))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return List.copyOf(projects.values());
        }

        @Override
        public Project save(Project project) {
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public void deleteById(long projectId) {
            projects.remove(projectId);
            participants.remove(projectId);
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
            withParticipant(projectId, userLoginId);
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
            participants.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                    .removeIf(participant -> participant.userId().equals(userLoginId));
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return List.copyOf(participants.getOrDefault(projectId, List.of()));
        }

        @Override
        public boolean existsByParticipant(String userLoginId) {
            return participants.values().stream()
                    .flatMap(List::stream)
                    .map(ProjectMember::userId)
                    .anyMatch(userLoginId::equals);
        }
    }
}
