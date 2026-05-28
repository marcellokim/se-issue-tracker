package com.github.marcellokim.issuetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        statusCounts.put(IssueStatus.DELETED, 1);

        DashboardSummaryService service = new DashboardSummaryService(
                new FakeProjectRepository(project, List.of(
                        ProjectMember.create(PROJECT_ID, pl.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, dev.getLoginId(), NOW),
                        ProjectMember.create(PROJECT_ID, tester.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(activeIssue), List.of(deletedIssue)),
                new FakeStatisticsRepository(statusCounts),
                new FakeUserRepository(List.of(pl, dev, tester)),
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
                new FakeProjectRepository(
                        List.of(project1, project2),
                        Map.of(
                                project1.getId(),
                                List.of(ProjectMember.create(project1.getId(), dev.getLoginId(), NOW)),
                                project2.getId(),
                                List.of(ProjectMember.create(project2.getId(), tester.getLoginId(), NOW)))),
                new FakeIssueRepository(List.of(), List.of()),
                new FakeStatisticsRepository(Map.of()),
                new FakeUserRepository(List.of(admin, dev, tester)),
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
    @DisplayName("admin does not see issues via relatedIssuesFor")
    void adminDoesNotSeeRelatedIssues() {
        User admin = user("admin", Role.ADMIN);
        User dev = user("dev", Role.DEV);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Issue issue1 = issue(101L, IssueStatus.NEW);

        DashboardSummaryService service = new DashboardSummaryService(
                new FakeProjectRepository(project1, List.of(
                        ProjectMember.create(project1.getId(), dev.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(issue1), List.of()),
                new FakeStatisticsRepository(Map.of()),
                new FakeUserRepository(List.of(admin, dev)),
                new PermissionPolicy());

        List<IssueSummary> issues = service.relatedIssuesFor(admin);

        assertEquals(List.of(), issues);
    }

    @Test
    @DisplayName("PL sees all issues in participating projects via relatedIssuesFor")
    void plSeesAllProjectIssues() {
        User pl = user("pl", Role.PL);
        User reporter = user("reporter", Role.DEV);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Issue issue1 = issue(101L, IssueStatus.NEW);
        Issue issue2 = issue(102L, IssueStatus.ASSIGNED);

        DashboardSummaryService service = new DashboardSummaryService(
                new FakeProjectRepository(project1, List.of(
                        ProjectMember.create(project1.getId(), pl.getLoginId(), NOW),
                        ProjectMember.create(project1.getId(), reporter.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(issue1, issue2), List.of()),
                new FakeStatisticsRepository(Map.of()),
                new FakeUserRepository(List.of(pl, reporter)),
                new PermissionPolicy());

        List<IssueSummary> issues = service.relatedIssuesFor(pl);

        assertEquals(List.of(101L, 102L), issues.stream()
                .map(IssueSummary::id)
                .toList());
    }

    @Test
    @DisplayName("dev sees only related issues in participating projects via relatedIssuesFor")
    void devSeesOnlyRelatedIssues() {
        User dev = user("dev", Role.DEV);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Issue issue1 = issue(101L, IssueStatus.NEW);

        DashboardSummaryService service = new DashboardSummaryService(
                new FakeProjectRepository(project1, List.of(
                        ProjectMember.create(project1.getId(), dev.getLoginId(), NOW))),
                new FakeIssueRepository(List.of(issue1), List.of()),
                new FakeStatisticsRepository(Map.of()),
                new FakeUserRepository(List.of(dev)),
                new PermissionPolicy());

        List<IssueSummary> issues = service.relatedIssuesFor(dev);

        assertEquals(0, issues.size());
    }

    @Test
    @DisplayName("non-admin dashboard includes only participating projects")
    void nonAdminDashboardIncludesOnlyParticipatingProjects() {
        User dev = user("dev", Role.DEV);
        User tester = user("tester", Role.TESTER);
        Project project1 = Project.fromPersistence(1L, "project1", "Demo project", "admin", NOW, NOW);
        Project project2 = Project.fromPersistence(2L, "project2", "Demo project", "admin", NOW, NOW);

        DashboardSummaryService service = new DashboardSummaryService(
                new FakeProjectRepository(
                        List.of(project1, project2),
                        Map.of(
                                project1.getId(),
                                List.of(ProjectMember.create(project1.getId(), dev.getLoginId(), NOW)),
                                project2.getId(),
                                List.of(ProjectMember.create(project2.getId(), tester.getLoginId(), NOW)))),
                new FakeIssueRepository(List.of(), List.of()),
                new FakeStatisticsRepository(Map.of()),
                new FakeUserRepository(List.of(dev, tester)),
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

    private static final class FakeProjectRepository implements ProjectRepository {

        private final List<Project> projects;
        private final Map<Long, List<ProjectMember>> membersByProjectId;

        private FakeProjectRepository(Project project, List<ProjectMember> members) {
            this(List.of(project), Map.of(project.getId(), members));
        }

        private FakeProjectRepository(List<Project> projects, Map<Long, List<ProjectMember>> membersByProjectId) {
            this.projects = List.copyOf(projects);
            this.membersByProjectId = Map.copyOf(membersByProjectId);
        }

        @Override
        public Optional<Project> findById(long projectId) {
            return projects.stream()
                    .filter(project -> project.getId() == projectId)
                    .findFirst();
        }

        @Override
        public Optional<Project> findByName(String name) {
            return projects.stream()
                    .filter(project -> project.getName().equals(name))
                    .findFirst();
        }

        @Override
        public List<Project> findAll() {
            return projects;
        }

        @Override
        public Project save(Project project) {
            throw new UnsupportedOperationException("save is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public void deleteById(long projectId) {
            throw new UnsupportedOperationException("deleteById is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public void addParticipant(long projectId, String userLoginId) {
            throw new UnsupportedOperationException("addParticipant is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public void removeParticipant(long projectId, String userLoginId) {
            throw new UnsupportedOperationException("removeParticipant is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public List<ProjectMember> findParticipants(long projectId) {
            return membersByProjectId.getOrDefault(projectId, List.of());
        }

        @Override
        public boolean existsByParticipant(String userLoginId) {
            return membersByProjectId.values().stream()
                    .flatMap(List::stream)
                    .map(ProjectMember::userId)
                    .anyMatch(userLoginId::equals);
        }
    }

    private static final class FakeIssueRepository implements IssueRepository {

        private final List<Issue> activeIssues;
        private final List<Issue> deletedIssues;

        private FakeIssueRepository(List<Issue> activeIssues, List<Issue> deletedIssues) {
            this.activeIssues = List.copyOf(activeIssues);
            this.deletedIssues = List.copyOf(deletedIssues);
        }

        @Override
        public Optional<Issue> findById(long issueId) {
            return activeIssues.stream()
                    .filter(issue -> issue.id() == issueId)
                    .findFirst();
        }

        @Override
        public List<Issue> findAllById(List<Long> issueIds) {
            return activeIssues.stream()
                    .filter(issue -> issueIds.contains(issue.id()))
                    .toList();
        }

        @Override
        public List<Issue> findByProject(long projectId) {
            return activeIssues;
        }

        @Override
        public List<Issue> findDeletedByProject(long projectId) {
            return deletedIssues;
        }

        @Override
        public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
            throw new UnsupportedOperationException("findByCriteria is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public boolean existsByProjectIdAndTitle(long projectId, String title) {
            return activeIssues.stream()
                    .anyMatch(issue -> issue.projectId() == projectId && issue.title().equals(title))
                    || deletedIssues.stream()
                            .anyMatch(issue -> issue.projectId() == projectId && issue.title().equals(title));
        }

        @Override
        public boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId) {
            return activeIssues.stream()
                    .anyMatch(issue -> issue.id() != excludedIssueId
                            && issue.projectId() == projectId
                            && issue.title().equals(title))
                    || deletedIssues.stream()
                            .anyMatch(issue -> issue.id() != excludedIssueId
                                    && issue.projectId() == projectId
                                    && issue.title().equals(title));
        }

        @Override
        public boolean existsByResponsibleUser(String userLoginId) {
            return activeIssues.stream()
                    .filter(issue -> issue.status() != IssueStatus.DELETED)
                    .anyMatch(issue -> userLoginId.equals(issue.assigneeId())
                            || userLoginId.equals(issue.verifierId())
                            || userLoginId.equals(issue.fixerId())
                            || userLoginId.equals(issue.resolverId()));
        }

        @Override
        public boolean existsActiveAssignmentByProjectAndUser(long projectId, String loginId) {
            throw new UnsupportedOperationException(
                    "existsActiveAssignmentByProjectAndUser is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public Issue save(Issue issue) {
            throw new UnsupportedOperationException("save is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public Issue softDelete(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("softDelete is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public Issue restore(long issueId, String changedById, String message, LocalDateTime changedDate) {
            throw new UnsupportedOperationException("restore is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            throw new UnsupportedOperationException(
                    "purgeDeletedBeyondLimit is not needed by DashboardSummaryServiceTest.");
        }

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
            return StatisticsReport.create(
                    countByStatus(projectId),
                    countByPriority(projectId),
                    List.of(),
                    List.of());
        }
    }

    private static final class FakeUserRepository implements UserRepository {

        private final List<User> users;

        private FakeUserRepository(List<User> users) {
            this.users = List.copyOf(users);
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
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public User save(User user) {
            throw new UnsupportedOperationException("save is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public void activate(String loginId) {
            throw new UnsupportedOperationException("activate is not needed by DashboardSummaryServiceTest.");
        }

        @Override
        public void deactivate(String loginId) {
            throw new UnsupportedOperationException("deactivate is not needed by DashboardSummaryServiceTest.");
        }
    }
}
