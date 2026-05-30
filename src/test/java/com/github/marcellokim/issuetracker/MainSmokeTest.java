package com.github.marcellokim.issuetracker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("애플리케이션 진입점")
class MainSmokeTest {

    @Test
    @DisplayName("Main class loads without error")
    void mainClassLoadsWithoutError() {
        assertDoesNotThrow(() -> Class.forName("com.github.marcellokim.issuetracker.Main"));
    }
}
