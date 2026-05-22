package com.github.marcellokim.issuetracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("애플리케이션 진입점")
class MainSmokeTest {

    private static final Duration MAIN_PROCESS_TIMEOUT = Duration.ofSeconds(10);

    @Test
    @DisplayName("초기 실행 진입점은 화면 구현 전에도 기동 메시지를 출력한다")
    void mainPrintsStartupMessage() {
        String text = captureMainOutput("--cli-demo");

        assertTrue(text.contains("Issue Tracker application started."));
        assertTrue(text.contains("Oracle repository demo"));
    }

    @Test
    @DisplayName("login check without database environment prints setup guide")
    void loginCheckWithoutDatabaseEnvironmentPrintsSetupGuide() throws IOException, InterruptedException {
        String text = captureMainOutputWithoutDatabaseEnvironment("--login-check", "admin", "password");

        assertTrue(text.contains("Issue Tracker application started."));
        assertTrue(text.contains("Oracle repository demo skipped."));
        assertTrue(text.contains("Set database environment variables to print DB/repository status:"));
    }

    @Test
    @DisplayName("login check without credentials prints usage")
    void loginCheckWithoutCredentialsPrintsUsage() {
        String text = captureMainOutput("--login-check", "admin");

        assertTrue(text.contains("Issue Tracker application started."));
        assertTrue(text.contains("Login check skipped."));
        assertTrue(text.contains(".\\gradlew.bat run --args=\"--login-check <loginId> <password>\""));
    }

    private static String captureMainOutput(String... args) {
        var originalOut = System.out;
        var output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            Main.main(args);
        } finally {
            System.setOut(originalOut);
        }

        return output.toString(StandardCharsets.UTF_8);
    }

    private static String captureMainOutputWithoutDatabaseEnvironment(String... args)
            throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add(currentJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Main.class.getName());
        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().remove("ITS_DB_URL");
        processBuilder.environment().remove("ITS_DB_USER");
        processBuilder.environment().remove("ITS_DB_PASSWORD");

        Process process = processBuilder.start();
        boolean finished = process.waitFor(MAIN_PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        if (!finished) {
            process.destroyForcibly();
            process.waitFor(1, TimeUnit.SECONDS);
            String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            fail("Main process timed out after " + MAIN_PROCESS_TIMEOUT + System.lineSeparator() + text);
        }

        String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        assertEquals(0, exitCode, text);
        return text;
    }

    private static String currentJavaExecutable() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }
}
