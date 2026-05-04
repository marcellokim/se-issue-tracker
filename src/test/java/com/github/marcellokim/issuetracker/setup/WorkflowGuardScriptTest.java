package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowGuardScriptTest {

    @Test
    void allowsFeatureBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feature/12-issue-search-ui", "teammate"));
    }

    @Test
    void blocksNonAdminPullRequestToMain() throws IOException, InterruptedException {
        assertBlocked(pullRequest("main", "feature/12-issue-search-ui", "teammate"));
    }

    @Test
    void allowsAdminBypassPullRequestToMain() throws IOException, InterruptedException {
        assertAllowed(pullRequest("main", "release/main-sync", "marcellokim"));
    }

    @Test
    void allowsAdminBypassWhenUserListContainsSpaces() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("main", "release/main-sync", "maintainer");
        environment.put("WORKFLOW_BYPASS_USERS", "marcellokim, maintainer");

        assertAllowed(environment);
    }

    @Test
    void blocksPullRequestFromDevBranchToDev() throws IOException, InterruptedException {
        assertBlocked(pullRequest("dev", "dev", "teammate"));
    }

    @Test
    void blocksNonAdminChangesToWorkflowGuardFiles() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".github/workflows/workflow-guard.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    void acceptsPullRequestTargetEventForBaseBranchPolicy() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");

        assertAllowed(environment);
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
