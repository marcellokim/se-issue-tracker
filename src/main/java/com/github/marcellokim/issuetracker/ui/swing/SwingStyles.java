package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;

final class SwingStyles {

    static final Dimension WINDOW_SIZE = new Dimension(1024, 768);
    static final int OUTER_PADDING = 32;
    static final int LOGIN_PANEL_WIDTH = 360;
    static final int FIELD_HEIGHT = 34;
    static final int BUTTON_HEIGHT = 36;
    static final int ROW_GAP = 8;
    static final int SECTION_GAP = 16;
    static final Color BACKGROUND = new Color(245, 247, 250);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(218, 224, 231);
    static final Color PRIMARY = new Color(32, 96, 160);
    static final Color PRIMARY_TEXT = Color.WHITE;
    static final Color BODY_TEXT = new Color(36, 43, 51);
    static final Color MUTED_TEXT = new Color(95, 106, 120);
    static final Color ERROR_TEXT = new Color(154, 35, 45);

    private SwingStyles() {
    }

    static Border surfaceBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(24, 24, 24, 24));
    }

    static void applyTitle(JLabel label) {
        label.setForeground(BODY_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
    }

    static void applySectionTitle(JLabel label) {
        label.setForeground(BODY_TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
    }

    static void applyMuted(JLabel label) {
        label.setForeground(MUTED_TEXT);
    }

    static void applyPrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(PRIMARY_TEXT);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(LOGIN_PANEL_WIDTH, BUTTON_HEIGHT));
    }

    static void fixHeight(JComponent component, int height) {
        Dimension preferred = component.getPreferredSize();
        Dimension size = new Dimension(LOGIN_PANEL_WIDTH, height);
        if (preferred.width > LOGIN_PANEL_WIDTH) {
            size.width = preferred.width;
        }
        component.setPreferredSize(size);
        component.setMinimumSize(size);
    }
}
