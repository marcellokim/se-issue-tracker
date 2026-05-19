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
    @DisplayName("초기 실행 진입점은 화면 구현 전에도 기동 메시지를 출력한다")
    void mainPrintsStartupMessage() {
        var originalOut = System.out;
        var output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            Main.main(new String[] {"--cli-demo"});
        } finally {
            System.setOut(originalOut);
        }

        String text = output.toString(StandardCharsets.UTF_8);

        assertTrue(text.contains("Issue Tracker application started."));
        assertTrue(text.contains("Oracle repository demo"));
    }
}
