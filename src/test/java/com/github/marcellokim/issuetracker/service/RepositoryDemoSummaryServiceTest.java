package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
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
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.InMemoryUserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Repository demo summary service")
class RepositoryDemoSummaryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 15, 0);
    private static final long PROJECT_ID = 10L;

    @Test
    @DisplayName("builds the CLI demo summary without exposing repository calls to Main")
    void summarizesSeedRepositoryState() {
        User admin = user("admin", Role.ADMIN, true);
        User dev = user("dev1", Role.DEV, true);
        User tester = user("tester1", Role.TESTER, true);
        Project project = Project.fromPersistence(PROJECT_ID, "Project A", "Demo", "admin", NOW, NOW);
        Issue issue = issue(101L, IssueStatus.NEW);

        RepositoryDemoSummaryService service = new RepositoryDemoSummaryService(
                new InMemoryUserRepository(admin, dev, tester),
                new InMemoryProjectRepository(project).withParticipants(PROJECT_ID, List.of(
                        ProjectMember.create(PROJECT_ID, admin.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))).rejectWrites(),
                new InMemoryIssueRepository(issue),
                new FakeStatisticsRepository(),
                new FakeAssignmentRecommendationRepository(dev, tester));

        RepositoryDemoSummary summary = service.summarizeSeedDemo();

        assertTrue(summary.admin().isPresent());
        assertEquals("admin", summary.admin().orElseThrow().loginId());
        assertEquals(Role.ADMIN, summary.admin().orElseThrow().role());
        assertTrue(summary.admin().orElseThrow().active());

        RepositoryDemoSummary.ProjectSummary projectSummary = summary.project().orElseThrow();
        assertEquals("Project A", projectSummary.projectName());
        assertEquals(3, projectSummary.memberCount());
        assertEquals(1, projectSummary.activeDevCount());
        assertEquals(1, projectSummary.activeTesterCount());
        assertEquals(1, projectSummary.issueCount());
        assertEquals(2, projectSummary.statusCounts().get(IssueStatus.NEW));
        assertEquals(1, projectSummary.priorityCounts().get(Priority.MAJOR));
        assertEquals(1, projectSummary.devRecommendationCandidateCount());
        assertEquals(1, projectSummary.testerRecommendationCandidateCount());
    }

    @Test
    @DisplayName("uses explicit demo identifiers without hardcoding them in the service path")
    void summarizesExplicitDemoRequest() {
        User admin = user("root", Role.ADMIN, true);
        User dev = user("dev1", Role.DEV, true);
        User tester = user("tester1", Role.TESTER, true);
        Project project = Project.fromPersistence(PROJECT_ID, "custom-project", "Demo", "root", NOW, NOW);
        Issue issue = issue(101L, IssueStatus.NEW);

        RepositoryDemoSummaryService service = new RepositoryDemoSummaryService(
                new InMemoryUserRepository(admin, dev, tester),
                new InMemoryProjectRepository(project).withParticipants(PROJECT_ID, List.of(
                        ProjectMember.create(PROJECT_ID, admin.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))).rejectWrites(),
                new InMemoryIssueRepository(issue),
                new FakeStatisticsRepository(),
                new FakeAssignmentRecommendationRepository(dev, tester));

        RepositoryDemoSummary summary = service.summarize(new RepositoryDemoRequest(" root ", " custom-project "));

        assertEquals("root", summary.admin().orElseThrow().loginId());
        assertEquals("custom-project", summary.project().orElseThrow().projectName());
    }

    private static User user(String loginId, Role role, boolean active) {
        return User.fromPersistence(loginId, loginId, "hash", role, active, NOW, NOW);
    }

    private static Issue issue(long id, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                PROJECT_ID,
                "Issue " + id,
                "Demo issue",
                user("reporter", Role.DEV, true))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .updatedAt(NOW));
    }

    private static UnsupportedOperationException unexpectedRepositoryCall(String methodName) {
        return new UnsupportedOperationException("Unexpected repository call: " + methodName);
    }

    private static final class FakeStatisticsRepository implements StatisticsRepository {

        @Override
        public Map<IssueStatus, Integer> countByStatus(long projectId) {
            Map<IssueStatus, Integer> counts = new EnumMap<>(IssueStatus.class);
            counts.put(IssueStatus.NEW, 2);
            return counts;
        }

        @Override
        public Map<Priority, Integer> countByPriority(long projectId) {
            Map<Priority, Integer> counts = new EnumMap<>(Priority.class);
            counts.put(Priority.MAJOR, 1);
            return counts;
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
            throw unexpectedRepositoryCall("buildReport");
        }
    }

    private static final class FakeAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        private final User dev;
        private final User tester;

        private FakeAssignmentRecommendationRepository(User dev, User tester) {
            this.dev = dev;
            this.tester = tester;
        }

        @Override
        public List<AssignmentCandidate> findDevAssigneeCandidates(long projectId) {
            return List.of(AssignmentCandidate.create(dev, 1));
        }

        @Override
        public List<AssignmentCandidate> findTesterVerifierCandidates(long projectId) {
            return List.of(AssignmentCandidate.create(tester, 1));
        }
    }
}
