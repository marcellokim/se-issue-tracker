package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("프로젝트 도메인 모델")
class ProjectTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Test
    @DisplayName("프로젝트는 이름과 관리자 ID를 가진 상태로 생성된다")
    void createProjectWithNameAndManager() {
        var project = Project.create(1L, "ITS", "Issue Tracking System", "admin", now, now);

        assertEquals(1L, project.id());
        assertEquals("ITS", project.name());
        assertEquals("Issue Tracking System", project.description());
        assertEquals("admin", project.managedById());
        assertEquals(now, project.createdDate());
        assertEquals(now, project.updatedAt());
    }

    @Test
    @DisplayName("프로젝트 이름은 필수 값이다")
    void rejectNullName() {
        assertThrows(NullPointerException.class,
                () -> Project.create(1L, null, "desc", "admin", now, now));
    }

    @Test
    @DisplayName("프로젝트 관리자 ID는 필수 값이다")
    void rejectNullManagedById() {
        assertThrows(NullPointerException.class,
                () -> Project.create(1L, "ITS", "desc", null, now, now));
    }

    @Test
    @DisplayName("프로젝트에서 이슈를 생성할 수 있다")
    void registerIssueCreatesIssue() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, null, null);

        var issue = project.registerIssue("ISSUE-1", "Login fails", "Cannot log in", null, reporter, now);

        assertNotNull(issue);
    }
}
