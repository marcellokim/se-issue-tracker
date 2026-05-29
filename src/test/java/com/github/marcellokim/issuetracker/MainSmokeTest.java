package com.github.marcellokim.issuetracker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("애플리케이션 진입점")
class MainSmokeTest {

    @Test
    @DisplayName("main entry point prints startup message without CLI command routing")
    void mainPrintsStartupMessageWithoutCliRouting() {
        String text = captureMainOutput("ignored");

        assertTrue(text.contains("Issue Tracker application started."));
        assertTrue(text.contains("UI entry point is not available yet."));
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
}
