package com.github.marcellokim.issuetracker.cli;

import com.github.marcellokim.issuetracker.config.ApplicationRuntime;
import com.github.marcellokim.issuetracker.config.DatabaseConnectionSummary;
import com.github.marcellokim.issuetracker.service.LoginCheckResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public final class LoginCheckCommand {

    private final ApplicationRuntime runtime;
    private final CliOutput output;

    public LoginCheckCommand(ApplicationRuntime runtime, CliOutput output) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.output = Objects.requireNonNull(output, "output");
    }

    public void run(String[] args) {
        if (args.length < 3) {
            output.println("Login check skipped.");
            output.println("Usage:");
            output.println("  .\\gradlew.bat run --args=\"--login-check <loginId> <password>\"");
            return;
        }

        if (!runtime.hasDatabaseEnvironment()) {
            new RepositoryDemoCommand(runtime, output).run();
            return;
        }

        String normalizedLoginId = args[1].trim();
        String password = args[2];

        try {
            printConnectionContext(runtime.databaseConnectionSummary());
            printLoginResult(runtime.loginCheckService().checkLogin(normalizedLoginId, password));
        } catch (IOException | SQLException | RuntimeException exception) {
            output.println("Login check failed.");
            output.println("Cause: " + exception.getMessage());
        }
    }

    private void printConnectionContext(DatabaseConnectionSummary summary) {
        output.println("DB URL: " + summary.url());
        output.println("DB user: " + summary.user());
        output.println("Current schema: " + summary.currentSchema());
        output.println("Oracle container: " + summary.containerName());
    }

    private void printLoginResult(LoginCheckResult result) {
        output.println("Login ID: " + result.loginId());
        result.account().ifPresentOrElse(
                value -> {
                    output.println("Account: found");
                    output.println("Role: " + value.role());
                    output.println("Active: " + value.active());
                },
                () -> output.println("Account: missing"));
        output.println("Login result: " + (result.success() ? "SUCCESS" : "FAILURE"));
        output.println("Message: " + result.message());
    }
}
