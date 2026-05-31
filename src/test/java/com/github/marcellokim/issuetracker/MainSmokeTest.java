package com.github.marcellokim.issuetracker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("애플리케이션 진입점")
class MainSmokeTest {

    @Test
    @DisplayName("Main class loads without error")
    void mainClassLoadsWithoutError() {
        assertDoesNotThrow(() -> Class.forName("com.github.marcellokim.issuetracker.Main"));
    }

    @Test
    @DisplayName("Swing app class loads without error")
    void swingAppClassLoadsWithoutError() {
        assertDoesNotThrow(() -> Class.forName("com.github.marcellokim.issuetracker.ui.swing.SwingApp"));
    }

    @Test
    @DisplayName("Swing 실행 옵션을 인식한다")
    void recognizesSwingLaunchOption() {
        assertTrue(Main.shouldLaunchSwing(new String[]{"--swing"}));
        assertFalse(Main.shouldLaunchSwing(new String[0]));
        assertFalse(Main.shouldLaunchSwing(new String[]{"--javafx"}));
    }

    @Test
    @DisplayName("Swing app exposes reusable launch entry point")
    void swingAppExposesLaunchEntryPoint() {
        assertDoesNotThrow(() -> Class
                .forName("com.github.marcellokim.issuetracker.ui.swing.SwingApp")
                .getDeclaredMethod("launch"));
    }
}
