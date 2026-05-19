package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(project.getIssues().isEmpty());
        assertTrue(project.getParticipants().isEmpty());
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
    @DisplayName("프로젝트에 이슈를 등록하면 이슈 목록에 추가된다")
    void registerIssueAddsToList() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var reporter = User.create("tester1", "Tester One", "hash", Role.TESTER, true, null, null);

        var issue = project.registerIssue("ISSUE-1", "Login fails", "Cannot log in", null, reporter, now);

        assertNotNull(issue);
        assertEquals(1, project.getIssues().size());
    }

    @Test
    @DisplayName("참여자를 추가하면 참여자 목록에 포함된다")
    void addParticipant() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var dev = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

        project.addParticipant(dev);

        assertEquals(1, project.getParticipants().size());
        assertEquals("dev1", project.getParticipants().get(0).getLoginId());
    }

    @Test
    @DisplayName("이미 참여 중인 사용자를 추가하면 예외가 발생한다")
    void rejectDuplicateParticipant() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var dev = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

        project.addParticipant(dev);

        assertThrows(IllegalArgumentException.class, () -> project.addParticipant(dev));
    }

    @Test
    @DisplayName("참여자를 제거하면 참여자 목록에서 삭제된다")
    void removeParticipant() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var dev = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

        project.addParticipant(dev);
        project.removeParticipant(dev);

        assertTrue(project.getParticipants().isEmpty());
    }

    @Test
    @DisplayName("참여하지 않은 사용자를 제거하면 예외가 발생한다")
    void rejectRemoveNonParticipant() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);
        var dev = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

        assertThrows(IllegalArgumentException.class, () -> project.removeParticipant(dev));
    }

    @Test
    @DisplayName("null 참여자는 거부된다")
    void rejectNullParticipant() {
        var project = Project.create(1L, "ITS", "desc", "admin", now, now);

        assertThrows(NullPointerException.class, () -> project.addParticipant(null));
        assertThrows(NullPointerException.class, () -> project.removeParticipant(null));
    }
}
