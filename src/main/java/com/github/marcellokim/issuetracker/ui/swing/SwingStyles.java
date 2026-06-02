package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.JTableHeader;

final class SwingStyles {

    static final Dimension WINDOW_SIZE = new Dimension(1024, 768);
    static final Dimension MINIMUM_SIZE = new Dimension(800, 600);
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
    static final Color BUTTON_BACKGROUND = new Color(248, 250, 252);
    static final Color BUTTON_BORDER = new Color(148, 163, 184);
    static final Color DISABLED_BUTTON_BACKGROUND = new Color(229, 234, 240);
    static final Color BODY_TEXT = new Color(36, 43, 51);
    static final Color MUTED_TEXT = new Color(95, 106, 120);
    static final Color ERROR_TEXT = new Color(154, 35, 45);
    static final Color TABLE_HEADER_BACKGROUND = new Color(241, 245, 249);
    static final Color TABLE_GRID = new Color(203, 213, 225);

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
        button.setUI(new BasicButtonUI());
        button.setBackground(PRIMARY);
        button.setForeground(PRIMARY_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY.darker()),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(LOGIN_PANEL_WIDTH, BUTTON_HEIGHT));
    }

    static void applyPrimaryButtonState(JButton button, boolean enabled) {
        button.setBackground(enabled ? PRIMARY : DISABLED_BUTTON_BACKGROUND);
    }

    static void applySecondaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(BODY_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BORDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }

    static void applyTableStyle(JTable table) {
        table.setRowHeight(28);
        table.setGridColor(TABLE_GRID);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(TABLE_HEADER_BACKGROUND);
            header.setForeground(BODY_TEXT);
            header.setFont(header.getFont().deriveFont(Font.BOLD));
        }
    }

    static void disableHeaderReordering(JTable table) {
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
        }
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
