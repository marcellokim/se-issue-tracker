package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.time.LocalDateTime;
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

        User created = service.createAccount("dev11", "Dev 11", "TempPassword1!", Role.DEV, "admin");

        assertEquals("dev11", created.getLoginId());
        assertEquals("Dev 11", created.getName());
        assertEquals(Role.DEV, created.getRole());
        assertTrue(created.isActive());
        assertTrue(PASSWORD_HASHER.isHashed(created.getPasswordHash()));
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
                () -> service.createAccount("dev11", "Dev 11", "TempPassword1!", Role.DEV, "pl1"));
    }

    @Test
    @DisplayName("admin updates account name and role")
    void adminUpdatesAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        User updated = service.updateAccount("dev1", "Tester 1", Role.TESTER, "admin");

        assertEquals("Tester 1", updated.getName());
        assertEquals(Role.TESTER, updated.getRole());
        assertTrue(updated.isActive());
    }

    @Test
    @DisplayName("admin activates and deactivates accounts")
    void adminActivatesAndDeactivatesAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        User inactive = service.deactivateAccount("dev1", "admin");
        assertFalse(inactive.isActive());

        User active = service.activateAccount("dev1", "admin");
        assertTrue(active.isActive());
    }

    @Test
    @DisplayName("admin cannot create duplicate login id")
    void adminCannotCreateDuplicateAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount("dev1", "Other Dev", "TempPassword1!", Role.DEV, "admin"));
    }

    @Test
    @DisplayName("admin cannot deactivate own account")
    void adminCannotDeactivateOwnAccount() {
        InMemoryUserRepository users = new InMemoryUserRepository(admin());
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.deactivateAccount("admin", "admin"));
    }

    @Test
    @DisplayName("admin role cannot be created or assigned")
    void adminRoleCannotBeCreatedOrAssigned() {
        InMemoryUserRepository users = new InMemoryUserRepository(
                admin(),
                user("dev1", Role.DEV, true));
        AccountService service = service(users);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount("second-admin", "Second Admin", "TempPassword1!", Role.ADMIN, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(" admin ", "Admin Clone", "TempPassword1!", Role.DEV, "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateAccount("dev1", "Promoted Admin", Role.ADMIN, "admin"));
    }

    private static AccountService service(InMemoryUserRepository users) {
        return new AccountService(new PermissionPolicy(), users, PASSWORD_HASHER);
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
}
