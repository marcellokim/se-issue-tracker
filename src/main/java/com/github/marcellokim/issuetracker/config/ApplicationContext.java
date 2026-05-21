package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.persistence.DatabaseInitializer;
import com.github.marcellokim.issuetracker.persistence.DatabaseEnvironment;
import com.github.marcellokim.issuetracker.persistence.DriverManagerConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.service.DashboardSummaryService;
import com.github.marcellokim.issuetracker.ui.DemoDashboardPresenter;
import java.io.IOException;
import java.sql.SQLException;

public record ApplicationContext(
        AuthenticationService authenticationService,
        DemoDashboardPresenter dashboardPresenter
) {

    public static ApplicationContext fromEnvironment() throws IOException, SQLException {
        DatabaseEnvironment environment = DatabaseEnvironment.fromSystem();
        var connectionProvider = DriverManagerConnectionProvider.from(environment);
        DatabaseInitializer.initialize(connectionProvider);

        var repositories = new JdbcRepositoryFactory(connectionProvider);
        return new ApplicationContext(
                new AuthenticationService(repositories.users()),
                new DemoDashboardPresenter(new DashboardSummaryService(
                        repositories.projects(),
                        repositories.issues(),
                        repositories.statistics(),
                        repositories.users()
                ))
        );
    }

}
