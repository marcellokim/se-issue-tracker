package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Login check service")
class LoginCheckServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 15, 30);
    private static final PasswordHasher PASSWORD_HASHER = new PasswordHasher();
    private static final String PASSWORD = "DemoLocalAdmin!";

    @Test
    @DisplayName("builds login-check output data without exposing repository calls to Main")
    void checksLoginAndAccountSummary() {
        User admin = user("admin", PASSWORD, Role.ADMIN, true);
        var service = new LoginCheckService(new InMemoryUserRepository(admin));

        LoginCheckResult result = service.checkLogin(" admin ", PASSWORD);

        assertEquals("admin", result.loginId());
        assertTrue(result.account().isPresent());
        assertEquals(Role.ADMIN, result.account().orElseThrow().role());
        assertTrue(result.account().orElseThrow().active());
        assertTrue(result.success());
        assertEquals("Login succeeded.", result.message());
    }

    @Test
    @DisplayName("keeps blank login id on the authentication failure path")
    void checksBlankLoginIdWithoutThrowing() {
        var service = new LoginCheckService(new InMemoryUserRepository());

        LoginCheckResult result = service.checkLogin(" ", PASSWORD);

        assertEquals("", result.loginId());
        assertTrue(result.account().isEmpty());
        assertFalse(result.success());
        assertEquals("ID and password are required.", result.message());
    }

    private static User user(String loginId, String password, Role role, boolean active) {
        return User.create(loginId, loginId, PASSWORD_HASHER.hash(password), role, active, NOW, NOW);
    }
}
