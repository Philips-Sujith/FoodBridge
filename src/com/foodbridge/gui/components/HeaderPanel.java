package com.foodbridge.gui.components;

import com.foodbridge.gui.Theme;
import com.foodbridge.model.User;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Custom-painted top application header with brand motif,
 * active user session info, and logout action.
 * Pure AWT component.
 */
public class HeaderPanel extends Panel {

    private final Label userInfoLabel;
    private final Button logoutButton;
    private Runnable onLogoutCallback;

    public HeaderPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 68));
        setBackground(Theme.PRIMARY_DARK);

        // Right-side user status and logout panel
        Panel rightPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 15, 18));
        rightPanel.setBackground(Theme.PRIMARY_DARK);

        userInfoLabel = new Label("Not Logged In");
        userInfoLabel.setForeground(Theme.PRIMARY_LIGHT);
        userInfoLabel.setFont(Theme.FONT_BODY_BOLD);
        rightPanel.add(userInfoLabel);

        logoutButton = new Button("Logout");
        Theme.styleDangerButton(logoutButton);
        logoutButton.setVisible(false);
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onLogoutCallback != null) {
                    onLogoutCallback.run();
                }
            }
        });
        rightPanel.add(logoutButton);

        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Updates active user presentation on header.
     *
     * @param user Active logged-in user or null if logged out
     */
    public void setUser(User user) {
        if (user != null) {
            String roleBadge = "[" + user.getRole().name() + "]";
            userInfoLabel.setText(user.getName() + " " + roleBadge);
            userInfoLabel.setForeground(Color.WHITE);
            logoutButton.setVisible(true);
        } else {
            userInfoLabel.setText("Please log in or register");
            userInfoLabel.setForeground(Theme.PRIMARY_LIGHT);
            logoutButton.setVisible(false);
        }
        validate();
        repaint();
    }

    public void setOnLogout(Runnable callback) {
        this.onLogoutCallback = callback;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int height = getHeight();

        // 1. Draw Brand Motif Icon (Bridge + Sprout Leaf)
        int iconX = 18;
        int iconY = 14;

        // Leaf background circle / badge
        g2.setColor(Theme.ACCENT);
        g2.fillRoundRect(iconX, iconY, 40, 40, 14, 14);

        // Bridge arch
        g2.setColor(Color.WHITE);
        g2.setStroke(new java.awt.BasicStroke(2.5f));
        g2.drawArc(iconX + 6, iconY + 16, 28, 20, 0, 180);

        // Bridge deck line
        g2.drawLine(iconX + 6, iconY + 26, iconX + 34, iconY + 26);

        // Food sprout leaf on top
        g2.fillOval(iconX + 16, iconY + 8, 8, 12);

        // 2. Title Text
        g2.setFont(Theme.FONT_HEADER);
        g2.setColor(Color.WHITE);
        g2.drawString("FoodBridge", iconX + 50, iconY + 22);

        // 3. Subtitle / Tagline
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.PRIMARY_LIGHT);
        g2.drawString("Excess Food Wastage Prevention Platform | Geospatial Haversine Matching", iconX + 50, iconY + 37);

        // Subtle bottom border line
        g2.setColor(Theme.ACCENT);
        g2.drawLine(0, height - 1, getWidth(), height - 1);
    }
}
