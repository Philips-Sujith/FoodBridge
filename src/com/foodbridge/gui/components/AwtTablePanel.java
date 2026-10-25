package com.foodbridge.gui.components;

import com.foodbridge.gui.Theme;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.ScrollPane;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, razor-sharp custom tabular component built strictly with pure java.awt (NO Swing).
 * 
 * Crisp Font & Rendering Architecture:
 * - Direct vector rendering in paint(Graphics) avoiding offscreen HiDPI raster scaling blur.
 * - Standardized RenderingHints across all text:
 *     KEY_TEXT_ANTIALIASING = VALUE_TEXT_ANTIALIAS_ON
 *     KEY_FRACTIONALMETRICS = VALUE_FRACTIONALMETRICS_OFF
 *     KEY_RENDERING = VALUE_RENDER_QUALITY
 * - Strict integer coordinate typography (no fractional subpixel offsets, no scaling transforms).
 * - Component-level FontMetrics matching the active graphics rasterizer.
 * - Clipped differential row repainting on hover and selection state changes.
 * 
 * Features:
 * 1. Resizable columns via header divider dragging (E_RESIZE_CURSOR, min width 50px).
 * 2. Double-click on header divider to auto-fit to longest text using FontMetrics.
 * 3. Optimal content-aware default widths (up to 340px) so Food Name, Pickup Site, and Claimed By are readable.
 * 4. Horizontal scrolling via ScrollPane with sticky header synchronized in lockstep.
 * 5. Ellipsis ("...") text truncation with lightweight AWT popup tooltip on cell hover.
 * 6. Continuous full-width row highlight rendering (zero vertical strip artifacts).
 * 7. Alternating row backgrounds and status badges (AVAILABLE, CLAIMED, EXPIRED, [IN RANGE]).
 */
public class AwtTablePanel extends Panel {

    public interface RowSelectionListener {
        void onRowSelected(int rowIndex, Object rowData);
    }

    private static final int HEADER_HEIGHT = 34;
    private static final int ROW_HEIGHT = 36;
    private static final int MIN_COL_WIDTH = 50;
    private static final int MAX_DEFAULT_COL_WIDTH = 340;
    private static final int CELL_PADDING_X = 10;

    private final String[] columnNames;
    private final int[] colWidths;
    private final List<RowItem> rowItems = new ArrayList<>();

    private final HeaderCanvas headerCanvas;
    private final TableBodyCanvas bodyCanvas;
    private final ScrollPane scrollPane;

    private int selectedRowIndex = -1;
    private int hoverRowIndex = -1;
    private int hoverColIndex = -1;
    private int currentScrollX = 0;
    private boolean userHasResized = false;

    private RowSelectionListener selectionListener;

    // Tooltip popup window (lightweight pure AWT)
    private Window tooltipWindow;
    private Label tooltipLabel;

    private static class RowItem {
        final String[] cellValues;
        final Object data;

        RowItem(String[] cellValues, Object data) {
            this.cellValues = cellValues;
            this.data = data;
        }
    }

    public AwtTablePanel(String[] columnNames) {
        this.columnNames = columnNames;
        this.colWidths = new int[columnNames.length];

        // Initialize column widths with comfortable defaults based on header labels
        FontMetrics fmHeader = Toolkit.getDefaultToolkit().getFontMetrics(Theme.FONT_BODY_BOLD);
        for (int i = 0; i < columnNames.length; i++) {
            int hWidth = fmHeader.stringWidth(columnNames[i]) + (CELL_PADDING_X * 2) + 16;
            colWidths[i] = Math.max(MIN_COL_WIDTH, Math.min(hWidth + 30, MAX_DEFAULT_COL_WIDTH));
        }

        setLayout(new BorderLayout());
        setBackground(Theme.BG_WHITE);

        // 1. Fixed Header Canvas at top (synchronizes with horizontal scroll position)
        headerCanvas = new HeaderCanvas();
        headerCanvas.setPreferredSize(new Dimension(800, HEADER_HEIGHT));
        add(headerCanvas, BorderLayout.NORTH);

        // 2. Scrollable Body
        scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        bodyCanvas = new TableBodyCanvas();
        scrollPane.add(bodyCanvas);
        add(scrollPane, BorderLayout.CENTER);

        // Synchronize header horizontal offset with scroll pane scrollbar
        scrollPane.getHAdjustable().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                currentScrollX = e.getValue();
                hideTooltip();
                headerCanvas.repaint();
            }
        });

        scrollPane.getVAdjustable().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                hideTooltip();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateDimensions();
            }
        });

        updateDimensions();
    }

    /**
     * Standardizes rendering hints across all Graphics contexts to ensure
     * crisp, razor-sharp text and consistent character advances without fractional blur.
     */
    private static void applyCrispTextHints(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
    }

    public void setSelectionListener(RowSelectionListener listener) {
        this.selectionListener = listener;
    }

    public synchronized void clearRows() {
        rowItems.clear();
        selectedRowIndex = -1;
        hoverRowIndex = -1;
        hoverColIndex = -1;
        hideTooltip();
        userHasResized = false;
        updateDimensions();
        repaint();
    }

    public synchronized void addRow(String[] cellValues, Object rowData) {
        rowItems.add(new RowItem(cellValues, rowData));
        if (!userHasResized) {
            computeOptimalColumnWidths();
        } else {
            updateDimensions();
        }
        repaint();
    }

    public synchronized void selectRow(int index) {
        if (index < 0 || index >= rowItems.size()) {
            return;
        }
        int prevSelected = selectedRowIndex;
        selectedRowIndex = index;

        // Repaint only affected rows for optimal performance
        if (prevSelected >= 0 && prevSelected < rowItems.size()) {
            bodyCanvas.repaintRow(prevSelected);
        }
        bodyCanvas.repaintRow(selectedRowIndex);

        if (selectionListener != null) {
            selectionListener.onRowSelected(selectedRowIndex, rowItems.get(selectedRowIndex).data);
        }
    }

    public int getSelectedRowIndex() {
        return selectedRowIndex;
    }

    public Object getSelectedRowData() {
        if (selectedRowIndex >= 0 && selectedRowIndex < rowItems.size()) {
            return rowItems.get(selectedRowIndex).data;
        }
        return null;
    }

    public int getRowCount() {
        return rowItems.size();
    }

    /**
     * Auto-fits a column to its longest text using FontMetrics.
     */
    public synchronized void autoFitColumn(int col) {
        if (col < 0 || col >= columnNames.length) {
            return;
        }
        FontMetrics fmHeader = getFontMetrics(Theme.FONT_BODY_BOLD);
        FontMetrics fmBody = getFontMetrics(Theme.FONT_BODY);
        FontMetrics fmBodyBold = getFontMetrics(Theme.FONT_BODY_BOLD);

        int maxW = fmHeader.stringWidth(columnNames[col]) + (CELL_PADDING_X * 2) + 14;

        for (RowItem item : rowItems) {
            if (col < item.cellValues.length && item.cellValues[col] != null) {
                String val = item.cellValues[col];
                FontMetrics fm = isHighlightedStatus(val) ? fmBodyBold : fmBody;
                int w = fm.stringWidth(val) + (CELL_PADDING_X * 2) + 14;
                if (w > maxW) {
                    maxW = w;
                }
            }
        }

        colWidths[col] = Math.max(MIN_COL_WIDTH, maxW);
        userHasResized = true;
        updateDimensions();
        repaint();
    }

    private synchronized void computeOptimalColumnWidths() {
        FontMetrics fmHeader = getFontMetrics(Theme.FONT_BODY_BOLD);
        FontMetrics fmBody = getFontMetrics(Theme.FONT_BODY);
        FontMetrics fmBodyBold = getFontMetrics(Theme.FONT_BODY_BOLD);

        for (int col = 0; col < columnNames.length; col++) {
            int maxW = fmHeader.stringWidth(columnNames[col]) + (CELL_PADDING_X * 2) + 14;
            for (RowItem item : rowItems) {
                if (col < item.cellValues.length && item.cellValues[col] != null) {
                    String val = item.cellValues[col];
                    FontMetrics fm = isHighlightedStatus(val) ? fmBodyBold : fmBody;
                    int w = fm.stringWidth(val) + (CELL_PADDING_X * 2) + 14;
                    if (w > maxW) {
                        maxW = w;
                    }
                }
            }
            colWidths[col] = Math.max(MIN_COL_WIDTH, Math.min(maxW, MAX_DEFAULT_COL_WIDTH));
        }
        updateDimensions();
    }

    private synchronized void updateDimensions() {
        int totalColsWidth = getTotalColumnsWidth();
        int viewportW = (scrollPane != null && scrollPane.getWidth() > 0) ? scrollPane.getWidth() : 800;
        int bodyW = Math.max(totalColsWidth, viewportW);
        int bodyH = Math.max(rowItems.size() * ROW_HEIGHT, 150);

        if (bodyCanvas != null) {
            bodyCanvas.setPreferredSize(new Dimension(bodyW, bodyH));
            bodyCanvas.setSize(bodyW, bodyH);
        }
        if (scrollPane != null) {
            scrollPane.validate();
        }
        if (headerCanvas != null) {
            headerCanvas.repaint();
        }
        if (bodyCanvas != null) {
            bodyCanvas.repaint();
        }
    }

    private int getTotalColumnsWidth() {
        int total = 0;
        for (int w : colWidths) {
            total += w;
        }
        return total;
    }

    private int getColumnAtX(int x) {
        int cumX = 0;
        for (int i = 0; i < colWidths.length; i++) {
            cumX += colWidths[i];
            if (x < cumX) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHighlightedStatus(String val) {
        return val != null && (val.contains("AVAILABLE") || val.contains("CLAIMED") || val.contains("[IN RANGE]"));
    }

    private static void drawTruncatedString(Graphics g, String text, FontMetrics fm, int x, int y, int availWidth) {
        if (text == null || text.isEmpty() || availWidth <= 0) {
            return;
        }
        if (fm.stringWidth(text) <= availWidth) {
            g.drawString(text, x, y);
            return;
        }

        String dots = "...";
        int dotsW = fm.stringWidth(dots);
        if (availWidth <= dotsW) {
            g.drawString(dots, x, y);
            return;
        }

        int targetWidth = availWidth - dotsW;
        int len = text.length();
        while (len > 0 && fm.stringWidth(text.substring(0, len)) > targetWidth) {
            len--;
        }
        g.drawString(text.substring(0, len) + dots, x, y);
    }

    // ==========================================================
    // Tooltip Implementation (Pure Java AWT)
    // ==========================================================

    private Frame getParentFrame() {
        Component c = this;
        while (c != null && !(c instanceof Frame)) {
            c = c.getParent();
        }
        return (Frame) c;
    }

    private void showTooltip(String text, int screenX, int screenY) {
        if (text == null || text.trim().isEmpty()) {
            hideTooltip();
            return;
        }
        Frame frame = getParentFrame();
        if (frame == null) {
            return;
        }

        if (tooltipWindow == null) {
            tooltipWindow = new Window(frame) {
                @Override
                public void update(Graphics g) {
                    paint(g);
                }

                @Override
                public void paint(Graphics g) {
                    applyCrispTextHints(g);
                    super.paint(g);
                    g.setColor(Theme.BORDER);
                    g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            };
            tooltipWindow.setBackground(new Color(254, 252, 232)); // soft warm tooltip yellow
            tooltipWindow.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
            tooltipLabel = new Label();
            tooltipLabel.setFont(Theme.FONT_SMALL);
            tooltipLabel.setForeground(Theme.TEXT_DARK);
            tooltipWindow.add(tooltipLabel);
        }

        tooltipLabel.setText(text);
        tooltipWindow.pack();

        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension sz = tooltipWindow.getSize();
        int x = screenX + 12;
        int y = screenY + 18;
        if (x + sz.width > scr.width - 10) {
            x = scr.width - sz.width - 10;
        }
        if (y + sz.height > scr.height - 10) {
            y = screenY - sz.height - 6;
        }
        tooltipWindow.setLocation(x, y);
        tooltipWindow.setVisible(true);
    }

    private void hideTooltip() {
        if (tooltipWindow != null && tooltipWindow.isVisible()) {
            tooltipWindow.setVisible(false);
        }
    }

    @Override
    public void removeNotify() {
        hideTooltip();
        if (tooltipWindow != null) {
            tooltipWindow.dispose();
            tooltipWindow = null;
        }
        super.removeNotify();
    }

    // ==========================================================
    // 1. Header Component (Direct Crisp Rendering)
    // ==========================================================

    private class HeaderCanvas extends Canvas {

        private int hoverDividerIndex = -1;
        private int dragDividerIndex = -1;
        private int dragStartX = 0;
        private int dragStartWidth = 0;

        HeaderCanvas() {
            setBackground(Theme.BG_SIDEBAR);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    hideTooltip();
                    if (hoverDividerIndex >= 0) {
                        dragDividerIndex = hoverDividerIndex;
                        int tableX = e.getX() + currentScrollX;
                        dragStartX = tableX;
                        dragStartWidth = colWidths[dragDividerIndex];
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragDividerIndex >= 0) {
                        dragDividerIndex = -1;
                        userHasResized = true;
                        updateDimensions();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && hoverDividerIndex >= 0) {
                        autoFitColumn(hoverDividerIndex);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (dragDividerIndex < 0 && hoverDividerIndex >= 0) {
                        hoverDividerIndex = -1;
                        setCursor(Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int tableX = e.getX() + currentScrollX;
                    int hit = findDividerNear(tableX);
                    if (hit != hoverDividerIndex) {
                        hoverDividerIndex = hit;
                        if (hoverDividerIndex >= 0) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                        } else {
                            setCursor(Cursor.getDefaultCursor());
                        }
                        repaint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragDividerIndex >= 0) {
                        userHasResized = true;
                        int tableX = e.getX() + currentScrollX;
                        int delta = tableX - dragStartX;
                        int newW = Math.max(MIN_COL_WIDTH, dragStartWidth + delta);
                        colWidths[dragDividerIndex] = newW;
                        updateDimensions();
                    }
                }
            });
        }

        private int findDividerNear(int tableX) {
            int cumX = 0;
            for (int i = 0; i < columnNames.length; i++) {
                cumX += colWidths[i];
                if (Math.abs(tableX - cumX) <= 5) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void update(Graphics g) {
            // Avoid default component background clearing; paint directly
            paint(g);
        }

        @Override
        public void paint(Graphics g) {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            // Apply consistent crisp text rendering hints directly on graphics context
            applyCrispTextHints(g);

            // Fill header background across full viewport width
            g.setColor(Theme.BG_SIDEBAR);
            g.fillRect(0, 0, width, height);

            g.setFont(Theme.FONT_BODY_BOLD);
            FontMetrics fm = g.getFontMetrics(Theme.FONT_BODY_BOLD);
            int textY = (HEADER_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

            int x = -currentScrollX;
            for (int i = 0; i < columnNames.length; i++) {
                int colW = colWidths[i];

                // Paint visible header cells
                if (x + colW >= 0 && x <= width) {
                    Shape origClip = g.getClip();
                    g.clipRect(Math.max(0, x), 0, Math.min(colW, width - x), height);

                    g.setColor(Theme.TEXT_DARK);
                    drawTruncatedString(g, columnNames[i], fm, x + CELL_PADDING_X, textY, colW - (CELL_PADDING_X * 2));

                    g.setClip(origClip);

                    // Divider line
                    g.setColor(Theme.BORDER);
                    g.drawLine(x + colW - 1, 4, x + colW - 1, height - 5);

                    // Hovered divider highlight
                    if (hoverDividerIndex == i || dragDividerIndex == i) {
                        g.setColor(Theme.PRIMARY);
                        g.drawLine(x + colW - 1, 0, x + colW - 1, height);
                        g.drawLine(x + colW - 2, 0, x + colW - 2, height);
                    }
                }
                x += colW;
            }

            // Bottom border
            g.setColor(Theme.BORDER);
            g.drawLine(0, height - 1, width, height - 1);
        }
    }

    // ==========================================================
    // 2. Table Body Component (Direct Crisp Vector Rendering)
    // ==========================================================

    private class TableBodyCanvas extends Canvas {

        TableBodyCanvas() {
            setBackground(Theme.BG_WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    hideTooltip();
                    int row = e.getY() / ROW_HEIGHT;
                    if (row >= 0 && row < rowItems.size()) {
                        selectRow(row);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoverRowIndex != -1) {
                        int prevHover = hoverRowIndex;
                        hoverRowIndex = -1;
                        hoverColIndex = -1;
                        repaintRow(prevHover);
                    }
                    hideTooltip();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int row = e.getY() / ROW_HEIGHT;
                    int col = getColumnAtX(e.getX());

                    int prevHover = hoverRowIndex;
                    hoverRowIndex = (row >= 0 && row < rowItems.size()) ? row : -1;
                    hoverColIndex = col;

                    if (prevHover != hoverRowIndex) {
                        if (prevHover >= 0 && prevHover < rowItems.size()) {
                            repaintRow(prevHover);
                        }
                        if (hoverRowIndex >= 0 && hoverRowIndex < rowItems.size()) {
                            repaintRow(hoverRowIndex);
                        }
                    }

                    if (hoverRowIndex >= 0 && hoverColIndex >= 0 && hoverColIndex < columnNames.length) {
                        RowItem item = rowItems.get(hoverRowIndex);
                        String val = (hoverColIndex < item.cellValues.length && item.cellValues[hoverColIndex] != null)
                                ? item.cellValues[hoverColIndex] : "";
                        if (!val.isEmpty()) {
                            FontMetrics fm = getFontMetrics(isHighlightedStatus(val) ? Theme.FONT_BODY_BOLD : Theme.FONT_BODY);
                            int avail = colWidths[hoverColIndex] - (CELL_PADDING_X * 2);
                            if (fm.stringWidth(val) > avail) {
                                showTooltip(val, e.getXOnScreen(), e.getYOnScreen());
                                return;
                            }
                        }
                    }
                    hideTooltip();
                }
            });
        }

        void repaintRow(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < rowItems.size()) {
                int rowY = rowIndex * ROW_HEIGHT;
                int rowW = Math.max(getTotalColumnsWidth(), getWidth());
                repaint(0, rowY, rowW, ROW_HEIGHT);
            }
        }

        @Override
        public void update(Graphics g) {
            // Avoid default component background clearing; paint directly
            paint(g);
        }

        @Override
        public void paint(Graphics g) {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            // Apply consistent crisp text rendering hints directly on graphics context
            applyCrispTextHints(g);

            int totalColsW = getTotalColumnsWidth();
            int totalRowW = Math.max(totalColsW, width);
            int rowCount = rowItems.size();

            if (rowCount == 0) {
                g.setColor(Theme.BG_WHITE);
                g.fillRect(0, 0, totalRowW, height);
                g.setColor(Theme.TEXT_MUTED);
                g.setFont(Theme.FONT_BODY);
                g.drawString("No records available in this view.", 20, 35);
                return;
            }

            Rectangle clip = g.getClipBounds();
            int startRow = 0;
            int endRow = rowCount - 1;
            if (clip != null) {
                startRow = Math.max(0, clip.y / ROW_HEIGHT);
                endRow = Math.min(rowCount - 1, (clip.y + clip.height) / ROW_HEIGHT);
            }

            FontMetrics fmPlain = g.getFontMetrics(Theme.FONT_BODY);
            FontMetrics fmBold = g.getFontMetrics(Theme.FONT_BODY_BOLD);

            for (int r = startRow; r <= endRow; r++) {
                int rowY = r * ROW_HEIGHT;
                RowItem item = rowItems.get(r);

                // Continuous full-width rectangle behind all cells: eliminates vertical stripes
                if (r == selectedRowIndex) {
                    g.setColor(Theme.ROW_SELECTED);
                    g.fillRect(0, rowY, totalRowW, ROW_HEIGHT);
                } else if (r == hoverRowIndex) {
                    g.setColor(Theme.PRIMARY_LIGHT);
                    g.fillRect(0, rowY, totalRowW, ROW_HEIGHT);
                } else if (r % 2 == 0) {
                    g.setColor(Theme.BG_WHITE);
                    g.fillRect(0, rowY, totalRowW, ROW_HEIGHT);
                } else {
                    g.setColor(Theme.ROW_ALT);
                    g.fillRect(0, rowY, totalRowW, ROW_HEIGHT);
                }

                // Row bottom separator line
                g.setColor(Theme.BORDER);
                g.drawLine(0, rowY + ROW_HEIGHT - 1, totalRowW, rowY + ROW_HEIGHT - 1);

                // Paint cell contents
                int colX = 0;
                for (int c = 0; c < columnNames.length; c++) {
                    int colW = colWidths[c];
                    String val = (c < item.cellValues.length && item.cellValues[c] != null) ? item.cellValues[c] : "";

                    // Determine typography and status color
                    Color textColor = Theme.TEXT_DARK;
                    Font textFont = Theme.FONT_BODY;
                    FontMetrics fm = fmPlain;

                    if (val.contains("AVAILABLE")) {
                        textColor = Theme.STATUS_AVAILABLE;
                        textFont = Theme.FONT_BODY_BOLD;
                        fm = fmBold;
                    } else if (val.contains("CLAIMED")) {
                        textColor = Theme.STATUS_CLAIMED;
                        textFont = Theme.FONT_BODY_BOLD;
                        fm = fmBold;
                    } else if (val.contains("EXPIRED")) {
                        textColor = Theme.STATUS_EXPIRED;
                        textFont = Theme.FONT_BODY;
                        fm = fmPlain;
                    } else if (val.contains("[IN RANGE]")) {
                        textColor = Theme.IN_RANGE_TEXT;
                        textFont = Theme.FONT_BODY_BOLD;
                        fm = fmBold;
                    }

                    g.setFont(textFont);
                    g.setColor(textColor);
                    int textY = rowY + (ROW_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

                    // Clip to cell bounds so text never bleeds into adjacent columns
                    Shape origClip = g.getClip();
                    g.clipRect(colX, rowY, colW, ROW_HEIGHT);
                    drawTruncatedString(g, val, fm, colX + CELL_PADDING_X, textY, colW - (CELL_PADDING_X * 2));
                    g.setClip(origClip);

                    // Subtle vertical column separator line
                    g.setColor(new Color(241, 245, 249));
                    g.drawLine(colX + colW - 1, rowY, colX + colW - 1, rowY + ROW_HEIGHT - 1);

                    colX += colW;
                }
            }

            // Fill leftover vertical space at bottom with clean background
            int renderedH = rowCount * ROW_HEIGHT;
            if (renderedH < height) {
                g.setColor(Theme.BG_WHITE);
                g.fillRect(0, renderedH, totalRowW, height - renderedH);
            }
        }
    }
}
