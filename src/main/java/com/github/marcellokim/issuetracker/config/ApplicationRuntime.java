package com.github.marcellokim.issuetracker.config;

import com.github.marcellokim.issuetracker.service.LoginCheckService;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummaryService;
import java.io.IOException;
import java.sql.SQLException;

public interface ApplicationRuntime {

    boolean hasDatabaseEnvironment();

    RepositoryDemoSummaryService repositoryDemoSummaryService() throws IOException, SQLException;

    LoginCheckService loginCheckService() throws IOException, SQLException;

    DatabaseConnectionSummary databaseConnectionSummary() throws IOException, SQLException;
}
