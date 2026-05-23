package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.persistence.DatabaseEnvironment;
import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.service.LoginCheckService;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummaryService;
import com.github.marcellokim.issuetracker.ui.DemoDashboardPresenter;
import java.io.IOException;
import java.sql.SQLException;

public final class ApplicationBootstrap implements ApplicationRuntime {

    private RepositoryContext context;

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
        return new LoginCheckService(context().repositories().users());
    }

    @Override
    public DatabaseConnectionSummary databaseConnectionSummary() throws IOException, SQLException {
        RepositoryContext current = context();
        return connectionSummary(current.environment(), current.connectionProvider());
    }

    public ApplicationContext startUiContext() throws IOException, SQLException {
        var repositories = context().repositories();
        return new ApplicationContext(
                new AuthenticationService(repositories.users()),
                new DemoDashboardPresenter(new DashboardSummaryService(
                        repositories.projects(),
                        repositories.issues(),
                        repositories.statistics(),
                        repositories.users())));
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
                    new JdbcRepositoryFactory(connectionProvider));
        }
        return context;
    }

    private static DatabaseConnectionSummary connectionSummary(
            DatabaseEnvironment environment,
            DriverManagerConnectionProvider connectionProvider
    ) throws SQLException {
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
            JdbcRepositoryFactory repositories
    ) {
    }
}
