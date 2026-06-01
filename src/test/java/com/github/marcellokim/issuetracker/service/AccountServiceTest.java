package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Account service")
class AccountServiceTest {

        @Test
        @DisplayName("admin creates a dev account")
        void createsDevAccount() {
                InMemoryUserRepository users = new InMemoryUserRepository(admin());
                TestPasswordHashing passwordHashing = new TestPasswordHashing();
                AccountService service = service(users, passwordHashing);
                String rawPassword = " TempPassword1! ";

                UserResult created = service.createAccount("dev11", "Dev 11", rawPassword, Role.DEV,
                                actor(users, "admin"));

                assertEquals("dev11", created.loginId());
                assertEquals("Dev 11", created.name());
                assertEquals(Role.DEV, created.role());
                assertTrue(created.active());
                assertEquals(rawPassword, passwordHashing.lastPassword());
                assertEquals("hash:" + rawPassword, users.findByLoginId("dev11").orElseThrow().getPasswordHash());
                assertTrue(users.findByLoginId("dev11").isPresent());
        }

        @Test
        @DisplayName("PL cannot add accounts")
        void plCannotAddAccount() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("pl1", Role.PL, true));
                AccountService service = service(users);

                assertThrows(SecurityException.class,
                                () -> service.createAccount("dev11", "Dev 11", "TempPassword1!", Role.DEV,
                                                actor(users, "pl1")));
        }

        @Test
        @DisplayName("admin renames an account")
        void renamesAccount() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("dev1", Role.DEV, true));
                AccountService service = service(users);

                UserResult updated = service.renameAccount("dev1", "Dev One", actor(users, "admin"));

                assertEquals("Dev One", updated.name());
                assertEquals(Role.DEV, updated.role());
        }

        @Test
        @DisplayName("admin changes a role")
        void changesRole() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("dev1", Role.DEV, true));
                AccountService service = service(users);

                UserResult updated = service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin"));

                assertEquals("DEV1", updated.name());
                assertEquals(Role.TESTER, updated.role());
        }

        @Test
        @DisplayName("project member keeps the role")
        void projectMemberKeepsRole() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("dev1", Role.DEV, true));
                InMemoryProjectRepository projects = new InMemoryProjectRepository()
                                .withProject(project(1L, "project1"))
                                .withParticipant(1L, "dev1");
                AccountService service = service(users, projects, new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin")));
        }

        @Test
        @DisplayName("assigned dev keeps the role")
        void assignedDevKeepsRole() {
                User dev = user("dev1", Role.DEV, true);
                User tester = user("tester1", Role.TESTER, true);
                InMemoryUserRepository users = new InMemoryUserRepository(admin(), dev, tester);
                InMemoryProjectRepository projects = new InMemoryProjectRepository()
                                .withProject(project(1L, "project1"));
                InMemoryIssueRepository issues = new InMemoryIssueRepository(assignedIssue(dev, tester));
                AccountService service = service(users, projects, issues);

                assertThrows(IllegalArgumentException.class,
                                () -> service.changeAccountRole("dev1", Role.TESTER, actor(users, "admin")));
        }

        @Test
        @DisplayName("admin can deactivate and activate account")
        void togglesAccountActive() {
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
        @DisplayName("project member stays active")
        void projectMemberStaysActive() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("pl1", Role.PL, true));
                InMemoryProjectRepository projects = new InMemoryProjectRepository()
                                .withProject(project(1L, "project1"))
                                .withParticipant(1L, "pl1");
                AccountService service = service(users, projects, new InMemoryIssueRepository());

                assertThrows(IllegalArgumentException.class,
                                () -> service.deactivateAccount("pl1", actor(users, "admin")));
        }

        @Test
        @DisplayName("assigned user stays active")
        void assignedUserStaysActive() {
                User dev = user("dev1", Role.DEV, true);
                User tester = user("tester1", Role.TESTER, true);
                InMemoryUserRepository users = new InMemoryUserRepository(admin(), dev, tester);
                InMemoryIssueRepository issues = new InMemoryIssueRepository(assignedIssue(dev, tester));
                AccountService service = service(users, new InMemoryProjectRepository(), issues);

                assertThrows(IllegalArgumentException.class,
                                () -> service.deactivateAccount("dev1", actor(users, "admin")));
                assertThrows(IllegalArgumentException.class,
                                () -> service.deactivateAccount("tester1", actor(users, "admin")));
        }

        @Test
        @DisplayName("old fixer can be deactivated")
        void deactivatesOldFixer() {
                User dev = user("dev1", Role.DEV, true);
                User tester = user("tester1", Role.TESTER, true);
                InMemoryUserRepository users = new InMemoryUserRepository(admin(), dev, tester);
                InMemoryIssueRepository issues = new InMemoryIssueRepository(completedIssueWithAudit(dev, tester));
                AccountService service = service(users, new InMemoryProjectRepository(), issues);

                UserResult result = service.deactivateAccount("dev1", actor(users, "admin"));

                assertFalse(result.active());
        }

        @Test
        @DisplayName("duplicate login id is not accepted")
        void duplicateLoginIdIsNotAccepted() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("dev1", Role.DEV, true));
                AccountService service = service(users);

                assertThrows(IllegalArgumentException.class,
                                () -> service.createAccount("dev1", "Other Dev", "TempPassword1!", Role.DEV,
                                                actor(users, "admin")));
        }

        @Test
        @DisplayName("admin cannot deactivate self")
        void adminCannotDeactivateSelf() {
                InMemoryUserRepository users = new InMemoryUserRepository(admin());
                AccountService service = service(users);

                assertThrows(IllegalArgumentException.class,
                                () -> service.deactivateAccount("admin", actor(users, "admin")));
        }

        @Test
        @DisplayName("admin role stays reserved")
        void adminRoleStaysReserved() {
                InMemoryUserRepository users = new InMemoryUserRepository(
                                admin(),
                                user("dev1", Role.DEV, true));
                AccountService service = service(users);

                assertThrows(IllegalArgumentException.class,
                                () -> service.createAccount("second-admin", "Second Admin", "TempPassword1!",
                                                Role.ADMIN,
                                                actor(users, "admin")));
                assertThrows(IllegalArgumentException.class,
                                () -> service.createAccount(" admin ", "Admin Clone", "TempPassword1!", Role.DEV,
                                                actor(users, "admin")));
                assertThrows(IllegalArgumentException.class,
                                () -> service.changeAccountRole("dev1", Role.ADMIN, actor(users, "admin")));
        }

        private static AccountService service(InMemoryUserRepository users) {
                return service(users, new InMemoryProjectRepository(), new InMemoryIssueRepository());
        }

        private static AccountService service(InMemoryUserRepository users, TestPasswordHashing passwordHashing) {
                return service(users, new InMemoryProjectRepository(), new InMemoryIssueRepository(), passwordHashing);
        }

        private static AccountService service(
                        InMemoryUserRepository users,
                        ProjectRepository projects,
                        InMemoryIssueRepository issues) {
                return service(users, projects, issues, new TestPasswordHashing());
        }

        private static AccountService service(
                        InMemoryUserRepository users,
                        ProjectRepository projects,
                        InMemoryIssueRepository issues,
                        PasswordHashing passwordHashing) {
                return new AccountService(new PermissionPolicy(), users, projects, issues, passwordHashing,
                                LocalDateTime::now);
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
                                "hash:password",
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

        private static Issue completedIssueWithAudit(User fixer, User resolver) {
                LocalDateTime now = LocalDateTime.of(2026, 5, 2, 0, 0);
                return Issue.fromPersistence(Issue.persistedState(1L, "Closed issue", "description", resolver)
                                .id(2L)
                                .issueId("ISSUE-2")
                                .reportedDate(now)
                                .priority(Priority.MAJOR)
                                .status(IssueStatus.CLOSED)
                                .fixer(fixer)
                                .resolver(resolver)
                                .updatedAt(now));
        }

        private static final class TestPasswordHashing implements PasswordHashing {
                private String lastPassword;

                @Override
                public String hash(String password) {
                        lastPassword = password;
                        return "hash:" + password;
                }

                @Override
                public boolean matches(String password, String storedCredential) {
                        return storedCredential.equals("hash:" + password);
                }

                @Override
                public boolean isHashed(String storedCredential) {
                        return storedCredential != null && storedCredential.startsWith("hash:");
                }

                @Override
                public String saltOf(String storedCredential) {
                        return "";
                }

                @Override
                public String hashOf(String storedCredential) {
                        return storedCredential == null ? "" : storedCredential.replaceFirst("^hash:", "");
                }

                private String lastPassword() {
                        return lastPassword;
                }
        }

}
