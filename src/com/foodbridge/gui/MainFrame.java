package com.foodbridge.gui;

import com.foodbridge.gui.components.HeaderPanel;
import com.foodbridge.gui.components.StatusBar;
import com.foodbridge.model.User;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application frame for the FoodBridge AWT Graphical Interface.
 * Coordinates views using CardLayout with a fixed sensible size (1000x650),
 * custom header, status bar, and safe window closing.
 * Pure AWT: No Swing dependencies.
 */
public class MainFrame extends Frame {

    public static final int DEFAULT_WIDTH = 1000;
    public static final int DEFAULT_HEIGHT = 650;

    private final HeaderPanel headerPanel;
    private final StatusBar statusBar;
    private final CardLayout cardLayout;
    private final Panel cardContainer;

    private User activeUser;
    private LoginPanel loginPanel;
    private RestaurantPanel restaurantPanel;
    private NgoPanel ngoPanel;

    public MainFrame() {
        super("FoodBridge — Excess Food Wastage Prevention Platform");

        // 1. Window sizing and centering (1000x650, minimum 1000x650)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setBackground(Theme.BG_LIGHT);

        // 2. Custom Top Header
        headerPanel = new HeaderPanel();
        headerPanel.setOnLogout(new Runnable() {
            @Override
            public void run() {
                logout();
            }
        });
        add(headerPanel, BorderLayout.NORTH);

        // 3. Bottom Status Bar
        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // 4. Central Card Layout Container for Screen Switching
        cardLayout = new CardLayout();
        cardContainer = new Panel(cardLayout);
        cardContainer.setBackground(Theme.BG_LIGHT);
        add(cardContainer, BorderLayout.CENTER);

        // 5. Clean Window Close handling
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
    }

    /**
     * Initializes all production screens and shows the Login screen.
     */
    public void initScreens() {
        loginPanel = new LoginPanel(this);
        restaurantPanel = new RestaurantPanel(this);
        ngoPanel = new NgoPanel(this);

        addScreen(loginPanel, "LOGIN");
        addScreen(restaurantPanel, "RESTAURANT");
        addScreen(ngoPanel, "NGO");

        showScreen("LOGIN");
    }

    /**
     * Registers a screen panel in the CardLayout.
     *
     * @param component The Panel to register
     * @param name The unique identifier for this screen
     */
    public void addScreen(Panel component, String name) {
        cardContainer.add(component, name);
    }

    /**
     * Switches the currently visible screen.
     *
     * @param name The registered identifier of the screen
     */
    public void showScreen(String name) {
        cardLayout.show(cardContainer, name);
        cardContainer.validate();
        cardContainer.repaint();
    }

    /**
     * Sets the logged-in user and updates header, status bar, and routes to appropriate dashboard.
     */
    public void setActiveUser(User user) {
        this.activeUser = user;
        headerPanel.setUser(user);
        if (user != null) {
            setStatusMessage("Logged in as: " + user.getName() + " (" + user.getRole().name() + ")", false);
            if (user.getRole() == com.foodbridge.model.Role.RESTAURANT && restaurantPanel != null) {
                showScreen("RESTAURANT");
                restaurantPanel.loadData();
            } else if (user.getRole() == com.foodbridge.model.Role.NGO && ngoPanel != null) {
                showScreen("NGO");
                ngoPanel.loadData();
            }
        } else {
            setStatusMessage("Please sign in or create an account.", false);
            showScreen("LOGIN");
        }
    }

    public User getActiveUser() {
        return activeUser;
    }

    /**
     * Resets active session and transitions to the login screen.
     */
    public void logout() {
        setActiveUser(null);
        setStatusMessage("You have been logged out successfully.", false);
    }

    /**
     * Updates message on the bottom status bar.
     */
    public void setStatusMessage(String message, boolean isError) {
        statusBar.setMessage(message, isError);
    }

    public HeaderPanel getHeaderPanel() {
        return headerPanel;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }
}

