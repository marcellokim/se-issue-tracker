package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("?꾨줈?앺듃 ?꾨찓??紐⑤뜽")
class ProjectTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("?꾨줈?앺듃???대쫫怨?愿由ъ옄 ID瑜?媛吏??곹깭濡??앹꽦?쒕떎")
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

    // @Test
    // @DisplayName("?꾨줈?앺듃?먯꽌 ?댁뒋瑜??앹꽦?????덈떎")
    // void registerIssueCreatesIssue() {
    // var project = Project.create(1L, "ITS", "desc", "admin", now, now);
    // var reporter = User.create("tester1", "Tester One", "hash", Role.TESTER,
    // true, null, null);

    // var issue = project.registerIssue("ISSUE-1", "Login fails", "Cannot log in",
    // null, reporter, now);

    // assertNotNull(issue);
    // }
}
