package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.dashboardProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dashboard controller")
class DashboardControllerTest {

    @Test
    @DisplayName("logged in admin can read dashboard data")
    void adminReadsDashboard() {
        ControllerTestSupport.AuthFixture auth = authenticated(Role.ADMIN);
        var dashboardSummaries = new ControllerTestSupport.FakeDashboardSummaryRepository(
                List.of(dashboardProject(PROJECT_ID, "project", 1)),
                List.of());
        DashboardController controller = new DashboardController(
                auth.service(),
                new DashboardSummaryService(dashboardSummaries, auth.users(), new PermissionPolicy()));

        assertEquals(1, controller.viewProjects().size());
        assertEquals(List.of(auth.user().getLoginId()), controller.viewUsers().stream()
                .map(UserResult::loginId)
                .toList());
    }

    @Test
    @DisplayName("dashboard needs a login")
    void needsLogin() {
        DashboardController controller = new DashboardController(
                anonymousAuth(),
                new DashboardSummaryService(
                        new ControllerTestSupport.FakeDashboardSummaryRepository(List.of(), List.of()),
                        new ControllerTestSupport.FakeUserRepository(),
                        new PermissionPolicy()));

        assertThrows(SecurityException.class, controller::viewProjects);
    }
}
