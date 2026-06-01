package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project")
class ProjectTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("new projects start unsaved")
    void createsUnsavedProject() {
        Project project = Project.create("ITS", "Issue Tracking System", "admin", now);

        assertEquals(0L, project.getId());
        assertEquals("ITS", project.getName());
        assertEquals("Issue Tracking System", project.getDescription());
        assertEquals("admin", project.getManagedByLoginId());
        assertEquals(now, project.getCreatedDate());
        assertEquals(now, project.getUpdatedAt());
    }

    @Test
    @DisplayName("saved projects keep their stored state")
    void restoresSavedProject() {
        Project project = Project.fromPersistence(1L, "ITS", "Issue Tracking System", "admin", now, now);

        assertEquals(1L, project.getId());
        assertEquals("ITS", project.getName());
        assertEquals("Issue Tracking System", project.getDescription());
        assertEquals("admin", project.getManagedByLoginId());
        assertEquals(now, project.getCreatedDate());
        assertEquals(now, project.getUpdatedAt());
    }

    @Test
    @DisplayName("project name and manager are required")
    void rejectsMissingProjectIdentity() {
        assertThrows(NullPointerException.class, () -> Project.create(null, "desc", "admin", now));
        assertThrows(NullPointerException.class, () -> Project.create("ITS", "desc", null, now));
        assertThrows(IllegalArgumentException.class, () -> Project.create(" ", "desc", "admin", now));
        assertThrows(IllegalArgumentException.class, () -> Project.create("ITS", "desc", " ", now));
    }

    @Test
    @DisplayName("saved project ids must be positive")
    void rejectsInvalidSavedId() {
        assertThrows(IllegalArgumentException.class,
                () -> Project.fromPersistence(0L, "ITS", "desc", "admin", now, now));
        assertThrows(IllegalArgumentException.class,
                () -> Project.fromPersistence(-1L, "ITS", "desc", "admin", now, now));
    }

    @Test
    @DisplayName("project changes require a timestamp")
    void rejectsMissingChangeTime() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);

        assertThrows(NullPointerException.class, () -> Project.create("ITS", "desc", "admin", null));
        assertThrows(NullPointerException.class, () -> project.rename("ITS v2", null));
        assertThrows(NullPointerException.class, () -> project.changeDescription("New description", null));
    }

    @Test
    @DisplayName("rename changes name and timestamp")
    void renamesProject() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);
        LocalDateTime later = now.plusHours(1);

        project.rename("ITS v2", later);

        assertEquals("ITS v2", project.getName());
        assertEquals(later, project.getUpdatedAt());
    }

    @Test
    @DisplayName("blank project names are rejected")
    void rejectsBlankRename() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);

        assertThrows(IllegalArgumentException.class, () -> project.rename("", now.plusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> project.rename(" ", now.plusHours(1)));
    }

    @Test
    @DisplayName("description edit is saved")
    void changesDescription() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);
        LocalDateTime later = now.plusHours(1);

        project.changeDescription("New description", later);

        assertEquals("New description", project.getDescription());
        assertEquals(later, project.getUpdatedAt());
    }
}
