package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.service.AuthenticationService;
import java.io.IOException;
import java.sql.SQLException;

public record ApplicationContext(
        AuthenticationService authenticationService
) {

    public static ApplicationContext fromEnvironment() throws IOException, SQLException {
        return new ApplicationBootstrap().startUiContext();
    }

}
