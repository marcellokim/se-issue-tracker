package com.github.marcellokim.issuetracker.controller;

import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.PROJECT_ID;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.anonymousAuth;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.authenticated;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.project;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.projectController;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.projectService;
import static com.github.marcellokim.issuetracker.controller.ControllerTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.AuthFixture;
import com.github.marcellokim.issuetracker.controller.ControllerTestSupport.FakeUserRepository;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.ProjectAdminDetail;
import com.github.marcellokim.issuetracker.service.ProjectMemberResult;
import com.github.marcellokim.issuetracker.service.ProjectResult;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project controller")
class ProjectControllerTest {

    @Test
    @DisplayName("admin opens project management")
    void adminProjectPage() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        projects.withParticipant(PROJECT_ID, "dev1");
        var users = new FakeUserRepository(auth.user(), user("dev1", Role.DEV));
        ProjectController controller = projectController(auth, projects, users);

        List<ProjectMemberResult> participants = controller.viewProjectParticipants(PROJECT_ID);
        ProjectAdminDetail adminDetail = controller.viewProjectAdminDetail(PROJECT_ID);

        assertEquals(List.of("dev1"), participants.stream().map(ProjectMemberResult::userId).toList());
        assertEquals("project-one", adminDetail.project().name());
        assertThrows(NoSuchMethodException.class, () -> ProjectAdminDetail.class.getMethod("issues"));
    }

    @Test
    @DisplayName("admin project commands reach the service")
    void adminProjectCommands() {
        AuthFixture auth = authenticated(Role.ADMIN);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        ProjectController controller = projectController(
                auth,
                projects,
                new FakeUserRepository(auth.user()));

        ProjectResult created = controller.createProject(" project-two ", "second project");
        ProjectResult renamed = controller.renameProject(PROJECT_ID, " project-renamed ");
        ProjectResult changed = controller.changeProjectDescription(PROJECT_ID, " updated description ");
        controller.deleteProject(PROJECT_ID);

        assertEquals("project-two", created.name());
        assertEquals("project-renamed", renamed.name());
        assertEquals("updated description", changed.description());
        assertFalse(projects.findById(PROJECT_ID).isPresent());
    }

    @Test
    @DisplayName("non-admin project page uses the smaller detail")
    void nonAdminProjectPage() {
        AuthFixture admin = authenticated(Role.ADMIN);
        User dev = user("dev", Role.DEV);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        var users = new FakeUserRepository(admin.user(), dev);
        ProjectController adminController = projectController(admin, projects, users);
        adminController.addProjectParticipant(PROJECT_ID, dev.getLoginId());

        AuthFixture devAuth = authenticated(Role.DEV);
        ProjectController devController = projectController(
                devAuth,
                projects,
                new FakeUserRepository(devAuth.user()));

        ProjectResult nonAdminDetail = devController.viewProjectNonAdminDetail(PROJECT_ID);

        assertEquals("project-one", nonAdminDetail.name());
    }

    @Test
    @DisplayName("project management needs login")
    void needsLogin() {
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID, "project-one"));
        ProjectController controller = new ProjectController(
                anonymousAuth(),
                projectService(projects, new FakeUserRepository(user("dev1", Role.DEV))));

        assertThrows(SecurityException.class, () -> controller.createProject("blocked", "blocked"));
    }
}
