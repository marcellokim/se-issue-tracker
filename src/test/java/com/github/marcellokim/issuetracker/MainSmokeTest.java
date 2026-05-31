package com.github.marcellokim.issuetracker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("application entry point")
class MainSmokeTest {

    @Test
    @DisplayName("Main class loads without error")
    void mainClassLoadsWithoutError() {
        assertDoesNotThrow(() -> Class.forName("com.github.marcellokim.issuetracker.Main"));
    }
}
