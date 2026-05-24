package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.domain.AssignmentCandidate;
import com.github.marcellokim.issuetracker.domain.DailyIssueCount;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.StatisticsReport;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.ProjectRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Project project = Project.fromPersistence(PROJECT_ID, "project1", "Demo", "admin", NOW, NOW);
        Issue issue = issue(101L, IssueStatus.NEW);

        RepositoryDemoSummaryService service = new RepositoryDemoSummaryService(
                new FakeUserRepository(List.of(admin, dev, tester)),
                new FakeProjectRepository(project, List.of(
                        ProjectMember.create(PROJECT_ID, admin.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(issue)),
                new FakeStatisticsRepository(),
                new FakeAssignmentRecommendationRepository(dev, tester));

        RepositoryDemoSummary summary = service.summarizeSeedDemo();

        assertTrue(summary.admin().isPresent());
        assertEquals("admin", summary.admin().orElseThrow().loginId());
        assertEquals(Role.ADMIN, summary.admin().orElseThrow().role());
        assertTrue(summary.admin().orElseThrow().active());

        RepositoryDemoSummary.ProjectSummary projectSummary = summary.project().orElseThrow();
        assertEquals("project1", projectSummary.projectName());
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
                new FakeUserRepository(List.of(admin, dev, tester)),
                new FakeProjectRepository(project, List.of(
                        ProjectMember.create(PROJECT_ID, admin.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(issue)),
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

    private static final class FakeUserRepository implements UserRepository {

        private final List<User> users;

        private FakeUserRepository(List<User> users) {
            this.users = List.copyOf(users);
        }

        @Override
        public Optional<User> findById(String userId) {
            return findByLoginId(userId);
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return users.stream()
                    .filter(user -> user.getLoginId().equals(loginId))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return users;
        }

        @Override
        public List<User> findByRole(long projectId, Role role) {
            return users.stream()
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public List<User> findActiveByRole(long projectId, Role role) {
            return users.stream()
                    .filter(User::isActive)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            throw new UnsupportedOperationException("save is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void activate(String loginId) {
            throw new UnsupportedOperationException("activate is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void deactivate(String loginId) {
            throw new UnsupportedOperationException("deactivate is not needed by RepositoryDemoSummaryServiceTest.");
        }
    }

    private static final class FakeProjectRepository implements ProjectRepository {

        private final Project project;
        private final List<ProjectMember> members;

        private FakeProjectRepository(Project project, List<ProjectMember> members) {
            this.project = project;
            this.members = List.copyOf(members);
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return project.getId() == projectId ? Optional.of(project) : Optional.empty();
        }

        @Override
        public Optional<Project> findByName(String name) {
            return project.getName().equals(name) ? Optional.of(project) : Optional.empty();
        }

        @Override
        public List<Project> findAll() {
            return List.of(project);
        }

        @Override
        public Project save(Project project) {
            throw new UnsupportedOperationException("save is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void deleteById(long projectId) {
            throw new UnsupportedOperationException("deleteById is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
            throw new UnsupportedOperationException("addParticipant is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
            throw new UnsupportedOperationException(
                    "removeParticipant is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return project.getId() == projectId ? members : List.of();
        }
    }

    private static final class FakeIssueRepository implements IssueRepository {

        private final List<Issue> issues;

        private FakeIssueRepository(List<Issue> issues) {
            this.issues = List.copyOf(issues);
        }

        @Override
        public Optional<Issue> findById(long issueId) {
            return issues.stream()
                    .filter(issue -> issue.id() == issueId)
                    .findFirst();
        }

        @Override
        public List<Issue> findAllById(List<Long> issueIds) {
            return List.of();
        }

        @Override
        public List<Issue> findByProject(long projectId) {
            return issues.stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .toList();
        }

        @Override
        public List<Issue> findDeletedByProject(long projectId) {
            return List.of();
        }

        @Override
        public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
            return issues;
        }

        @Override
        public Issue save(Issue issue) {
            throw new UnsupportedOperationException("save is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("softDelete is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("restore is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            throw new UnsupportedOperationException(
                    "purgeDeletedBeyondLimit is not needed by RepositoryDemoSummaryServiceTest.");
        }

        @Override
        public void purge(long issueId) {
            throw new UnsupportedOperationException("purge is not needed by RepositoryDemoSummaryServiceTest.");
        }
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
            throw new UnsupportedOperationException("buildReport is not needed by RepositoryDemoSummaryServiceTest.");
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
