package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dashboard summary service")
class DashboardSummaryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 20, 20);
    private static final long PROJECT_ID = 10L;

    @Test
    @DisplayName("builds dashboard read model without exposing repositories to the presenter")
    void buildsDashboardProjectSummaries() {
        Project project = Project.fromPersistence(PROJECT_ID, "project1", "Demo project", "admin", NOW, NOW);
        User pl = user("pl", Role.PL);
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        Issue activeIssue = issue(101L, IssueStatus.NEW);
        Issue deletedIssue = issue(102L, IssueStatus.DELETED);
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 1);

        DashboardSummaryService service = new DashboardSummaryService(
                new InMemoryProjectRepository(project).withParticipants(PROJECT_ID, List.of(
                        ProjectMember.create(PROJECT_ID, pl.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))).rejectWrites(),
                new InMemoryIssueRepository(activeIssue, deletedIssue),
                new FakeStatisticsRepository(statusCounts),
                new InMemoryUserRepository(pl, dev, tester),
                new PermissionPolicy());

        DashboardProjectSummary summary = service.projectSummariesFor(pl).getFirst();

        assertEquals("project1", summary.projectName());
        assertEquals(3, summary.memberCount());
        assertEquals(1, summary.projectLeaderCount());
        assertEquals(1, summary.developerCount());
        assertEquals(1, summary.testerCount());
        assertEquals(1, summary.visibleIssueCount());
        assertEquals(1, summary.deletedIssueCount());
        assertEquals(statusCounts, summary.statusCounts());
        assertEquals(0, summary.statusCounts().getOrDefault(IssueStatus.DELETED, 0));
    }

    @Test
    @DisplayName("admin dashboard includes all projects and users")
    void adminDashboardIncludesAllProjectsAndUsers() {
        User admin = user("admin", Role.ADMIN);
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Project project2 = Project.fromPersistence(2L, "project2", "Demo project", "admin", NOW, NOW);

        DashboardSummaryService service = new DashboardSummaryService(
                new InMemoryProjectRepository(project1, project2)
                        .withParticipants(project1.getId(),
                                List.of(ProjectMember.create(project1.getId(), dev.getLoginId(), NOW)))
                        .withParticipants(project2.getId(),
                                List.of(ProjectMember.create(project2.getId(), tester.getLoginId(), NOW)))
                        .rejectWrites(),
                new InMemoryIssueRepository(),
                new FakeStatisticsRepository(Map.of()),
                new InMemoryUserRepository(admin, dev, tester),
                new PermissionPolicy());

        List<DashboardProjectSummary> summaries = service.projectSummariesFor(admin);

        assertEquals(List.of("project1", "project2"), summaries.stream()
                .map(DashboardProjectSummary::projectName)
                .toList());
        assertEquals(List.of("admin", "dev", "tester"), service.usersFor(admin).stream()
                .map(UserResult::loginId)
                .toList());
    }

    @Test
    @DisplayName("non-admin dashboard includes only participating projects")
    void nonAdminDashboardIncludesOnlyParticipatingProjects() {
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Project project2 = Project.fromPersistence(2L, "project2", "Demo project", "admin", NOW, NOW);

        DashboardSummaryService service = new DashboardSummaryService(
                new InMemoryProjectRepository(project1, project2)
                        .withParticipants(project1.getId(),
                                List.of(ProjectMember.create(project1.getId(), dev.getLoginId(), NOW)))
                        .withParticipants(project2.getId(),
                                List.of(ProjectMember.create(project2.getId(), tester.getLoginId(), NOW)))
                        .rejectWrites(),
                new InMemoryIssueRepository(),
                new FakeStatisticsRepository(Map.of()),
                new InMemoryUserRepository(dev, tester),
                new PermissionPolicy());

        List<DashboardProjectSummary> summaries = service.projectSummariesFor(dev);

        assertEquals(List.of("project1"), summaries.stream()
                .map(DashboardProjectSummary::projectName)
                .toList());
        assertEquals(List.of(), service.usersFor(dev));
    }

    private static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "hash", role, true, NOW, NOW);
    }

    private static Issue issue(long id, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                PROJECT_ID,
                "Issue " + id,
                "Demo issue",
                user("reporter", Role.DEV))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .updatedAt(NOW));
    }

    private static final class FakeStatisticsRepository implements StatisticsRepository {

        private final Map<IssueStatus, Integer> statusCounts;

        private FakeStatisticsRepository(Map<IssueStatus, Integer> statusCounts) {
            this.statusCounts = Map.copyOf(statusCounts);
        }

        @Override
        public Map<IssueStatus, Integer> countByStatus(long projectId) {
            return statusCounts;
        }

        @Override
        public Map<Priority, Integer> countByPriority(long projectId) {
            return Map.of();
        }

        @Override
        public List<DailyIssueCount> countReportedIssuesByDay(long projectId) {
            return List.of();
        }

        @Override
        public List<DailyIssueCount> countReportedIssuesByDay(
                long projectId,
                LocalDate fromInclusive,
                LocalDate toInclusive) {
            return List.of();
        }

        @Override
        public List<MonthlyIssueCount> countReportedIssuesByMonth(long projectId) {
            return List.of();
        }

        @Override
        public List<MonthlyIssueCount> countReportedIssuesByMonth(
                long projectId,
                YearMonth fromInclusive,
                YearMonth toInclusive) {
            return List.of();
        }

        @Override
        public StatisticsReport buildReport(
                long projectId,
                LocalDate dailyFromInclusive,
                LocalDate dailyToInclusive,
                YearMonth monthlyFromInclusive,
                YearMonth monthlyToInclusive) {
            return StatisticsReportTestFactory.create(
                    countByStatus(projectId),
                    countByPriority(projectId),
                    List.of(),
                    List.of());
        }
    }

}
