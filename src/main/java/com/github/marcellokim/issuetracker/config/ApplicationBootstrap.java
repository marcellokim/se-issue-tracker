package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.controller.AssignmentController;
import com.github.marcellokim.issuetracker.controller.AuthenticationController;
import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.controller.DashboardController;
import com.github.marcellokim.issuetracker.controller.DeletedIssueController;
import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.controller.IssueStateController;
import com.github.marcellokim.issuetracker.controller.ProjectController;
import com.github.marcellokim.issuetracker.controller.StatisticsController;
import com.github.marcellokim.issuetracker.persistence.DatabaseEnvironment;
import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.repository.UserRepository;
import com.github.marcellokim.issuetracker.service.AssignmentRecommendationService;
import com.github.marcellokim.issuetracker.service.AssignmentService;
import com.github.marcellokim.issuetracker.service.KNNAssignmentRecommendation;
import com.github.marcellokim.issuetracker.service.AccountService;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.technical.SystemClock;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.DeletedIssueService;
import com.github.marcellokim.issuetracker.service.IssueService;
import com.github.marcellokim.issuetracker.service.IssueStateService;
import com.github.marcellokim.issuetracker.service.IssueWorkflowService;
import com.github.marcellokim.issuetracker.service.LoginCheckService;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.service.PermissionPolicy;
import com.github.marcellokim.issuetracker.service.ProjectService;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummaryService;
import com.github.marcellokim.issuetracker.service.StatisticsService;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import com.github.marcellokim.issuetracker.service.CommentIdProvider;
import com.github.marcellokim.issuetracker.technical.CommentIdGenerator;

import java.io.IOException;
import java.sql.SQLException;

public final class ApplicationBootstrap implements ApplicationRuntime {

        private RepositoryContext context;
        private final PasswordHashing passwordHashing = new PasswordHasher();

        @Override
        public boolean hasDatabaseEnvironment() {
                return DatabaseEnvironment.isSystemConfigured();
        }

        @Override
        public RepositoryDemoSummaryService repositoryDemoSummaryService() throws IOException, SQLException {
                var repositories = context().repositories();
                return new RepositoryDemoSummaryService(
                                repositories.users(),
                                repositories.projects(),
                                repositories.issues(),
                                repositories.statistics(),
                                repositories.assignmentRecommendations());
        }

        @Override
        public LoginCheckService loginCheckService() throws IOException, SQLException {
                var users = context().repositories().users();
                return new LoginCheckService(users, authenticationService(users));
        }

        @Override
        public DatabaseConnectionSummary databaseConnectionSummary() throws IOException, SQLException {
                RepositoryContext current = context();
                return connectionSummary(current.environment(), current.connectionProvider());
        }

        public ApplicationContext startUiContext() throws IOException, SQLException {
                var repositories = context().repositories();
                var users = repositories.users();
                var projects = repositories.projects();
                var issues = repositories.issues();
                var comments = repositories.comments();
                var issueHistory = repositories.issueHistory();
                var issueDependencies = repositories.issueDependencies();
                var statistics = repositories.statistics();
                var assignmentRecommendations = repositories.assignmentRecommendations();
                var dashboardSummaries = repositories.dashboardSummaries();
                PermissionPolicy permissionPolicy = new PermissionPolicy();
                Clock clock = new SystemClock();
                CommentIdProvider commentIdProvider = new CommentIdGenerator();
                AuthenticationService authenticationService = authenticationService(users);
                AccountService accountService = new AccountService(
                                permissionPolicy,
                                users,
                                projects,
                                issues,
                                passwordHashing,
                                clock);
                IssueService issueService = new IssueService(
                                projects,
                                issues,
                                issueDependencies,
                                comments,
                                issueHistory,
                                users,
                                permissionPolicy,
                                clock);
                AssignmentService assignmentService = new AssignmentService(
                                issues,
                                users,
                                permissionPolicy,
                                new AssignmentRecommendationService(issues, users, new KNNAssignmentRecommendation()),
                                clock);
                IssueStateService issueStateService = new IssueStateService(
                                issues,
                                issueDependencies,
                                users,
                                permissionPolicy,
                                clock,
                                commentIdProvider);
                IssueWorkflowService issueWorkflowService = new IssueWorkflowService(
                                issues,
                                issueDependencies,
                                comments,
                                users,
                                permissionPolicy);
                DeletedIssueService deletedIssueService = new DeletedIssueService(
                                issues,
                                users,
                                permissionPolicy,
                                clock);
                ProjectService projectService = new ProjectService(
                                projects,
                                issues,
                                users,
                                permissionPolicy,
                                clock);
                StatisticsService statisticsService = new StatisticsService(permissionPolicy, statistics, users);
                DashboardSummaryService dashboardSummaryService = new DashboardSummaryService(
                                dashboardSummaries,
                                users,
                                permissionPolicy);
                return new ApplicationContext(
                                new AuthenticationController(authenticationService),
                                new AccountController(authenticationService, accountService),
                                new DashboardController(authenticationService, dashboardSummaryService),
                                new ProjectController(authenticationService, projectService),
                                new IssueController(authenticationService, issueService, issueWorkflowService),
                                new AssignmentController(authenticationService, assignmentService),
                                new IssueStateController(authenticationService, issueStateService),
                                new DeletedIssueController(authenticationService, deletedIssueService),
                                new StatisticsController(authenticationService, statisticsService));
        }

        private synchronized RepositoryContext context() throws IOException, SQLException {
                if (context == null) {
                        // JavaFX와 CLI 진단 경로가 동시에 runtime을 요청해도 DB 초기화는 한 번만 수행함.
                        DatabaseEnvironment environment = DatabaseEnvironment.fromSystem();
                        var connectionProvider = DriverManagerConnectionProvider.from(environment);
                        DatabaseInitializer.initializeApplication(connectionProvider);
                        context = new RepositoryContext(
                                        environment,
                                        connectionProvider,
                                        new JdbcRepositoryFactory(connectionProvider, passwordHashing));
                }
                return context;
        }

        private AuthenticationService authenticationService(UserRepository users) {
                return new AuthenticationService(users, passwordHashing, new SessionStore());
        }

        private static DatabaseConnectionSummary connectionSummary(
                        DatabaseEnvironment environment,
                        DriverManagerConnectionProvider connectionProvider) throws SQLException {
                String sql = """
                                select sys_context('USERENV', 'CURRENT_SCHEMA') as current_schema,
                                       sys_context('USERENV', 'CON_NAME') as container_name
                                from dual
                                """;
                try (var connection = connectionProvider.getConnection();
                                var statement = connection.prepareStatement(sql);
                                var resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                                return new DatabaseConnectionSummary(
                                                environment.url(),
                                                environment.user(),
                                                resultSet.getString("current_schema"),
                                                resultSet.getString("container_name"));
                        }
                }
                return new DatabaseConnectionSummary(environment.url(), environment.user(), "", "");
        }

        private record RepositoryContext(
                        DatabaseEnvironment environment,
                        DriverManagerConnectionProvider connectionProvider,
                        JdbcRepositoryFactory repositories) {
        }
}
