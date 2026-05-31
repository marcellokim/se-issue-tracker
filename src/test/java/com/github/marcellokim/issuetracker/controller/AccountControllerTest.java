package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.UserResult;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Account controller")
class AccountControllerTest {

    @Test
    @DisplayName("admin can create an account")
    void adminCreatesAccount() {
        ControllerTestSupport.AuthFixture auth = authenticated(Role.ADMIN);
        var projects = new InMemoryProjectRepository();
        var issues = new ControllerTestSupport.FakeIssueRepository();
        PasswordHasher hasher = new PasswordHasher();
        AccountController controller = new AccountController(
                auth.service(),
                new AccountService(new PermissionPolicy(), auth.users(), projects, issues, hasher,
                        java.time.LocalDateTime::now));

        UserResult result = controller.createAccount("newdev", "New Dev", "pass123", Role.DEV);

        assertEquals("newdev", result.loginId());
        assertEquals(Role.DEV, result.role());
        assertTrue(result.active());
    }

    @Test
    @DisplayName("admin can update another account")
    void adminUpdatesAccount() {
        ControllerTestSupport.AuthFixture auth = authenticated(Role.ADMIN);
        User target = user("target1", Role.DEV);
        auth.users().save(target);
        var projects = new InMemoryProjectRepository();
        var issues = new ControllerTestSupport.FakeIssueRepository();
        PasswordHasher hasher = new PasswordHasher();
        AccountController controller = new AccountController(
                auth.service(),
                new AccountService(new PermissionPolicy(), auth.users(), projects, issues, hasher,
                        java.time.LocalDateTime::now));

        UserResult renamed = controller.renameAccount("target1", "Renamed");
        assertEquals("Renamed", renamed.name());

        UserResult roleChanged = controller.changeAccountRole("target1", Role.TESTER);
        assertEquals(Role.TESTER, roleChanged.role());

        UserResult deactivated = controller.deactivateAccount("target1");
        assertFalse(deactivated.active());

        UserResult activated = controller.activateAccount("target1");
        assertTrue(activated.active());
    }

    @Test
    @DisplayName("account management needs a login")
    void needsLogin() {
        AccountController controller = new AccountController(
                anonymousAuth(),
                new AccountService(new PermissionPolicy(), new ControllerTestSupport.FakeUserRepository(),
                        new InMemoryProjectRepository(), new ControllerTestSupport.FakeIssueRepository(),
                        new PasswordHasher(), java.time.LocalDateTime::now));

        assertThrows(SecurityException.class,
                () -> controller.createAccount("x", "X", "pass", Role.DEV));
    }
}
