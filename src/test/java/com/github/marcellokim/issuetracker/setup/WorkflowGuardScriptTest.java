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
        Files.writeString(changedFiles, ".github/workflows/gradle.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("작업 브랜치 slug에 대문자가 있어도 dev 대상 PR을 허용한다")
    void allowsUppercaseSlugInWorkBranch() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feature/12-Issue_Search-UI", "teammate"));
    }

    @Test
    @DisplayName("리뷰 봇 이벤트라도 PR 작성자가 관리자이면 보호 자동화 파일 수정을 허용한다")
    void allowsAdminAuthoredGuardChangesWhenReviewBotTriggersPullRequestTarget() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "scripts/start-task.sh\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "chore/60-dev-ahead-start-task", "coderabbitai[bot]");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "marcellokim");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertAllowed(environment);
    }

    @Test
    @DisplayName("pull_request_target 이벤트에서도 기준 브랜치 정책을 적용한다")
    void acceptsPullRequestTargetEventForBaseBranchPolicy() throws IOException, InterruptedException {
        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");

        assertAllowed(environment);
    }

    @Test
    @DisplayName("docs 타입 브랜치에서 dev 대상 PR을 허용한다")
    void allowsDocsBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "docs/18-update-readme", "teammate"));
    }

    @Test
    @DisplayName("test 타입 브랜치에서 dev 대상 PR을 허용한다")
    void allowsTestBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "test/21-add-unit-tests", "teammate"));
    }

    @Test
    @DisplayName("chore 타입 브랜치에서 dev 대상 PR을 허용한다")
    void allowsChoresBranchPullRequestToDev() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "chore/30-update-deps", "teammate"));
    }

    @Test
    @DisplayName("PR_AUTHOR가 비관리자이면 actor가 관리자여도 보호 파일 수정을 차단한다")
    void blocksPrAuthorNonAdminEvenWhenActorIsAdmin() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "scripts/start-task.sh\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "marcellokim");
        environment.put("GITHUB_EVENT_NAME", "pull_request_target");
        environment.put("PR_AUTHOR", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName(".githooks 내 파일 수정을 일반 사용자에게 차단한다")
    void blocksNonAdminChangesToGithooksFiles() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".githooks/pre-commit\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName(".github/dependabot.yml 수정을 일반 사용자에게 차단한다")
    void blocksNonAdminChangesToDependabotYml() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".github/dependabot.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName(".github/labeler.yml 수정을 일반 사용자에게 차단한다")
    void blocksNonAdminChangesToLabelerYml() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, ".github/labeler.yml\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("scripts/validate-public-attribution.sh 수정을 일반 사용자에게 차단한다")
    void blocksNonAdminChangesToPublicAttributionScript() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "scripts/validate-public-attribution.sh\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("scripts/lib/git-refs.sh 수정을 일반 사용자에게 차단한다")
    void blocksNonAdminChangesToGitRefsScript() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "scripts/lib/git-refs.sh\nREADME.md\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertBlocked(environment);
    }

    @Test
    @DisplayName("비보호 파일만 수정하는 PR은 일반 사용자에게 허용한다")
    void allowsNonAdminChangesToUnguardedFiles() throws IOException, InterruptedException {
        Path changedFiles = Files.createTempFile("workflow-guard-changes", ".txt");
        Files.writeString(changedFiles, "README.md\nsrc/main/java/Main.java\n");

        Map<String, String> environment = pullRequest("dev", "feature/12-issue-search-ui", "teammate");
        environment.put("CHANGED_FILES_PATH", changedFiles.toString());

        assertAllowed(environment);
    }

    @Test
    @DisplayName("slug에 점(.)이 포함된 브랜치에서 dev 대상 PR을 허용한다")
    void allowsDotInSlugForWorkBranch() throws IOException, InterruptedException {
        assertAllowed(pullRequest("dev", "feature/12-issue.search.ui", "teammate"));
    }

    private void assertAllowed(Map<String, String> environment) throws IOException, InterruptedException {
        var result = runGuard(environment);


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
