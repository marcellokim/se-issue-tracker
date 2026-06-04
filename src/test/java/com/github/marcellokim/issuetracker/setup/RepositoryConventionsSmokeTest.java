package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Repository automation conventions")
class RepositoryConventionsSmokeTest {

    static Stream<String> requiredPaths() {
        return Stream.of(
                "README.md",
                ".github/workflows/gradle.yml",
                ".github/workflows/workflow-guard.yml",
                ".github/workflows/pr-labeler.yml",
                ".github/workflows/add-to-project.yml",
                ".github/workflows/pr-metadata.yml",
                ".github/workflows/codeql.yml",
                ".github/workflows/project-maintenance.yml",
                ".sonarcloud.properties",
                ".github/dependabot.yml",
                "config/github/labels.json",
                "config/github/milestones.json",
                "docs/team-setup-manual.md",
                "docs/automation-playbook.md",
                "docs/textbook-concept-baseline.md",
                "docs/templates/submission-readme.txt.template",
                ".githooks/commit-msg",
                "scripts/bootstrap-dev.sh",
                "scripts/audit-project.sh",
                "scripts/lib/git-refs.sh",
                "scripts/lib/python.sh",
                "scripts/start-task.sh",
                "scripts/open-pr.sh",
                "scripts/validate-workflow-guard.sh",
                "scripts/validate-public-attribution.sh",
                "scripts/sync-project-board.sh",
                "scripts/bootstrap.sh",
                "scripts/package-submission.sh",
                "src/main/java/com/github/marcellokim/issuetracker/Main.java"
        );
    }

    @ParameterizedTest
    @DisplayName("required automation files are present")
    @MethodSource("requiredPaths")
    void requiredAutomationFilesExist(String relativePath) {
        assertTrue(
                Files.exists(Path.of(relativePath)),
                () -> "필수 저장소 자동화 파일이 없습니다: " + relativePath
        );
    }

    static Stream<ScriptExpectation> requiredScriptGuardrails() {
        return Stream.of(
                new ScriptExpectation("scripts/start-task.sh", "작업트리에 커밋되지 않은 변경이 있습니다"),
                new ScriptExpectation("scripts/start-task.sh", "--type feat|fix|docs|test|ci|chore|refactor"),
                new ScriptExpectation("scripts/start-task.sh", "ensure_origin_fetch_ref dev"),
                new ScriptExpectation("scripts/lib/git-refs.sh", "refs/heads/${branch}:refs/remotes/origin/${branch}"),
                new ScriptExpectation("scripts/lib/python.sh", "python3 python py"),
                new ScriptExpectation("scripts/bootstrap-dev.sh", "scripts/lib/*.sh"),
                new ScriptExpectation("scripts/start-task.sh", "git merge-base --is-ancestor origin/main origin/dev"),
                new ScriptExpectation("scripts/start-task.sh", "origin/dev가 origin/main을 포함하지 않습니다"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "\"merge-base\", \"--is-ancestor\", \"origin/main\", \"origin/dev\""),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "origin/main이 origin/dev의 조상입니다"),
                new ScriptExpectation("scripts/start-task.sh", "./scripts/open-pr.sh"),
                new ScriptExpectation("scripts/open-pr.sh", "PR을 올릴 수 있는 작업 브랜치가 아닙니다"),
                new ScriptExpectation("scripts/open-pr.sh", "[0-9]+-[A-Za-z0-9._-]+"),
                new ScriptExpectation("scripts/open-pr.sh", "refs/heads/dev:refs/remotes/origin/dev"),
                new ScriptExpectation("scripts/open-pr.sh", "gh auth login"),
                new ScriptExpectation("scripts/open-pr.sh", "gh pr create"),
                new ScriptExpectation("scripts/open-pr.sh", "sync-pr-metadata"),
                new ScriptExpectation("scripts/open-pr.sh", "상태 라벨을 review로 이동"),
                new ScriptExpectation("scripts/open-pr.sh", "sync-project-board.sh"),
                new ScriptExpectation(".github/workflows/gradle.yml", "test/**"),
                new ScriptExpectation(".github/workflows/gradle.yml", "name: 빌드와 테스트"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "name: 워크플로우 보호"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "name: 워크플로우 정책 검사"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "WORKFLOW_BYPASS_USERS"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "PR_AUTHOR"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "gh pr diff"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "GITHUB_SHA"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "GITHUB_REPOSITORY"),
                new ScriptExpectation(".github/workflows/workflow-guard.yml", "github.event.pull_request.number || github.ref"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", "일반 작업 PR은 main이 아니라 dev"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", "feat|fix|docs|test|ci|chore|refactor"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", ".github/workflows/*"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", "repos/$repository/commits/$sha/pulls"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", "PR merge push 허용"),
                new ScriptExpectation("scripts/validate-workflow-guard.sh", "관리자 외에는 워크플로우/브랜치 보호 우회 지점을 수정할 수 없습니다"),
                new ScriptExpectation("scripts/lib/bootstrap_github.py", "WORKFLOW_BYPASS_USERS"),
                new ScriptExpectation("scripts/lib/bootstrap_github.py", "빌드와 테스트"),
                new ScriptExpectation("scripts/lib/bootstrap_github.py", "워크플로우 정책 검사"),
                new ScriptExpectation("scripts/lib/bootstrap_github.py", "branches/{branch}/protection"),
                new ScriptExpectation("scripts/open-pr.sh", "## 요약"),
                new ScriptExpectation("scripts/open-pr.sh", "## 검증"),
                new ScriptExpectation("scripts/open-pr.sh", "## 관련 이슈"),
                new ScriptExpectation(".githooks/pre-commit", "main/dev 브랜치 직접 커밋은 금지"),
                new ScriptExpectation(".githooks/pre-commit", "validate-public-attribution.sh --staged"),
                new ScriptExpectation(".githooks/commit-msg", "validate-public-attribution.sh --commit-msg"),
                new ScriptExpectation(".githooks/pre-push", "main/dev 직접 push는 금지"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "공개 이력 표기 정책 확인"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "synchronize"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "cancel-in-progress: true"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "github.event_name == 'pull_request'"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "min_graphql_remaining=250"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "GitHub rate-limit 조회 실패"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "name: 프로젝트 상태 정렬"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "자동화 정합성 점검 전 GraphQL rate-limit 확인"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "자동화 정합성 점검을 건너뜁니다"),
                new ScriptExpectation(".github/workflows/add-to-project.yml", "name: 프로젝트 항목 자동 추가"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "name: PR 메타데이터 정렬"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "pull_request_target"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "sync-pr-metadata"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "PR 메타데이터 자동 보정을 건너뜁니다"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "PR linked issue, assignee, milestone, project metadata"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "Closes #"),
                new ScriptExpectation(".github/workflows/pr-labeler.yml", "name: PR 라벨 적용"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: 보안 코드 분석"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: 보안 코드 분석 (${{ matrix.label }})"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: Java/Kotlin 분석 대상 빌드"),
                new ScriptExpectation("build.gradle", "id 'application'"),
                new ScriptExpectation("build.gradle", "id 'org.openjfx.javafxplugin' version '0.1.0'"),
                new ScriptExpectation("build.gradle", "version = '21.0.6'"),
                new ScriptExpectation("build.gradle", "modules = ['javafx.controls', 'javafx.fxml', 'javafx.graphics']"),
                new ScriptExpectation("build.gradle", "mainClass = 'com.github.marcellokim.issuetracker.Main'"),
                new ScriptExpectation("build.gradle", "args '--swing'"),
                new ScriptExpectation("build.gradle", "providers.gradleProperty('pythonExecutable')"),
                new ScriptExpectation("build.gradle", "? 'python' : 'python3'"),
                new ScriptExpectation("build.gradle", "commandLine pythonExecutable.get(), 'scripts/lib/project_maintenance.py'"),
                new ScriptExpectation("README.md", "./gradlew run"),
                new ScriptExpectation("README.md", "./gradlew check -PpythonExecutable=py"),
                new ScriptExpectation("README.md", "./scripts/start-task.sh --type docs"),
                new ScriptExpectation("README.md", "docs/textbook-concept-baseline.md"),
                new ScriptExpectation("docs/textbook-concept-baseline.md", "강의 PDF 원문은"),
                new ScriptExpectation("docs/textbook-concept-baseline.md", "도메인 모델은 개념 모델"),
                new ScriptExpectation("docs/textbook-concept-baseline.md", "SSD는 선택한 한 유스케이스 시나리오마다 작성"),
                new ScriptExpectation("docs/textbook-concept-baseline.md", "Operation Contract는 system operation 하나의 효과"),
                new ScriptExpectation(".githooks/pre-commit", "feat/<issue>-<slug>"),
                new ScriptExpectation(".githooks/pre-commit", "[0-9]+-[A-Za-z0-9._-]+"),
                new ScriptExpectation("scripts/audit-project.sh", "project_maintenance.py audit"),
                new ScriptExpectation("scripts/sync-project-board.sh", "project_maintenance.py sync-project"),
                new ScriptExpectation("scripts/package-submission.sh", "docs/qa/artifacts/"),
                new ScriptExpectation("scripts/package-submission.sh", ".idea/"),
                new ScriptExpectation("scripts/package-submission.sh", ".vscode/"),
                new ScriptExpectation("scripts/package-submission.sh", ".DS_Store"),
                new ScriptExpectation("scripts/package-submission.sh", "__pycache__/"),
                new ScriptExpectation("scripts/package-submission.sh", "docs/textbook/"),
                new ScriptExpectation("scripts/package-submission.sh", "SE_Term_Project_2026-1.pdf"),
                new ScriptExpectation("scripts/package-submission.sh", "*:Zone.Identifier"),
                new ScriptExpectation("scripts/package-submission.sh", "AGENTS.md"),
                new ScriptExpectation("scripts/package-submission.sh", "MEMORY.md"),
                new ScriptExpectation("scripts/package-submission.sh", "memory.md"),
                new ScriptExpectation(".gitignore", "docs/qa/artifacts/"),
                new ScriptExpectation(".gitignore", "docs/textbook/"),
                new ScriptExpectation(".gitignore", "SE_Term_Project_2026-1.pdf"),
                new ScriptExpectation(".gitignore", "AGENTS.md"),
                new ScriptExpectation(".gitignore", "MEMORY.md"),
                new ScriptExpectation(".gitignore", "memory.md"),
                new ScriptExpectation(".github/dependabot.yml", "target-branch: \"dev\""),
                new ScriptExpectation(".github/dependabot.yml", "version-update:semver-major"),
                new ScriptExpectation("scripts/package-submission.sh", ".gemini/"),
                new ScriptExpectation("scripts/package-submission.sh", ".github/copilot-instructions.md"),
                new ScriptExpectation("scripts/package-submission.sh", ".pr_agent.toml")
        );
    }

    @ParameterizedTest
    @DisplayName("automation scripts and workflows keep Korean guidance text")
    @MethodSource("requiredScriptGuardrails")
    void workflowScriptsExplainGuardrails(ScriptExpectation expectation) throws IOException {
        var text = Files.readString(Path.of(expectation.relativePath()));

        assertTrue(
                text.contains(expectation.expectedText()),
                () -> expectation.relativePath() + "에 안내 문구가 없습니다: " + expectation.expectedText()
        );
    }

    @Test
    @DisplayName("open-pr script syncs issue status and project board before creating a PR")
    void openPrPreparesIssueStatusBeforeCreatingPullRequest() throws IOException {
        var text = Files.readString(Path.of("scripts/open-pr.sh"));

        var newPullRequestBody = text.indexOf("body=\"## 요약");
        var preCreateIssueStatus = text.indexOf("\nmark_issue_review\n", newPullRequestBody);
        var preCreateProjectSync = text.indexOf("\nsync_project_board\n", preCreateIssueStatus);
        var createPullRequest = text.indexOf("gh pr create --base dev", newPullRequestBody);

        assertTrue(newPullRequestBody >= 0, "새 PR 생성 본문 정의가 없습니다.");
        assertTrue(preCreateIssueStatus >= 0, "PR 생성 전 이슈 상태 라벨 정렬 단계가 없습니다.");
        assertTrue(preCreateProjectSync >= 0, "PR 생성 전 프로젝트 상태 정렬 단계가 없습니다.");
        assertTrue(createPullRequest >= 0, "PR 생성 단계가 없습니다.");
        assertTrue(
                preCreateIssueStatus < createPullRequest,
                "이슈 상태 라벨 정렬은 pull_request 체크가 시작되기 전에 끝나야 합니다."
        );
        assertTrue(
                preCreateProjectSync < createPullRequest,
                "프로젝트 상태 정렬은 pull_request 체크가 시작되기 전에 끝나야 합니다."
        );
        assertTrue(
                preCreateIssueStatus < preCreateProjectSync,
                "이슈 상태 라벨 정렬 후 프로젝트 상태 정렬이 수행되어야 합니다."
        );
    }

    @Test
    @DisplayName("open-pr script restores the saved issue status when PR creation fails")
    void openPrRestoresIssueStatusWhenPullRequestCreationFails() throws IOException {
        var text = Files.readString(Path.of("scripts/open-pr.sh"));

        var previousStatusCapture = text.indexOf("previous_status_labels=\"$(current_status_labels)\"");
        var rollbackTrap = text.indexOf("trap rollback_precreate_status_on_exit EXIT", previousStatusCapture);
        var preCreateIssueStatus = text.indexOf("\nmark_issue_review\n", rollbackTrap);
        var createPullRequest = text.indexOf("gh pr create --base dev", preCreateIssueStatus);
        var clearRollbackTrap = text.indexOf("trap - EXIT", createPullRequest);
        var restoreIssueStatus = text.indexOf("restore_issue_status_labels \"$previous_status_labels\"");
        var restoreProjectStatus = text.indexOf("sync_project_board", restoreIssueStatus);

        assertTrue(previousStatusCapture >= 0, "PR 생성 전 기존 이슈 상태 라벨을 저장해야 합니다.");
        assertTrue(rollbackTrap > previousStatusCapture, "PR 생성 전 상태 변경 전에 rollback trap을 등록해야 합니다.");
        assertTrue(preCreateIssueStatus > rollbackTrap, "rollback trap 등록 후 이슈 상태 라벨을 변경해야 합니다.");
        assertTrue(createPullRequest > preCreateIssueStatus, "이슈 상태 라벨 변경 후 gh pr create를 호출해야 합니다.");
        assertTrue(clearRollbackTrap > createPullRequest, "PR 생성 성공 후 rollback trap을 해제해야 합니다.");
        assertTrue(restoreIssueStatus >= 0, "PR 생성 전 단계 실패 시 이슈 상태 라벨을 복구해야 합니다.");
        assertTrue(restoreProjectStatus > restoreIssueStatus, "이슈 상태 복구 후 프로젝트 상태를 다시 정렬해야 합니다.");
    }

    @Test
    @DisplayName("GraphQL-dependent automation is not required for branch protection")
    void graphqlDependentAutomationIsNotARequiredStatusCheck() throws IOException {
        var text = Files.readString(Path.of("scripts/lib/bootstrap_github.py"));

        assertTrue(text.contains("REQUIRED_STATUS_CHECKS = (\"빌드와 테스트\", \"워크플로우 정책 검사\")"));
        assertFalse(
                text.contains("\"PR 메타데이터 정렬\""),
                "PR 메타데이터 정렬은 GraphQL rate-limit 영향을 받으므로 필수 체크가 아니어야 합니다."
        );
        assertFalse(
                text.contains("\"프로젝트 상태 정렬\""),
                "프로젝트 상태 정렬은 GraphQL rate-limit 영향을 받으므로 필수 체크가 아니어야 합니다."
        );
    }

    @Test
    @DisplayName("local-only audit does not require repository detection in submission zips")
    void localOnlyAuditSkipsRepositoryDetection() throws IOException {
        var text = Files.readString(Path.of("scripts/lib/project_maintenance.py"));

        var auditStart = text.indexOf("def audit(args: argparse.Namespace) -> int:");
        var localOnlyBranch = text.indexOf("if args.local_only:", auditStart);
        var repoDetection = text.indexOf("repo = args.repo or detect_repo()", auditStart);

        assertTrue(auditStart >= 0, "audit 함수가 없습니다.");
        assertTrue(localOnlyBranch > auditStart, "local-only audit 분기점이 없습니다.");
        assertTrue(repoDetection > auditStart, "repo 감지 코드가 없습니다.");
        assertTrue(
                localOnlyBranch < repoDetection,
                "local-only audit는 .git 없는 제출 zip 내부에서도 동작하도록 repo 감지보다 먼저 처리되어야 합니다."
        );
    }

    @Test
    @DisplayName("SonarCloud auto analysis shares Gradle coverage exclusions")
    void sonarCloudAutomaticAnalysisUsesTheSameCoverageExclusions() throws IOException {
        var gradle = Files.readString(Path.of("build.gradle"));
        var sonarCloud = Files.readString(Path.of(".sonarcloud.properties"));
        var gradleExclusions = gradleCoverageExclusions(gradle);
        var sonarExclusions = sonarPropertyValues(sonarCloud, "sonar.coverage.exclusions");
        var automaticAnalysisExclusions = sonarPropertyValues(sonarCloud, "sonar.exclusions");

        assertFalse(gradleExclusions.isEmpty(), "Gradle coverage 제외 정책을 찾을 수 없습니다.");
        assertFalse(sonarExclusions.isEmpty(), ".sonarcloud.properties에도 coverage 제외 정책이 있어야 합니다.");
        assertEquals(gradleExclusions, sonarExclusions, "SonarCloud 자동 분석 coverage 제외 정책이 Gradle과 달라졌습니다.");
        assertFalse(
                gradleExclusions.stream().anyMatch(value -> value.startsWith("src/main/java/")),
                "Coverage 제외 정책은 SonarCloud 파일 키와 안정적으로 맞도록 source-root 고정 경로 대신 glob 패턴을 사용해야 합니다."
        );
        assertTrue(
                automaticAnalysisExclusions.containsAll(sonarExclusions),
                ".sonarcloud.properties의 자동 분석 source 제외 정책은 coverage 제외 정책을 포함해야 합니다."
        );
    }

    @Test
    @DisplayName("issue list and search use the standard API path")
    void issueListAndSearchUseStandardPathWithoutRelatedDuplicateApi() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            var offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(
                            readUnchecked(path),
                            "searchRelatedProjectIssues",
                            "viewRelatedProjectIssues"))
                    .toList();

            assertTrue(
                    offenders.isEmpty(),
                    () -> "삭제 대상 related 이슈 API가 남아 있습니다: " + offenders
            );
        }
    }

    @Test
    @DisplayName("submission packaging creates zip files with Python standard library")
    void packageSubmissionUsesPythonZipArchive() throws IOException {
        var text = Files.readString(Path.of("scripts/package-submission.sh"));

        assertFalse(text.contains("require_tool zip"), "zip CLI가 없어도 제출 패키지를 만들 수 있어야 합니다.");
        assertTrue(text.contains("import zipfile"), "Python 표준 zipfile 모듈로 archive를 생성해야 합니다.");
        assertTrue(text.contains("zipfile.ZIP_DEFLATED"), "제출 zip은 압축된 zip archive여야 합니다.");
    }

    private static boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static Set<String> gradleCoverageExclusions(String gradleText) {
        var matcher = Pattern.compile("def coverageExcludedSources = \\[(?<body>.*?)\\]\\.join\\(','\\)",
                Pattern.DOTALL).matcher(gradleText);
        if (!matcher.find()) {
            return Set.of();
        }
        return quotedValues(matcher.group("body"));
    }

    private static Set<String> quotedValues(String text) {
        var matcher = Pattern.compile("'([^']+)'").matcher(text);
        var values = new LinkedHashSet<String>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static Set<String> sonarPropertyValues(String propertiesText, String key) {
        return propertiesText.lines()
                .filter(line -> line.startsWith(key + "="))
                .findFirst()
                .map(line -> line.substring(key.length() + 1))
                .map(RepositoryConventionsSmokeTest::commaSeparatedValues)
                .orElseGet(Set::of);
    }

    private static Set<String> commaSeparatedValues(String text) {
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    record ScriptExpectation(String relativePath, String expectedText) {
    }
}
