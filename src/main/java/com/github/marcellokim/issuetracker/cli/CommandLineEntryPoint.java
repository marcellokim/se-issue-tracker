package com.github.marcellokim.issuetracker.cli;

import com.github.marcellokim.issuetracker.config.ApplicationRuntime;
import java.util.Arrays;
import java.util.Objects;

public final class CommandLineEntryPoint {

    private final ApplicationRuntime runtime;
    private final CliOutput output;

    public CommandLineEntryPoint(ApplicationRuntime runtime, CliOutput output) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.output = Objects.requireNonNull(output, "output");
    }

    public void run(String[] args) {
        output.println("Issue Tracker application started.");

        if (isLoginCheck(args)) {
            new LoginCheckCommand(runtime, output).run(args);
            return;
        }

        if (isCliDemo(args)) {
            new RepositoryDemoCommand(runtime, output).run();
            return;
        }

        output.println("UI is not available. Use --cli-demo or --login-check.");
    }

    private static boolean isCliDemo(String[] args) {
        return Arrays.asList(args).contains("--cli-demo");
    }

    private static boolean isLoginCheck(String[] args) {
        return args.length > 0 && "--login-check".equals(args[0]);
    }
}
