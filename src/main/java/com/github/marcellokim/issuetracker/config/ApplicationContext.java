package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.service.AuthenticationService;
import com.github.marcellokim.issuetracker.ui.DemoDashboardPresenter;
import java.io.IOException;
import java.sql.SQLException;

public record ApplicationContext(
        AuthenticationService authenticationService,
        DemoDashboardPresenter dashboardPresenter
) {

    public static ApplicationContext fromEnvironment() throws IOException, SQLException {
        return new ApplicationBootstrap().startUiContext();
    }

}
