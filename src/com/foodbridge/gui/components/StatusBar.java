package com.foodbridge.gui.components;

import com.foodbridge.config.DBConnection;
import com.foodbridge.gui.Theme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;

/**
 * Bottom status bar providing real-time system feedback,
 * asynchronous operation status, and active database mode.
 * Pure AWT component.
 */
public class StatusBar extends Panel {

    private final Label messageLabel;
    private final Label dbModeLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 32));
        setBackground(Theme.BG_SIDEBAR);

        // Message indicator on the left
        Panel leftPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        leftPanel.setBackground(Theme.BG_SIDEBAR);
        messageLabel = new Label("System Ready");
        messageLabel.setFont(Theme.FONT_SMALL);
        messageLabel.setForeground(Theme.TEXT_MUTED);
        leftPanel.add(messageLabel);

        // DB Mode on the right
        Panel rightPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        rightPanel.setBackground(Theme.BG_SIDEBAR);
        dbModeLabel = new Label();
        dbModeLabel.setFont(Theme.FONT_SMALL_BOLD);
        refreshDbMode();
        rightPanel.add(dbModeLabel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setMessage(String message, boolean isError) {
        messageLabel.setText(message);
        if (isError) {
            messageLabel.setForeground(Theme.STATUS_EXPIRED);
        } else {
            messageLabel.setForeground(Theme.TEXT_DARK);
        }
    }

    public void refreshDbMode() {
        try {
            String mode = DBConnection.getActiveMode();
            if ("mysql".equalsIgnoreCase(mode)) {
                dbModeLabel.setText("Database: MySQL (Active)");
                dbModeLabel.setForeground(Theme.PRIMARY);
            } else {
                dbModeLabel.setText("Database: Embedded H2 (Local)");
                dbModeLabel.setForeground(Theme.STATUS_CLAIMED);
            }
        } catch (Exception e) {
            dbModeLabel.setText("Database: Unconnected");
            dbModeLabel.setForeground(Theme.STATUS_EXPIRED);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        // Draw top border
        g.setColor(Theme.BORDER);
        g.drawLine(0, 0, getWidth(), 0);
    }
}
