package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeUserRepository;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Authentication controller")
class AuthenticationControllerTest {

    @Test
    @DisplayName("login starts a session and logout clears it")
    void loginAndLogout() {
        PasswordHasher hasher = new PasswordHasher();
        User user = User.fromPersistence("dev", "dev", hasher.hash("secret"), Role.DEV, true, NOW, NOW);
        var users = new FakeUserRepository(user);
        AuthenticationService authService = new AuthenticationService(users, hasher, new SessionStore());
        AuthenticationController controller = new AuthenticationController(authService);

        var result = controller.login(user.getLoginId(), "secret");
        controller.logout();

        assertTrue(result.success());
        assertEquals(Optional.empty(), authService.currentUser());
    }
}
