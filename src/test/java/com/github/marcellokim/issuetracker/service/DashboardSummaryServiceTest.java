package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dashboard summary service")
class DashboardSummaryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 20, 20);

    @Test
    @DisplayName("project card shows counts")
    void projectCardShowsCounts() {
        User pl = user("pl", Role.PL);
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 1);
        DashboardProjectSnapshot snapshot = snapshot(10L, "project1", statusCounts);

        DashboardSummaryService service = service(
                new FakeDashboardSummaryRepository(List.of(snapshot), List.of(snapshot)),
                new InMemoryUserRepository(pl));

        DashboardProjectSummary summary = service.projectSummariesFor(pl).getFirst();

        assertEquals("project1", summary.projectName());
        assertEquals(3, summary.memberCount());
        assertEquals(1, summary.projectLeaderCount());
        assertEquals(1, summary.developerCount());
        assertEquals(1, summary.testerCount());
        assertEquals(1, summary.visibleIssueCount());
        assertEquals(statusCounts, summary.statusCounts());
        assertEquals(0, summary.statusCounts().getOrDefault(IssueStatus.DELETED, 0));
    }

    @Test
    @DisplayName("admin sees projects and users")
    void adminSeesDashboard() {
        User admin = user("admin", Role.ADMIN);
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        DashboardProjectSnapshot project1 = snapshot(1L, "project1", Map.of());
        DashboardProjectSnapshot project2 = snapshot(2L, "project2", Map.of());

        FakeDashboardSummaryRepository summaries =
                new FakeDashboardSummaryRepository(List.of(project1, project2), List.of(project1));
        DashboardSummaryService service = service(summaries, new InMemoryUserRepository(admin, dev, tester));

        List<DashboardProjectSummary> actualSummaries = service.projectSummariesFor(admin);

        assertEquals(List.of("project1", "project2"), actualSummaries.stream()
                .map(DashboardProjectSummary::projectName)
                .toList());
        assertEquals(1, summaries.allProjectCalls);
        assertEquals(0, summaries.participantProjectCalls);
        assertEquals(List.of("admin", "dev", "tester"), service.usersFor(admin).stream()
                .map(UserResult::loginId)
                .toList());
    }

    @Test
    @DisplayName("member sees joined projects only")
    void memberSeesJoinedProjects() {
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        DashboardProjectSnapshot project1 = snapshot(1L, "project1", Map.of());
        DashboardProjectSnapshot project2 = snapshot(2L, "project2", Map.of());

        FakeDashboardSummaryRepository summaries =
                new FakeDashboardSummaryRepository(List.of(project1, project2), List.of(project1));
        DashboardSummaryService service = service(summaries, new InMemoryUserRepository(dev, tester));

        List<DashboardProjectSummary> actualSummaries = service.projectSummariesFor(dev);

        assertEquals(List.of("project1"), actualSummaries.stream()
                .map(DashboardProjectSummary::projectName)
                .toList());
        assertEquals(0, summaries.allProjectCalls);
        assertEquals(1, summaries.participantProjectCalls);
        assertEquals(dev.getLoginId(), summaries.lastParticipantLoginId);
        assertEquals(List.of(), service.usersFor(dev));
    }

    @Test
    @DisplayName("inactive user is stopped")
    void inactiveUserIsStopped() {
        User inactiveDev = User.fromPersistence("dev", "dev", "hash", Role.DEV, false, NOW, NOW);
        FakeDashboardSummaryRepository summaries = new FakeDashboardSummaryRepository(List.of(), List.of());
        DashboardSummaryService service = service(summaries, new InMemoryUserRepository(inactiveDev));

        assertThrows(SecurityException.class, () -> service.projectSummariesFor(inactiveDev));
        assertEquals(0, summaries.allProjectCalls);
        assertEquals(0, summaries.participantProjectCalls);
    }

    private static DashboardSummaryService service(
            DashboardSummaryRepository dashboardSummaryRepository,
            InMemoryUserRepository userRepository) {
        return new DashboardSummaryService(dashboardSummaryRepository, userRepository, new PermissionPolicy());
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "hash", role, true, NOW, NOW);
    }

    private static DashboardProjectSnapshot snapshot(
            long projectId,
            String projectName,
            Map<IssueStatus, Integer> statusCounts) {
        return new DashboardProjectSnapshot(
                projectId,
                projectName,
                "Demo project",
                3,
                1,
                1,
                1,
                1,
                statusCounts);
    }

    private static final class FakeDashboardSummaryRepository implements DashboardSummaryRepository {

        private final List<DashboardProjectSnapshot> allProjectSummaries;
        private final List<DashboardProjectSnapshot> participantProjectSummaries;
        private int allProjectCalls;
        private int participantProjectCalls;
        private String lastParticipantLoginId;

        private FakeDashboardSummaryRepository(
                List<DashboardProjectSnapshot> allProjectSummaries,
                List<DashboardProjectSnapshot> participantProjectSummaries) {
            this.allProjectSummaries = List.copyOf(allProjectSummaries);
            this.participantProjectSummaries = List.copyOf(participantProjectSummaries);
        }

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            allProjectCalls++;
            return allProjectSummaries;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            participantProjectCalls++;
            lastParticipantLoginId = loginId;
            return participantProjectSummaries;
        }
    }
}
