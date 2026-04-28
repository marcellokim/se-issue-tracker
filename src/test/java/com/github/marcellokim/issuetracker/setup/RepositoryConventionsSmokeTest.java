package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RepositoryConventionsSmokeTest {

    static Stream<String> requiredPaths() {
        return Stream.of(
                "README.md",
                ".github/workflows/gradle.yml",
                ".github/workflows/pr-labeler.yml",
                ".github/workflows/add-to-project.yml",
                "config/github/labels.json",
                "config/github/milestones.json",
                "docs/team-setup-manual.md",
                "docs/automation-playbook.md",
                "docs/templates/submission-readme.txt.template",
                "scripts/bootstrap-dev.sh",
                "scripts/start-task.sh",
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
}
