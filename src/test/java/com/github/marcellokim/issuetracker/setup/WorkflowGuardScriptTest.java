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

@DisplayName("워크플로우 보호 스크립트")
class WorkflowGuardScriptTest {

    @Test
    @DisplayName("기능 브랜치에서 dev 대상 PR을 허용한다")
    void allowsFeatureBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feature/12-issue-search-ui", "teammate"));
    }

    @Test
    @DisplayName("일반 사용자의 main 대상 PR을 차단한다")
    void blocksNonAdminPullRequestToMain() throws IOException, InterruptedException {
        assertBlocked(pullRequest("main", "feature/12-issue-search-ui", "teammate"));
    }

    @Test
    @DisplayName("관리자 main 대상 PR 예외를 허용한다")
    void allowsAdminBypassPullRequestToMain() throws IOException, InterruptedException {
        assertAllowed(pullRequest("main", "release/main-sync", "marcellokim"));
    }

    @Test
    @DisplayName("관리자 목록에 공백이 있어도 예외 계정을 인식한다")
    void allowsAdminBypassWhenUserListContainsSpaces() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("main", "release/main-sync", "maintainer");
        environment.put("WORKFLOW_BYPASS_USERS", "marcellokim, maintainer");

        assertAllowed(environment);
    }

    @Test
    @DisplayName("dev 브랜치를 head로 쓰는 PR을 차단한다")
    void blocksPullRequestFromDevBranchToDev() throws IOException, InterruptedException {
        assertBlocked(pullRequest("dev", "dev", "teammate"));
    }

    @Test
    @DisplayName("일반 사용자의 보호 자동화 파일 수정을 차단한다")
    void blocksNonAdminChangesToWorkflowGuardFiles() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".github/workflows/workflow-guard.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("pull_request_target 이벤트에서도 기준 브랜치 정책을 적용한다")
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
