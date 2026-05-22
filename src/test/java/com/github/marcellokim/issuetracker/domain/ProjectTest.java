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
    @DisplayName("create builds a transient project")
    void createProjectWithNameAndManager() {
        Project project = Project.create("ITS", "Issue Tracking System", "admin", now);

        assertEquals(0L, project.getId());
        assertEquals("ITS", project.getName());
        assertEquals("Issue Tracking System", project.getDescription());
        assertEquals("admin", project.getManagedByLoginId());
        assertEquals(now, project.getCreatedDate());
        assertEquals(now, project.getUpdatedAt());
    }

    @Test
    @DisplayName("fromPersistence restores stored id and timestamps")
    void restoreProjectFromPersistence() {
        Project project = Project.fromPersistence(1L, "ITS", "Issue Tracking System", "admin", now, now);

        assertEquals(1L, project.getId());
        assertEquals("ITS", project.getName());
        assertEquals(now, project.getCreatedDate());
        assertEquals(now, project.getUpdatedAt());
    }

    @Test
    @DisplayName("project name is required")
    void rejectNullName() {
        assertThrows(NullPointerException.class, () -> Project.create(null, "desc", "admin", now));
    }

    @Test
    @DisplayName("project manager id is required")
    void rejectNullManagedById() {
        assertThrows(NullPointerException.class, () -> Project.create("ITS", "desc", null, now));
    }

    @Test
    @DisplayName("blank name is rejected by requireText")
    void rejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> Project.create("", "desc", "admin", now));
        assertThrows(IllegalArgumentException.class, () -> Project.create(" ", "desc", "admin", now));
    }

    @Test
    @DisplayName("registerIssue creates issue via Creator pattern")
    void registerIssueCreatesIssue() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);
        User reporter = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, true, now, now);

        Issue issue = project.registerIssue("ISSUE-1", "Bug", "Login fails", Priority.MAJOR, reporter, now);

        assertEquals("ISSUE-1", issue.getIssueId());
        assertEquals("Bug", issue.getTitle());
        assertEquals(IssueStatus.NEW, issue.getStatus());
        assertEquals(Priority.MAJOR, issue.getPriority());
    }

    @Test
    @DisplayName("rename updates name and updatedAt")
    void renameUpdatesNameAndTimestamp() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);
        LocalDateTime later = now.plusHours(1);

        project.rename("ITS v2", later);

        assertEquals("ITS v2", project.getName());
        assertEquals(later, project.getUpdatedAt());
    }

    @Test
    @DisplayName("rename rejects blank name")
    void renameRejectsBlankName() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);

        assertThrows(IllegalArgumentException.class, () -> project.rename("", now.plusHours(1)));
    }

    @Test
    @DisplayName("changeDescription updates description and updatedAt")
    void changeDescriptionUpdatesDescriptionAndTimestamp() {
        Project project = Project.fromPersistence(1L, "ITS", "desc", "admin", now, now);
        LocalDateTime later = now.plusHours(1);

        project.changeDescription("New description", later);

        assertEquals("New description", project.getDescription());
        assertEquals(later, project.getUpdatedAt());
    }
}
