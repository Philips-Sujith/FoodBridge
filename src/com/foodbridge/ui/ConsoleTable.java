package com.foodbridge.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Robust, reusable console table formatter for FoodBridge.
 * Guarantees exact column boundary alignment across all terminal rows,
 * handles ANSI escape codes without distorting column widths,
 * and provides clean, graceful truncation for long text values.
 */
public class ConsoleTable {

    public enum Alignment {
        LEFT,
        RIGHT,
        CENTER
    }

    public static class Column {
        private final String header;
        private final int width;
        private final Alignment alignment;

        public Column(String header, int width, Alignment alignment) {
            this.header = header != null ? header : "";
            this.width = Math.max(width, 1);
            this.alignment = alignment != null ? alignment : Alignment.LEFT;
        }

        public String getHeader() {
            return header;
        }

        public int getWidth() {
            return width;
        }

        public Alignment getAlignment() {
            return alignment;
        }
    }

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[0-9;]*[a-zA-Z]");

    // Border styling characters
    private static final String CORNER = "+";
    private static final String HORIZONTAL = "-";
    private static final String VERTICAL = "|";

    private final List<Column> columns = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public ConsoleTable addColumn(String header, int width, Alignment alignment) {
        columns.add(new Column(header, width, alignment));
        return this;
    }

    public ConsoleTable addRow(String... cells) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            if (i < cells.length && cells[i] != null) {
                row.add(cells[i]);
            } else {
                row.add("");
            }
        }
        rows.add(row);
        return this;
    }

    public static String stripAnsi(String input) {
        if (input == null) return "";
        return ANSI_PATTERN.matcher(input).replaceAll("");
    }

    public static int visibleLength(String input) {
        return stripAnsi(input).length();
    }

    /**
     * Formats a single cell value to fit the specified width with proper alignment.
     * Truncates long text gracefully with "..." while preserving ANSI escape sequences.
     */
    public static String formatCell(String text, int width, Alignment alignment) {
        if (text == null) text = "";

        int vLen = visibleLength(text);

        // If text exceeds width, cleanly truncate it
        if (vLen > width) {
            String clean = stripAnsi(text);
            if (width <= 3) {
                text = clean.substring(0, width);
            } else {
                text = clean.substring(0, width - 3) + "...";
            }
            vLen = width;
        }

        int paddingNeeded = Math.max(0, width - vLen);

        switch (alignment) {
            case RIGHT:
                return " ".repeat(paddingNeeded) + text;
            case CENTER:
                int leftPad = paddingNeeded / 2;
                int rightPad = paddingNeeded - leftPad;
                return " ".repeat(leftPad) + text + " ".repeat(rightPad);
            case LEFT:
            default:
                return text + " ".repeat(paddingNeeded);
        }
    }

    private String buildBorderLine() {
        StringBuilder sb = new StringBuilder();
        for (Column col : columns) {
            sb.append(CORNER);
            sb.append(HORIZONTAL.repeat(col.getWidth() + 2)); // 1 leading space + width + 1 trailing space
        }
        sb.append(CORNER);
        return sb.toString();
    }

    /**
     * Prints the complete, beautifully aligned table to System.out.
     */
    public void print() {
        if (columns.isEmpty()) {
            return;
        }

        String border = buildBorderLine();
        String grayBorder = ConsoleUI.GRAY + border + ConsoleUI.RESET;

        // Top border
        System.out.println(grayBorder);

        // Header row
        StringBuilder headerSb = new StringBuilder();
        for (Column col : columns) {
            headerSb.append(ConsoleUI.GRAY).append(VERTICAL).append(" ").append(ConsoleUI.RESET);
            headerSb.append(ConsoleUI.BOLD)
                    .append(formatCell(col.getHeader(), col.getWidth(), col.getAlignment()))
                    .append(ConsoleUI.RESET);
            headerSb.append(" ");
        }
        headerSb.append(ConsoleUI.GRAY).append(VERTICAL).append(ConsoleUI.RESET);
        System.out.println(headerSb.toString());

        // Header separator
        System.out.println(grayBorder);

        // Data rows
        if (rows.isEmpty()) {
            // Display empty notice centered
            int innerWidth = border.length() - 2;
            String emptyNotice = "(No records to display)";
            int pad = Math.max(0, innerWidth - emptyNotice.length());
            int left = pad / 2;
            int right = pad - left;
            System.out.println(ConsoleUI.GRAY + VERTICAL + " ".repeat(left) + ConsoleUI.RESET +
                    ConsoleUI.DIM + emptyNotice + ConsoleUI.RESET +
                    ConsoleUI.GRAY + " ".repeat(right) + VERTICAL + ConsoleUI.RESET);
        } else {
            for (List<String> row : rows) {
                StringBuilder rowSb = new StringBuilder();
                for (int i = 0; i < columns.size(); i++) {
                    Column col = columns.get(i);
                    String rawVal = row.get(i);
                    rowSb.append(ConsoleUI.GRAY).append(VERTICAL).append(" ").append(ConsoleUI.RESET);
                    rowSb.append(formatCell(rawVal, col.getWidth(), col.getAlignment()));
                    rowSb.append(" ");
                }
                rowSb.append(ConsoleUI.GRAY).append(VERTICAL).append(ConsoleUI.RESET);
                System.out.println(rowSb.toString());
            }
        }

        // Bottom border
        System.out.println(grayBorder);
    }
}
