package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Workflow guard script")
class WorkflowGuardScriptTest {

    @Test
    @DisplayName("allows feature branches to target dev")
    void allowsFeatBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feat/12-issue-search-ui", "teammate"));
    }

    @Test
    @DisplayName("blocks regular users from targeting main")
    void blocksNonAdminPullRequestToMain() throws IOException, InterruptedException {
        assertBlocked(pullRequest("main", "feat/12-issue-search-ui", "teammate"));
    }

    @Test
    @DisplayName("allows admin exceptions for main")
    void allowsAdminBypassPullRequestToMain() throws IOException, InterruptedException {
        assertAllowed(pullRequest("main", "release/main-sync", "marcellokim"));
    }

    @Test
    @DisplayName("handles spaces in admin allowlist")
    void allowsAdminBypassWhenUserListContainsSpaces() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("main", "release/main-sync", "maintainer");
        environment.put("WORKFLOW_BYPASS_USERS", "marcellokim, maintainer");

        assertAllowed(environment);
    }

    @Test
    @DisplayName("blocks dev as a PR head branch")
    void blocksPullRequestFromDevBranchToDev() throws IOException, InterruptedException {
        assertBlocked(pullRequest("dev", "dev", "teammate"));
    }

    @Test
    @DisplayName("blocks protected automation edits by regular users")
    void blocksNonAdminChangesToWorkflowGuardFiles() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".github/workflows/gradle.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feat/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("allows uppercase slugs in work branches targeting dev")
    void allowsUppercaseSlugInWorkBranch() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feat/12-Issue_Search-UI", "teammate"));
    }

    @Test
    @DisplayName("allows protected automation edits from admin-authored review bot events")
    void allowsAdminAuthoredGuardChangesWhenReviewBotTriggersPullRequestTarget() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "scripts/start-task.sh\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "chore/60-dev-ahead-start-task", "gemini-code-assist[bot]");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "marcellokim");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertAllowed(environment);
    }

    @Test
    @DisplayName("applies base branch rules to pull_request_target events")
    void acceptsPullRequestTargetEventForBaseBranchPolicy() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("dev", "feat/12-issue-search-ui", "teammate");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");

        assertAllowed(environment);
    }

    @Test
    @DisplayName("keeps already-open feature branches compatible")
    void allowsLegacyFeatureBranchForOpenPullRequestCompatibility() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feature/12-issue-search-ui", "teammate"));
    }

    @Test
    @DisplayName("allows Dependabot Gradle dependency updates")
    void allowsDependabotGradleDependencyPullRequestToDev() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-dependabot", ".txt");
        Files.writeString(changedFiles, "build.gradle\n");

        Map<String, String> environment = pullRequest(
                "dev",
                "dependabot/gradle/dev/org.junit-junit-bom-6.1.0",
                "dependabot[bot]");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "dependabot[bot]");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertAllowed(environment);
    }

    @Test
    @DisplayName("allows Dependabot GitHub Actions workflow updates")
    void allowsDependabotGithubActionsPullRequestToDev() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-dependabot", ".txt");
        Files.writeString(changedFiles, ".github/workflows/gradle.yml\n");

        Map<String, String> environment = pullRequest(
                "dev",
                "dependabot/github_actions/dev/actions-checkout-6",
                "dependabot[bot]");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "dependabot[bot]");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertAllowed(environment);
    }

    @Test
    @DisplayName("blocks non-dependency edits from Dependabot branches")
    void blocksDependabotBranchWhenUnexpectedFilesChange() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-dependabot", ".txt");
        Files.writeString(changedFiles, "src/main/java/com/github/marcellokim/issuetracker/Main.java\n");

        Map<String, String> environment = pullRequest(
                "dev",
                "dependabot/gradle/dev/org.junit-junit-bom-6.1.0",
                "dependabot[bot]");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "dependabot[bot]");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    private void assertAllowed(Map<String, String> environment) throws IOException, InterruptedException {
        var result = runGuard(environment);

        assertEquals(0, result.exitCode(), result.output());
    }

    private void assertBlocked(Map<String, String> environment) throws IOException, InterruptedException {
        var result = runGuard(environment);

        assertNotEquals(0, result.exitCode(), result.output());
    }

    private Map<String, String> pullRequest(String baseRef, String headRef, String actor) {
        return new HashMap<>(Map.of(
                "GITHUB_EVENT_NAME", "pull_request",
                "GITHUB_BASE_REF", baseRef,
                "GITHUB_HEAD_REF", headRef,
                "GITHUB_ACTOR", actor,
                "WORKFLOW_BYPASS_USERS", "marcellokim"
        ));
    }

    private GuardResult runGuard(Map<String, String> environment) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder("bash", "scripts/validate-workflow-guard.sh");
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        Map<String, String> processEnvironment = processBuilder.environment();
        processEnvironment.putAll(environment);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new GuardResult(exitCode, output);
    }

    record GuardResult(int exitCode, String output) {
    }
}
