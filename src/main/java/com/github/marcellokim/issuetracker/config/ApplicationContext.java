package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.ui.DemoDashboardPresenter;
import java.io.IOException;
import java.sql.SQLException;

public record ApplicationContext(
        AuthenticationService authenticationService,
        DemoDashboardPresenter dashboardPresenter
) {

    public static ApplicationContext fromEnvironment() throws IOException, SQLException {
        if (!hasText(System.getenv("ITS_DB_URL"))
                || !hasText(System.getenv("ITS_DB_USER"))
                || !hasText(System.getenv("ITS_DB_PASSWORD"))) {
            throw new IllegalStateException(
                    "Oracle environment is missing. Set ITS_DB_URL, ITS_DB_USER, ITS_DB_PASSWORD."
            );
        }

        var connectionProvider = DriverManagerConnectionProvider.fromEnvironment();
        DatabaseInitializer.initialize(connectionProvider);

        var repositories = new JdbcRepositoryFactory(connectionProvider);
        return new ApplicationContext(
                new AuthenticationService(repositories.users()),
                new DemoDashboardPresenter(
                        repositories.projects(),
                        repositories.issues(),
                        repositories.statistics(),
                        repositories.users()
                )
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
