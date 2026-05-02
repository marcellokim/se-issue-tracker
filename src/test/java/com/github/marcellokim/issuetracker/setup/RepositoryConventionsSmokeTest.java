package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RepositoryConventionsSmokeTest {

    static Stream<String> requiredPaths() {
        return Stream.of(
                "README.md",
                "SE_Term_Project_2026-1.pdf",
                ".github/workflows/gradle.yml",
                ".github/workflows/pr-labeler.yml",
                ".github/workflows/add-to-project.yml",
                ".github/workflows/project-maintenance.yml",
                "config/github/labels.json",
                "config/github/milestones.json",
                "docs/team-setup-manual.md",
                "docs/automation-playbook.md",
                "docs/templates/submission-readme.txt.template",
                "scripts/bootstrap-dev.sh",
                "scripts/audit-project.sh",
                "scripts/start-task.sh",
                "scripts/open-pr.sh",
                "scripts/sync-project-board.sh",
                "scripts/bootstrap.sh",
                "scripts/package-submission.sh"
        );
    }

    @ParameterizedTest
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
                new ScriptExpectation("scripts/start-task.sh", "origin/main과 origin/dev의 파일 내용이 다릅니다"),
                new ScriptExpectation("scripts/start-task.sh", "./scripts/open-pr.sh"),
                new ScriptExpectation("scripts/open-pr.sh", "PR을 올릴 수 있는 작업 브랜치가 아닙니다"),
                new ScriptExpectation("scripts/open-pr.sh", "gh auth login"),
                new ScriptExpectation("scripts/open-pr.sh", "gh pr create"),
                new ScriptExpectation("scripts/open-pr.sh", "상태 라벨을 review로 이동"),
                new ScriptExpectation("scripts/open-pr.sh", "sync-project-board.sh"),
                new ScriptExpectation(".github/workflows/gradle.yml", "test/**"),
                new ScriptExpectation(".github/workflows/project-maintenance.yml", "synchronize"),
                new ScriptExpectation(".githooks/pre-commit", "docs/<issue>-<slug>"),
                new ScriptExpectation("scripts/audit-project.sh", "project_maintenance.py audit"),
                new ScriptExpectation("scripts/sync-project-board.sh", "project_maintenance.py sync-project")
        );
    }

    @ParameterizedTest
    @MethodSource("requiredScriptGuardrails")
    void workflowScriptsExplainGuardrails(ScriptExpectation expectation) throws IOException {
        var text = Files.readString(Path.of(expectation.relativePath()));

        assertTrue(
                text.contains(expectation.expectedText()),
                () -> expectation.relativePath() + "에 안내 문구가 없습니다: " + expectation.expectedText()
        );
    }

    record ScriptExpectation(String relativePath, String expectedText) {
    }
}
