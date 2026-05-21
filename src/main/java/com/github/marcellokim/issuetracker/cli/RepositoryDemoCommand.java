package com.github.marcellokim.issuetracker.cli;

import com.github.marcellokim.issuetracker.config.ApplicationRuntime;
import com.github.marcellokim.issuetracker.service.RepositoryDemoSummary;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public final class RepositoryDemoCommand {

    private final ApplicationRuntime runtime;
    private final CliOutput output;
    private final RepositoryDemoSummarySource summarySource;

    public RepositoryDemoCommand(ApplicationRuntime runtime, CliOutput output) {
        this(runtime, output, () -> runtime.repositoryDemoSummaryService().summarizeSeedDemo());
    }

    /*
     * CLI command 출력 포맷 책임만 별도 검증 가능해야 함.
     * 기본 runtime 경로 유지, 테스트에서는 summary 조회 source만 교체.
     */
    RepositoryDemoCommand(
            ApplicationRuntime runtime,
            CliOutput output,
            RepositoryDemoSummarySource summarySource
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.output = Objects.requireNonNull(output, "output");
        this.summarySource = Objects.requireNonNull(summarySource, "summarySource");
    }

    public void run() {
        if (!runtime.hasDatabaseEnvironment()) {
            printDatabaseSetupGuide();
            return;
        }

        try {
            printRepositorySummary(summarySource.summarizeSeedDemo());
        } catch (IOException | SQLException | RuntimeException exception) {
            output.println("Oracle repository demo failed.");
            output.println("Cause: " + exception.getMessage());
            output.println("Check that Oracle XE is running and ITS_DB_URL points to XEPDB1.");
        }
    }

    private void printDatabaseSetupGuide() {
        output.println("Oracle repository demo skipped.");
        output.println("Set database environment variables to print DB/repository status:");
        output.println("  $env:ITS_DB_URL=\"jdbc:oracle:thin:@//localhost:1521/XEPDB1\"");
        output.println("  $env:ITS_DB_USER=\"ITS_USER\"");
        output.println("  $env:ITS_DB_PASSWORD=\"your_password\"");
        output.println("Then run:");
        output.println("  .\\gradlew.bat run --args=\"--cli-demo\"");
    }

    private void printRepositorySummary(RepositoryDemoSummary summary) {
        output.println("Oracle repository demo ready.");
        summary.admin().ifPresentOrElse(
                user -> output.println(
                        "Admin: " + user.loginId() + " / " + user.role() + " / active=" + user.active()),
                () -> output.println("Admin: missing"));

        summary.project().ifPresentOrElse(
                this::printProjectSummary,
                () -> output.println("Project: project1 missing"));
    }

    private void printProjectSummary(RepositoryDemoSummary.ProjectSummary summary) {
        output.println("Project: " + summary.projectName());
        output.println("Members: " + summary.memberCount());
        output.println("Active devs: " + summary.activeDevCount());
        output.println("Active testers: " + summary.activeTesterCount());
        output.println("Issues: " + summary.issueCount());
        output.println("Status counts: " + summary.statusCounts());
        output.println("Priority counts: " + summary.priorityCounts());
        output.println("Dev recommendation candidates: " + summary.devRecommendationCandidateCount());
        output.println("Tester recommendation candidates: " + summary.testerRecommendationCandidateCount());
    }

    @FunctionalInterface
    interface RepositoryDemoSummarySource {

        RepositoryDemoSummary summarizeSeedDemo() throws IOException, SQLException;
    }
}
