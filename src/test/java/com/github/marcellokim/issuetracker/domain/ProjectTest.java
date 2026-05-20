package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project domain model")
class ProjectTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("project is created with name and manager id")
    void createProjectWithNameAndManager() {
        var project = Project.create(1L, "ITS", "Issue Tracking System", "admin", now, now);

        assertEquals(1L, project.getId());
        assertEquals("ITS", project.getName());
        assertEquals("Issue Tracking System", project.getDescription());
        assertEquals("admin", project.getManagedById());
        assertEquals(now, project.getCreatedDate());
        assertEquals(now, project.getUpdatedAt());
    }

    @Test
    @DisplayName("project name is required")
    void rejectNullName() {
        assertThrows(NullPointerException.class,
                () -> Project.create(1L, null, "desc", "admin", now, now));
    }

    @Test
    @DisplayName("project manager id is required")
    void rejectNullManagedById() {
        assertThrows(NullPointerException.class,
                () -> Project.create(1L, "ITS", "desc", null, now, now));
    }
}
