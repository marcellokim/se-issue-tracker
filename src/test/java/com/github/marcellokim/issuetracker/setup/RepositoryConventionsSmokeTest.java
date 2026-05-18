package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("저장소 자동화 규칙 점검")
class RepositoryConventionsSmokeTest {

    static Stream<String> requiredPaths() {
        return Stream.of(
                "README.md",
                "SE_Term_Project_2026-1.pdf",
                ".github/workflows/gradle.yml",
                ".github/workflows/workflow-guard.yml",
                ".github/workflows/pr-labeler.yml",
                ".github/workflows/add-to-project.yml",
                ".github/workflows/pr-metadata.yml",
                ".github/workflows/codeql.yml",
                ".github/workflows/project-maintenance.yml",
                ".coderabbit.yaml",
                ".pr_agent.toml",
                ".gemini/config.yaml",
                ".gemini/styleguide.md",
                ".github/copilot-instructions.md",
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
    @DisplayName("필수 자동화 파일이 모두 존재한다")
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
                new ScriptExpectation(".github/workflows/add-to-project.yml", "name: 프로젝트 항목 자동 추가"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "name: PR 메타데이터 정렬"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "pull_request_target"),
                new ScriptExpectation(".github/workflows/pr-metadata.yml", "sync-pr-metadata"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "PR linked issue, assignee, milestone, project metadata"),
                new ScriptExpectation("scripts/lib/project_maintenance.py", "Closes #"),
                new ScriptExpectation("scripts/lib/bootstrap_github.py", "PR 메타데이터 정렬"),
                new ScriptExpectation(".github/workflows/pr-labeler.yml", "name: PR 라벨 적용"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: 보안 코드 분석"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: 보안 코드 분석 (${{ matrix.label }})"),
                new ScriptExpectation(".github/workflows/codeql.yml", "name: Java/Kotlin 분석 대상 빌드"),
                new ScriptExpectation("build.gradle", "id 'application'"),
                new ScriptExpectation("build.gradle", "id 'org.openjfx.javafxplugin' version '0.1.0'"),
                new ScriptExpectation("build.gradle", "version = '21.0.6'"),
                new ScriptExpectation("build.gradle", "modules = ['javafx.controls', 'javafx.fxml']"),
                new ScriptExpectation("build.gradle", "mainClass = 'com.github.marcellokim.issuetracker.Main'"),
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
                new ScriptExpectation(".coderabbit.yaml", "language: \"ko-KR\""),
                new ScriptExpectation(".coderabbit.yaml", "tone_instructions"),
                new ScriptExpectation(".coderabbit.yaml", "auto_title_instructions"),
                new ScriptExpectation(".coderabbit.yaml", "poem: false"),
                new ScriptExpectation(".coderabbit.yaml", """
                finishing_touches:
                    unit_tests:
                      enabled: false
                    docstrings:
                      enabled: false
                """),
                new ScriptExpectation(".coderabbit.yaml", "pre_merge_checks"),
                new ScriptExpectation(".coderabbit.yaml", "mode: \"off\""),
                new ScriptExpectation(".coderabbit.yaml", "auto_reply: false"),
                new ScriptExpectation(".coderabbit.yaml", ".github/copilot-instructions.md"),
                new ScriptExpectation(".github/dependabot.yml", "target-branch: \"dev\""),
                new ScriptExpectation(".github/dependabot.yml", "version-update:semver-major"),
                new ScriptExpectation(".pr_agent.toml", "Qodo/PR-Agent is intentionally disabled"),
                new ScriptExpectation(".pr_agent.toml", "use_repo_settings_file = true"),
                new ScriptExpectation(".pr_agent.toml", "pr_commands = []"),
                new ScriptExpectation(".pr_agent.toml", "handle_push_trigger = false"),
                new ScriptExpectation(".pr_agent.toml", "push_commands = []"),
                new ScriptExpectation(".pr_agent.toml", "enable_auto_checks_feedback = false"),
                new ScriptExpectation(".pr_agent.toml", "persistent_comment = false"),
                new ScriptExpectation(".pr_agent.toml", "final_update_message = false"),
                new ScriptExpectation(".pr_agent.toml", "[checks]"),
                new ScriptExpectation(".gemini/config.yaml", "comment_severity_threshold: MEDIUM"),
                new ScriptExpectation(".gemini/config.yaml", "max_review_comments: 10"),
                new ScriptExpectation(".gemini/config.yaml", "include_drafts: false"),
                new ScriptExpectation(".gemini/styleguide.md", "Write review comments in Korean"),
                new ScriptExpectation(".gemini/styleguide.md", "Normal pull requests target `dev`"),
                new ScriptExpectation(".github/copilot-instructions.md", "자동 리뷰, 요약, 제안, 체크 실패 분석, 채팅 응답은 가능한 한 한국어")
        );
    }

    @ParameterizedTest
    @DisplayName("자동화 스크립트와 워크플로우가 한국어 안내 문구를 포함한다")
    @MethodSource("requiredScriptGuardrails")
    void workflowScriptsExplainGuardrails(ScriptExpectation expectation) throws IOException {
        var text = Files.readString(Path.of(expectation.relativePath()));

        assertTrue(
                text.contains(expectation.expectedText()),
                () -> expectation.relativePath() + "에 안내 문구가 없습니다: " + expectation.expectedText()
        );
    }

    @Test
    @DisplayName("표준 PR 스크립트는 PR 생성 전 이슈 상태와 프로젝트 보드를 먼저 정렬한다")
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
    @DisplayName("표준 PR 스크립트는 PR 생성 실패 시 사전 변경한 이슈 상태를 복구한다")
    void openPrRestoresIssueStatusWhenPullRequestCreationFails() throws IOException {
        var text = Files.readString(Path.of("scripts/open-pr.sh"));

        var previousStatusCapture = text.indexOf("previous_status_labels=\"$(current_status_labels)\"");
        var createPullRequestFailureGuard = text.indexOf("if ! pr_url=\"$(gh pr create --base dev");
        var restoreIssueStatus = text.indexOf("restore_issue_status_labels \"$previous_status_labels\"", createPullRequestFailureGuard);
        var restoreProjectStatus = text.indexOf("sync_project_board", restoreIssueStatus);

        assertTrue(previousStatusCapture >= 0, "PR 생성 전 기존 이슈 상태 라벨을 저장해야 합니다.");
        assertTrue(createPullRequestFailureGuard >= 0, "gh pr create 실패를 명시적으로 처리해야 합니다.");
        assertTrue(restoreIssueStatus > createPullRequestFailureGuard, "PR 생성 실패 시 이슈 상태 라벨을 복구해야 합니다.");
        assertTrue(restoreProjectStatus > restoreIssueStatus, "이슈 상태 복구 후 프로젝트 상태를 다시 정렬해야 합니다.");
    }

    record ScriptExpectation(String relativePath, String expectedText) {
    }
}
