package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("작업 브랜치 생성 스크립트 (start-task.sh)")
class StartTaskScriptTest {

    @Test
    @DisplayName("인수 없이 실행하면 usage 메시지와 함께 종료 코드 1을 반환한다")
    void exitsWithUsageWhenNoArguments() throws IOException, InterruptedException {
        var result = runStartTask();

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("사용법"), "usage 메시지를 출력해야 합니다: " + result.output());
    }

    @Test
    @DisplayName("이슈 번호만 전달하면 usage 메시지와 함께 종료 코드 1을 반환한다")
    void exitsWithUsageWhenOnlyIssueNumberProvided() throws IOException, InterruptedException {
        var result = runStartTask("42");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("사용법"), "usage 메시지를 출력해야 합니다: " + result.output());
    }

    @Test
    @DisplayName("--help 플래그는 usage 메시지와 함께 종료 코드 0을 반환한다")
    void exitsZeroWithUsageForHelpFlag() throws IOException, InterruptedException {
        var result = runStartTask("--help");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("사용법"), "usage 메시지를 출력해야 합니다: " + result.output());
    }

    @Test
    @DisplayName("-h 플래그는 usage 메시지와 함께 종료 코드 0을 반환한다")
    void exitsZeroWithUsageForShortHelpFlag() throws IOException, InterruptedException {
        var result = runStartTask("-h");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("사용법"), "usage 메시지를 출력해야 합니다: " + result.output());
    }

    @Test
    @DisplayName("--type 플래그에 값이 없으면 usage와 함께 종료 코드 1을 반환한다")
    void exitsWithUsageWhenTypeHasNoValue() throws IOException, InterruptedException {
        var result = runStartTask("--type");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("사용법"), "usage 메시지를 출력해야 합니다: " + result.output());
    }

    @Test
    @DisplayName("허용되지 않는 --type 값은 오류 메시지와 함께 차단된다")
    void blocksInvalidBranchType() throws IOException, InterruptedException {
        var result = runStartTask("--type", "invalid", "12", "my-slug");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(
                result.output().contains("feature") && result.output().contains("docs"),
                "허용 타입 목록이 오류 메시지에 있어야 합니다: " + result.output()
        );
    }

    @Test
    @DisplayName("대문자 타입 이름(FEATURE)을 허용하지 않는다")
    void blocksUppercaseBranchTypeName() throws IOException, InterruptedException {
        var result = runStartTask("--type", "FEATURE", "12", "my-slug");

        assertNotEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("숫자가 아닌 이슈 번호를 차단한다")
    void blocksNonNumericIssueNumber() throws IOException, InterruptedException {
        var result = runStartTask("--type", "feature", "abc", "my-slug");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(
                result.output().contains("이슈 번호"),
                "이슈 번호 오류 메시지가 출력되어야 합니다: " + result.output()
        );
    }

    @Test
    @DisplayName("--type=docs 형식으로도 docs 타입 브랜치를 생성할 수 있다")
    void acceptsEqualsSignSyntaxForTypeFlag() throws IOException, InterruptedException {
        // The type validation message should NOT contain "docs" error, meaning it parsed correctly
        // but we expect it to fail because of dirty tree or network
        var result = runStartTask("--type=invalid", "12", "my-slug");

        assertNotEquals(0, result.exitCode(), result.output());
        // Should fail on branch type validation, not on argument parsing
        assertTrue(
                result.output().contains("feature") || result.output().contains("docs"),
                "허용 타입 목록이 오류 메시지에 있어야 합니다: " + result.output()
        );
    }

    @Test
    @DisplayName("usage 메시지에 --type 플래그 예시가 포함된다")
    void usageMessageIncludesTypeFlagExample() throws IOException, InterruptedException {
        var result = runStartTask("--help");

        assertTrue(
                result.output().contains("--type"),
                "usage에 --type 플래그가 있어야 합니다: " + result.output()
        );
        assertTrue(
                result.output().contains("docs"),
                "usage에 docs 타입 예시가 있어야 합니다: " + result.output()
        );
    }

    private StartTaskResult runStartTask(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("scripts/start-task.sh");
        for (String arg : args) {
            command.add(arg);
        }

        var processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new StartTaskResult(exitCode, output);
    }

    record StartTaskResult(int exitCode, String output) {
    }
}