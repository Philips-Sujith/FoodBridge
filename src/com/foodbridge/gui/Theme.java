package com.foodbridge.gui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;

/**
 * Centralized theme definitions for FoodBridge AWT Graphical Interface.
 * Defines a professional green-themed palette, typography, and styling helpers.
 * PURE AWT: No Swing dependencies.
 */
public class Theme {

    private Theme() {
        // Utility class
    }

    // --- Color Palette ---
    public static final Color PRIMARY = new Color(4, 120, 87);       // Emerald/Forest #047857
    public static final Color PRIMARY_DARK = new Color(6, 78, 59);   // Deep Forest #064E3B
    public static final Color PRIMARY_LIGHT = new Color(236, 253, 245); // Mint Tint #ECFDF5
    public static final Color ACCENT = new Color(16, 185, 129);      // Bright Emerald #10B981

    public static final Color BG_DARK = new Color(15, 23, 42);       // Dark Slate #0F172A
    public static final Color BG_LIGHT = new Color(241, 245, 249);   // Light Gray #F1F5F9
    public static final Color BG_WHITE = new Color(255, 255, 255);   // Crisp White
    public static final Color BG_SIDEBAR = new Color(248, 250, 252); // Sidebar Soft #F8FAFC

    public static final Color TEXT_DARK = new Color(15, 23, 42);     // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139); // Slate 500
    public static final Color TEXT_LIGHT = new Color(255, 255, 255); // White

    public static final Color BORDER = new Color(226, 232, 240);     // Slate 200
    public static final Color ROW_ALT = new Color(248, 250, 252);    // Alternate row
    public static final Color ROW_SELECTED = new Color(209, 250, 229);// Highlighted selected row

    // Status Colors
    public static final Color STATUS_AVAILABLE = new Color(22, 163, 74); // Green
    public static final Color STATUS_CLAIMED = new Color(217, 119, 6);   // Amber
    public static final Color STATUS_EXPIRED = new Color(220, 38, 38);   // Red
    public static final Color IN_RANGE_BG = new Color(220, 252, 231);    // Light green badge
    public static final Color IN_RANGE_TEXT = new Color(21, 128, 61);    // Dark green badge text

    // --- Typography ---
    private static final String FONT_FAMILY = "Segoe UI";
    private static final String FONT_FALLBACK = "SansSerif";

    public static final Font FONT_HEADER = createFont(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = createFont(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY_BOLD = createFont(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_BODY = createFont(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL = createFont(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = createFont(FONT_FAMILY, Font.BOLD, 11);

    private static Font createFont(String name, int style, int size) {
        Font font = new Font(name, style, size);
        if (!font.getFamily().equalsIgnoreCase(name)) {
            font = new Font(FONT_FALLBACK, style, size);
        }
        return font;
    }

    // --- Component Styling Helpers ---

    public static void stylePrimaryButton(Button btn) {
        btn.setBackground(PRIMARY);
        btn.setForeground(TEXT_LIGHT);
        btn.setFont(FONT_BODY_BOLD);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(Button btn) {
        btn.setBackground(BG_LIGHT);
        btn.setForeground(TEXT_DARK);
        btn.setFont(FONT_BODY_BOLD);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleDangerButton(Button btn) {
        btn.setBackground(STATUS_EXPIRED);
        btn.setForeground(TEXT_LIGHT);
        btn.setFont(FONT_BODY_BOLD);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleCardPanel(Panel panel) {
        panel.setBackground(BG_WHITE);
    }
}
