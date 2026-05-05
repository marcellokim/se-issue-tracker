package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("공개 이력 작성자 표기 정책")
class PublicAttributionPolicyTest {

    @Test
    @DisplayName("공동작성자 trailer가 있는 커밋 메시지를 차단한다")
    void blocksCommitMessageWithCoAuthorTrailer() throws IOException, InterruptedException {
        Path message = Files.createTempFile("commit-message", ".txt");
        Files.writeString(message, "Policy update\n\n" + "Co-authored-" + "by: Tool <tool@example.test>\n");

        var result = runPolicy("--commit-msg", message.toString());

        assertNotEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("외부 도구명이 있는 커밋 메시지를 차단한다")
    void blocksCommitMessageWithExternalToolName() throws IOException, InterruptedException {
        Path message = Files.createTempFile("commit-message", ".txt");
        Files.writeString(message, "Policy update\n\nGenerated with " + "O" + "m" + "X" + "\n");

        var result = runPolicy("--commit-msg", message.toString());

        assertNotEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("깨끗한 커밋 메시지는 허용한다")
    void allowsCleanCommitMessage() throws IOException, InterruptedException {
        Path message = Files.createTempFile("commit-message", ".txt");
        Files.writeString(message, """
                Keep repository authorship policy explicit

                Constraint: Public history must show only human repository contributors.
                Rejected: Tool-specific attribution trailers | they alter public authorship display.
                Confidence: high
                Scope-risk: narrow
                Tested: attribution policy smoke test
                Not-tested: GitHub UI rendering
                """);

        var result = runPolicy("--commit-msg", message.toString());

        assertEquals(0, result.exitCode(), result.output());
    }

    private PolicyResult runPolicy(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 2];
        command[0] = "bash";
        command[1] = "scripts/validate-public-attribution.sh";
        System.arraycopy(args, 0, command, 2, args.length);

        var processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new PolicyResult(exitCode, output);
    }

    record PolicyResult(int exitCode, String output) {
    }
}
