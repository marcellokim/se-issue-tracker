package com.github.marcellokim.issuetracker.controller;

import com.github.marcellokim.issuetracker.domain.Comment;
import com.github.marcellokim.issuetracker.domain.CommentPurpose;
import com.github.marcellokim.issuetracker.domain.Issue;
import com.github.marcellokim.issuetracker.domain.IssueHistory;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.domain.Priority;
import com.github.marcellokim.issuetracker.domain.Project;
import com.github.marcellokim.issuetracker.domain.ProjectMember;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.repository.AssignmentRecommendationRepository;
import com.github.marcellokim.issuetracker.repository.CommentRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository.DashboardProjectSnapshot;
import com.github.marcellokim.issuetracker.repository.DeletedIssueRepository;
import com.github.marcellokim.issuetracker.repository.IssueRepository;
import com.github.marcellokim.issuetracker.repository.IssueSearchCriteria;
import com.github.marcellokim.issuetracker.repository.StatisticsReport;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.DailyIssueCount;
import com.github.marcellokim.issuetracker.repository.StatisticsRepository.MonthlyIssueCount;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.support.FakeIssueDependencyRepository;
import com.github.marcellokim.issuetracker.support.FakeIssueHistoryRepository;
import com.github.marcellokim.issuetracker.support.InMemoryIssueRepository;
import com.github.marcellokim.issuetracker.support.InMemoryProjectRepository;
import com.github.marcellokim.issuetracker.support.SequentialIssueIdProvider;
import com.github.marcellokim.issuetracker.support.StatisticsReportTestFactory;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class ControllerTestSupport {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 19, 10, 0);
    static final long PROJECT_ID = 10L;

    private ControllerTestSupport() {
    }

    static IssueController issueController(User user, Issue... issues) {
        return issueController(user, new FakeCommentRepository(), issues);
    }

    static IssueController issueController(User user, FakeCommentRepository comments, Issue... issues) {
        FakeUserRepository users = new FakeUserRepository(user);
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, user.getLoginId());
        users.attachProjects(projects);
        SessionStore sessionStore = new SessionStore();
        sessionStore.start(user.getLoginId());
        AuthenticationService authService = new AuthenticationService(users, new PasswordHasher(), sessionStore);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        PermissionPolicy policy = new PermissionPolicy();
        IssueService issueService = new IssueService(
                projects,
                issueRepository,
                dependencies,
                comments,
                new FakeIssueHistoryRepository(),
                users,
                policy,
                new SequentialIssueIdProvider(),
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issueRepository,
                dependencies,
                comments,
                users,
                policy);
        return new IssueController(authService, issueService, workflowService);
    }

    static IssueController unauthenticatedIssueController(Issue... issues) {
        FakeUserRepository users = new FakeUserRepository(user("dev1", Role.DEV));
        InMemoryProjectRepository projects = new InMemoryProjectRepository(project(PROJECT_ID))
                .withParticipant(PROJECT_ID, "dev1");
        users.attachProjects(projects);
        FakeIssueDependencyRepository dependencies = new FakeIssueDependencyRepository();
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository(issues);
        PermissionPolicy policy = new PermissionPolicy();
        IssueService issueService = new IssueService(
                projects,
                issueRepository,
                dependencies,
                new FakeCommentRepository(),
                new FakeIssueHistoryRepository(),
                users,
                policy,
                new SequentialIssueIdProvider(),
                () -> NOW);
        IssueWorkflowService workflowService = new IssueWorkflowService(
                issueRepository,
                dependencies,
                new FakeCommentRepository(),
                users,
                policy);
        return new IssueController(anonymousAuth(), issueService, workflowService);
    }

    static Issue persistedIssue(long id, String issueId, User reporter) {
        return Issue.fromPersistence(Issue.persistedState(
                PROJECT_ID,
                "Issue " + id,
                "Controller test issue " + id,
                reporter)
                .id(id)
                .issueId(issueId)
                .reportedDate(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.NEW)
                .updatedAt(NOW));
    }

    static ProjectController projectController(
            AuthFixture auth,
            InMemoryProjectRepository projects,
            FakeUserRepository users) {
        return projectController(auth, projects, users, new InMemoryIssueRepository());
    }

    static ProjectController projectController(
            AuthFixture auth,
            InMemoryProjectRepository projects,
            FakeUserRepository users,
            InMemoryIssueRepository issues) {
        return new ProjectController(auth.service(), projectService(projects, users, issues));
    }

    static ProjectService projectService(
            InMemoryProjectRepository projects,
            FakeUserRepository users) {
        return projectService(projects, users, new InMemoryIssueRepository());
    }

    static ProjectService projectService(
            InMemoryProjectRepository projects,
            FakeUserRepository users,
            InMemoryIssueRepository issues) {
        users.attachProjects(projects);
        return new ProjectService(
                projects,
                issues,
                users,
                new PermissionPolicy(),
                () -> NOW);
    }

    static Project project(long projectId, String name) {
        return Project.fromPersistence(projectId, name, "description", "admin", NOW, NOW);
    }

    static AssignmentController assignmentController(AuthFixture auth, FakeIssueRepository issues) {
        return new AssignmentController(
                auth.service(),
                new AssignmentService(
                        issues,
                        auth.users(),
                        new PermissionPolicy(),
                        new AssignmentRecommendationService(
                                new FakeAssignmentRecommendationRepository(),
                                new KNNAssignmentRecommendation()),
                        () -> NOW));
    }

    static Issue issueWithAssigneeAndVerifier(
            long id,
            long projectId,
            IssueStatus status,
            User assignee,
            User verifier) {
        return Issue.fromPersistence(Issue.persistedState(
                projectId,
                "Issue " + id,
                "Controller test issue",
                user("reporter", Role.DEV))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .updatedAt(NOW)
                .priority(Priority.MAJOR)
                .status(status)
                .assignee(assignee)
                .verifier(verifier));
    }

    static Issue issueWithCompletedOwners(long id, long projectId, User fixer, User resolver) {
        return Issue.fromPersistence(Issue.persistedState(
                projectId,
                "Issue " + id,
                "Controller test issue",
                user("reporter", Role.DEV))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .updatedAt(NOW)
                .priority(Priority.MAJOR)
                .status(IssueStatus.RESOLVED)
                .fixer(fixer)
                .resolver(resolver));
    }

    static String nextCommentId() {
        return "COMMENT-test-" + UUID.randomUUID();
    }

    static AuthFixture authenticated(Role role) {
        User user = user(role.name().toLowerCase(Locale.ROOT), role);
        SessionStore sessionStore = new SessionStore();
        sessionStore.start(user.getLoginId());
        FakeUserRepository users = new FakeUserRepository(user);
        return new AuthFixture(
                new AuthenticationService(users, new PasswordHasher(), sessionStore),
                users,
                user);
    }

    static AuthenticationService anonymousAuth() {
        return new AuthenticationService(new FakeUserRepository(), new PasswordHasher(), new SessionStore());
    }

    static User user(String loginId, Role role) {
        return User.fromPersistence(loginId, loginId, "stored-password", role, true, NOW, NOW);
    }

    static Project project(long projectId) {
        return Project.fromPersistence(projectId, "project-" + projectId, "demo project", "admin", NOW, NOW);
    }

    static Issue issue(long id, long projectId, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                projectId,
                "Issue " + id,
                "Controller test issue",
                user("reporter", Role.DEV))
                .id(id)
                .issueId("ISSUE-" + id)
                .reportedDate(NOW)
                .updatedAt(NOW)
                .priority(Priority.MAJOR)
                .status(status));
    }

    static Issue copyWithStatus(Issue issue, IssueStatus status) {
        return Issue.fromPersistence(Issue.persistedState(
                issue.projectId(),
                issue.title(),
                issue.description(),
                user(issue.reporterId(), Role.DEV))
                .id(issue.id())
                .issueId(issue.getIssueId())
                .reportedDate(issue.reportedDate())
                .updatedAt(NOW.plusMinutes(1))
                .priority(issue.priority())
                .status(status));
    }

    static StatisticsReport report() {
        Map<IssueStatus, Integer> statusCounts = new EnumMap<>(IssueStatus.class);
        statusCounts.put(IssueStatus.NEW, 1);
        Map<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
        priorityCounts.put(Priority.MAJOR, 1);
        return StatisticsReportTestFactory.create(
                statusCounts,
                priorityCounts,
                List.of(new DailyIssueCount(LocalDate.of(2026, 5, 19), 1)),
                List.of(new MonthlyIssueCount(YearMonth.of(2026, 5), 1)));
    }

    static DashboardProjectSnapshot dashboardProject(long projectId, String name, int visibleIssues) {
        return new DashboardProjectSnapshot(
                projectId,
                name,
                "description",
                1,
                0,
                0,
                0,
                visibleIssues,
                Map.of(IssueStatus.NEW, visibleIssues));
    }

    record AuthFixture(AuthenticationService service, FakeUserRepository users, User user) {
    }

    static final class FakeDashboardSummaryRepository implements DashboardSummaryRepository {

        private final List<DashboardProjectSnapshot> allProjectSummaries;
        private final List<DashboardProjectSnapshot> participantProjectSummaries;

        FakeDashboardSummaryRepository(
                List<DashboardProjectSnapshot> allProjectSummaries,
                List<DashboardProjectSnapshot> participantProjectSummaries) {
            this.allProjectSummaries = List.copyOf(allProjectSummaries);
            this.participantProjectSummaries = List.copyOf(participantProjectSummaries);
        }

        @Override
        public List<DashboardProjectSnapshot> findAllProjectSummaries() {
            return allProjectSummaries;
        }

        @Override
        public List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId) {
            return participantProjectSummaries;
        }
    }

    static final class FakeIssueRepository implements IssueRepository, DeletedIssueRepository {

        private final Map<Long, Issue> issuesById = new LinkedHashMap<>();
        String lastChangedBy;
        String lastRestoreMessage;
        int lastPurgeLimit;
        long lastPurgedIssueId;

        FakeIssueRepository(Issue... issues) {
            for (Issue issue : issues) {
                issuesById.put(issue.id(), issue);
            }
        }

        @Override
        public Optional<Issue> findById(long issueId) {
            return Optional.ofNullable(issuesById.get(issueId));
        }

        @Override
        public List<Issue> findAllById(List<Long> issueIds) {
            return issueIds.stream()
                    .filter(issuesById::containsKey)
                    .map(issuesById::get)
                    .toList();
        }

        public List<Issue> findByProject(long projectId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .toList();
        }

        @Override
        public List<Issue> findDeletedByProject(long projectId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .filter(issue -> issue.status() == IssueStatus.DELETED)
                    .toList();
        }

        @Override
        public List<Issue> findByCriteria(IssueSearchCriteria criteria) {
            return new ArrayList<>(issuesById.values());
        }

        @Override
        public boolean existsByProjectIdAndTitle(long projectId, String title) {
            return issuesById.values().stream()
                    .anyMatch(issue -> issue.projectId() == projectId && issue.title().equals(title));
        }

        @Override
        public boolean existsByProjectIdAndTitleExcludingIssueId(long projectId, String title, long excludedIssueId) {
            return issuesById.values().stream()
                    .anyMatch(issue -> issue.id() != excludedIssueId
                            && issue.projectId() == projectId
                            && issue.title().equals(title));
        }

        @Override
        public boolean hasCurrentIssueResponsibility(String userLoginId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.status() == IssueStatus.ASSIGNED || issue.status() == IssueStatus.FIXED)
                    .anyMatch(issue -> userLoginId.equals(issue.assigneeId())
                            || userLoginId.equals(issue.verifierId()));
        }

        @Override
        public boolean hasCurrentIssueResponsibility(long projectId, String loginId) {
            return issuesById.values().stream()
                    .filter(issue -> issue.projectId() == projectId)
                    .filter(issue -> issue.status() == IssueStatus.ASSIGNED || issue.status() == IssueStatus.FIXED)
                    .anyMatch(issue -> loginId.equals(issue.assigneeId()) || loginId.equals(issue.verifierId()));
        }

        @Override
        public Issue save(Issue issue) {
            issuesById.put(issue.id(), issue);
            return issue;
        }

        @Override
        public Issue softDelete(Issue issue, String changedById, String message, LocalDateTime changedDate) {
            lastChangedBy = changedById;
            Issue deleted = copyWithStatus(issue, IssueStatus.DELETED);
            issuesById.put(issue.id(), deleted);
            return deleted;
        }

        @Override
        public Issue restore(Issue issue, String changedById, String message, LocalDateTime changedDate) {
            lastChangedBy = changedById;
            lastRestoreMessage = message;
            Issue restored = copyWithStatus(issue, IssueStatus.NEW);
            issuesById.put(issue.id(), restored);
            return restored;
        }

        @Override
        public int purgeDeletedById(long issueId) {
            Issue issue = issuesById.get(issueId);
            if (issue == null || issue.status() != IssueStatus.DELETED) {
                return 0;
            }
            issuesById.remove(issueId);
            lastPurgedIssueId = issueId;
            return 1;
        }

        @Override
        public int purgeDeletedBeyondLimit(long projectId, int maxDeletedIssues) {
            lastPurgeLimit = maxDeletedIssues;
            return 2;
        }
    }

    static final class FakeUserRepository implements UserRepository {

        private final Map<String, User> usersByLoginId = new LinkedHashMap<>();
        private InMemoryProjectRepository projects;

        FakeUserRepository(User... users) {
            for (User user : users) {
                usersByLoginId.put(user.getLoginId(), user);
            }
        }

        void attachProjects(InMemoryProjectRepository projects) {
            this.projects = Objects.requireNonNull(projects, "projects");
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return Optional.ofNullable(usersByLoginId.get(loginId));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersByLoginId.values());
        }

        @Override
        public List<User> findByRole(long projectId, Role role) {
            if (projects == null) {
                return usersByLoginId.values().stream()
                        .filter(user -> user.getRole() == role)
                        .toList();
            }

            return projects.findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .map(usersByLoginId::get)
                    .filter(Objects::nonNull)
                    .filter(user -> user.getRole() == role)
                    .toList();
        }

        @Override
        public boolean existsActiveProjectMember(long projectId, String loginId) {
            User user = usersByLoginId.get(loginId);
            if (user == null || !user.isActive()) {
                return false;
            }
            return projects == null || projects.findParticipants(projectId).stream()
                    .map(ProjectMember::userId)
                    .anyMatch(loginId::equals);
        }

        @Override
        public User save(User user) {
            usersByLoginId.put(user.getLoginId(), user);
            return user;
        }
    }

    static final class FakeStatisticsRepository implements StatisticsRepository {

        StatisticsReport report = report();
        long reportProjectId;
        LocalDate dailyFrom;
        YearMonth monthlyTo;

        @Override
        public StatisticsReport calculateProjectStatistics(
                long projectId,
                LocalDate dailyFromInclusive,
                LocalDate dailyToInclusive,
                YearMonth monthlyFromInclusive,
                YearMonth monthlyToInclusive) {
            reportProjectId = projectId;
            dailyFrom = dailyFromInclusive;
            monthlyTo = monthlyToInclusive;
            return report;
        }
    }

    static final class FakeCommentRepository implements CommentRepository {

        private final Map<Long, Comment> comments = new LinkedHashMap<>();
        private long nextId = 100L;

        FakeCommentRepository(Comment... comments) {
            for (Comment comment : comments) {
                saveInternal(comment);
            }
        }

        @Override
        public Optional<Comment> findById(long commentId) {
            return Optional.ofNullable(comments.get(commentId));
        }

        @Override
        public List<Comment> findByIssueId(long issueId) {
            return comments.values().stream()
                    .filter(comment -> comment.issueId() == issueId)
                    .toList();
        }

        private Comment saveInternal(Comment comment) {
            if (comment.id() != 0L) {
                comments.put(comment.id(), comment);
                nextId = Math.max(nextId, comment.id() + 1L);
                return comment;
            }
            Comment saved = Comment.fromPersistence(
                    nextId++,
                    comment.issueId(),
                    comment.writerId(),
                    comment.content(),
                    comment.purpose(),
                    comment.createdDate(),
                    comment.updatedDate());
            comments.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Comment saveCommentAndRecordHistory(Comment comment, IssueHistory history) {
            return saveInternal(comment);
        }

        private void deleteGeneralInternal(long issueId, long commentId, String writerLoginId) {
            Comment comment = comments.get(commentId);
            if (comment == null
                    || comment.issueId() != issueId
                    || !Objects.equals(comment.writerId(), writerLoginId)
                    || comment.purpose() != CommentPurpose.GENERAL) {
                throw new IllegalArgumentException(
                        "Comment was not deleted because it does not exist, is not owned by the writer, "
                                + "or is not a GENERAL comment.");
            }
            comments.remove(commentId);
        }

        @Override
        public void deleteGeneralByIdAndRecordIssueChange(
                long issueId,
                long commentId,
                String writerLoginId,
                IssueHistory history) {
            deleteGeneralInternal(issueId, commentId, writerLoginId);
        }
    }

    static final class FakeAssignmentRecommendationRepository implements AssignmentRecommendationRepository {

        private final List<IssueRecommendationData> resolvedIssues = new ArrayList<>();
        private final List<User> candidates = new ArrayList<>();

        void addResolvedIssue(IssueRecommendationData data) {
            resolvedIssues.add(data);
        }

        void addCandidate(User user) {
            candidates.add(user);
        }

        @Override
        public List<IssueRecommendationData> findResolvedIssuesForRecommendation(long projectId) {
            return List.copyOf(resolvedIssues);
        }

        @Override
        public List<User> findActiveDevCandidates(long projectId) {
            return candidates.stream().filter(user -> user.getRole() == Role.DEV && user.isActive()).toList();
        }

        @Override
        public List<User> findActiveTesterCandidates(long projectId) {
            return candidates.stream().filter(user -> user.getRole() == Role.TESTER && user.isActive()).toList();
        }
    }
}
