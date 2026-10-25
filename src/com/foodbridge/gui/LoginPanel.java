package com.foodbridge.gui;

import com.foodbridge.dao.LocationDAO;
import com.foodbridge.gui.components.DialogHelper;
import com.foodbridge.model.Location;
import com.foodbridge.model.Role;
import com.foodbridge.model.User;
import com.foodbridge.service.AuthService;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Login and Registration screen using pure java.awt.
 * Features:
 * - Dynamic role selector (Choice: NGO / RESTAURANT)
 * - Masked password field (setEchoChar('*'))
 * - Location selector loaded dynamically from 50 predefined localities (required for NGO)
 * - Seamless toggle between Login and Register modes
 * - Quick-login shortcuts for evaluator demonstration
 */
public class LoginPanel extends Panel {

    private final MainFrame mainFrame;
    private final AuthService authService;
    private final LocationDAO locationDAO;
    private final List<Location> locationsList = new ArrayList<>();

    private boolean isRegisterMode = false;

    // Form Components
    private final Label formTitleLabel;
    private final Label roleLabel;
    private final Choice roleChoice;
    private final Label nameLabel;
    private final TextField nameField;
    private final Label phoneLabel;
    private final TextField phoneField;
    private final Label usernameLabel;
    private final TextField usernameField;
    private final Label passwordLabel;
    private final TextField passwordField;
    private final Label locationLabel;
    private final Choice locationChoice;
    private final Label locationHelpLabel;

    private final Button actionButton;
    private final Button toggleModeButton;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = new AuthService();
        this.locationDAO = new LocationDAO();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_LIGHT);

        // Load 50 predefined locations from database
        loadLocations();

        // Outer centering container
        Panel centerWrapper = new Panel(new GridBagLayout());
        centerWrapper.setBackground(Theme.BG_LIGHT);

        // Card Panel (White background)
        Panel card = new Panel(new BorderLayout());
        card.setBackground(Theme.BG_WHITE);
        card.setPreferredSize(new Dimension(540, 520));

        // 1. Card Header
        Panel cardHeader = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 16));
        cardHeader.setBackground(Theme.PRIMARY_DARK);
        formTitleLabel = new Label("Welcome to FoodBridge — Sign In");
        formTitleLabel.setFont(Theme.FONT_SUBTITLE);
        formTitleLabel.setForeground(Theme.TEXT_LIGHT);
        cardHeader.add(formTitleLabel);
        card.add(cardHeader, BorderLayout.NORTH);

        // 2. Form Body (GridBagLayout)
        Panel formBody = new Panel(new GridBagLayout());
        formBody.setBackground(Theme.BG_WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Role Choice (Register mode only)
        roleLabel = new Label("Select Your Role:");
        roleLabel.setFont(Theme.FONT_BODY_BOLD);
        roleLabel.setForeground(Theme.TEXT_DARK);
        roleChoice = new Choice();
        roleChoice.setFont(Theme.FONT_BODY);
        roleChoice.add("NGO / Orphanage / Relief Org (Food Claimer)");
        roleChoice.add("Restaurant / Hotel / Caterer (Food Publisher)");
        roleChoice.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateLocationVisibility();
            }
        });

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(roleLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(roleChoice, gbc);
        row++;

        // Display Name (Register mode only)
        nameLabel = new Label("Organization / Entity Name:");
        nameLabel.setFont(Theme.FONT_BODY_BOLD);
        nameLabel.setForeground(Theme.TEXT_DARK);
        nameField = new TextField(25);
        nameField.setFont(Theme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(nameField, gbc);
        row++;

        // Phone Number (Register mode only)
        phoneLabel = new Label("Contact Phone Number:");
        phoneLabel.setFont(Theme.FONT_BODY_BOLD);
        phoneLabel.setForeground(Theme.TEXT_DARK);
        phoneField = new TextField(25);
        phoneField.setFont(Theme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(phoneLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(phoneField, gbc);
        row++;

        // Username
        usernameLabel = new Label("Username:");
        usernameLabel.setFont(Theme.FONT_BODY_BOLD);
        usernameLabel.setForeground(Theme.TEXT_DARK);
        usernameField = new TextField(25);
        usernameField.setFont(Theme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(usernameField, gbc);
        row++;

        // Password
        passwordLabel = new Label("Password:");
        passwordLabel.setFont(Theme.FONT_BODY_BOLD);
        passwordLabel.setForeground(Theme.TEXT_DARK);
        passwordField = new TextField(25);
        passwordField.setFont(Theme.FONT_BODY);
        passwordField.setEchoChar('*'); // Masked password field

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(passwordField, gbc);
        row++;

        // Location Choice (NGO base location in Register mode)
        locationLabel = new Label("NGO Base Location:");
        locationLabel.setFont(Theme.FONT_BODY_BOLD);
        locationLabel.setForeground(Theme.TEXT_DARK);
        locationChoice = new Choice();
        locationChoice.setFont(Theme.FONT_BODY);
        for (Location loc : locationsList) {
            locationChoice.add(loc.getLocationName() + " (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
        }

        locationHelpLabel = new Label("*(Required for NGO distance calculation. Restaurants select pickup per listing)");
        locationHelpLabel.setFont(Theme.FONT_SMALL);
        locationHelpLabel.setForeground(Theme.TEXT_MUTED);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formBody.add(locationLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(locationChoice, gbc);
        row++;

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.7;
        formBody.add(locationHelpLabel, gbc);
        row++;

        // Action Button (Login / Register)
        actionButton = new Button("Sign In");
        Theme.stylePrimaryButton(actionButton);
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSubmit();
            }
        });

        // Toggle Login / Register Button
        toggleModeButton = new Button("New user? Create an account");
        Theme.styleSecondaryButton(toggleModeButton);
        toggleModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMode();
            }
        });

        Panel actionsPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        actionsPanel.setBackground(Theme.BG_WHITE);
        actionsPanel.add(actionButton);
        actionsPanel.add(toggleModeButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formBody.add(actionsPanel, gbc);
        row++;

        card.add(formBody, BorderLayout.CENTER);

        // 3. Quick Demo Buttons for Evaluator Ease
        Panel demoPanel = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        demoPanel.setBackground(Theme.BG_SIDEBAR);

        Label demoLabel = new Label("Evaluator Quick Login:");
        demoLabel.setFont(Theme.FONT_SMALL_BOLD);
        demoLabel.setForeground(Theme.TEXT_MUTED);
        demoPanel.add(demoLabel);

        Button quickRestBtn = new Button("Demo Restaurant");
        Theme.styleSecondaryButton(quickRestBtn);
        quickRestBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                usernameField.setText("demo_restaurant_1");
                passwordField.setText("Password@123");
                if (isRegisterMode) toggleMode();
                handleSubmit();
            }
        });
        demoPanel.add(quickRestBtn);

        Button quickNgoBtn = new Button("Demo NGO");
        Theme.styleSecondaryButton(quickNgoBtn);
        quickNgoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                usernameField.setText("demo_ngo");
                passwordField.setText("Password@123");
                if (isRegisterMode) toggleMode();
                handleSubmit();
            }
        });
        demoPanel.add(quickNgoBtn);

        card.add(demoPanel, BorderLayout.SOUTH);

        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);

        // Apply initial Login Mode visibility
        applyModeVisibility();
    }

    private void loadLocations() {
        try {
            locationsList.clear();
            locationsList.addAll(locationDAO.getAllLocations());
        } catch (SQLException e) {
            System.err.println("[GUI WARN] Could not load locations: " + e.getMessage());
        }
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        applyModeVisibility();
    }

    private void applyModeVisibility() {
        if (isRegisterMode) {
            formTitleLabel.setText("Register New Account (FoodBridge)");
            roleLabel.setVisible(true);
            roleChoice.setVisible(true);
            nameLabel.setVisible(true);
            nameField.setVisible(true);
            phoneLabel.setVisible(true);
            phoneField.setVisible(true);
            actionButton.setLabel("Create Account");
            toggleModeButton.setLabel("Already registered? Sign In");
            updateLocationVisibility();
        } else {
            formTitleLabel.setText("Welcome to FoodBridge — Sign In");
            roleLabel.setVisible(false);
            roleChoice.setVisible(false);
            nameLabel.setVisible(false);
            nameField.setVisible(false);
            phoneLabel.setVisible(false);
            phoneField.setVisible(false);
            locationLabel.setVisible(false);
            locationChoice.setVisible(false);
            locationHelpLabel.setVisible(false);
            actionButton.setLabel("Sign In");
            toggleModeButton.setLabel("New user? Create an account");
        }
        validate();
        repaint();
    }

    private void updateLocationVisibility() {
        if (!isRegisterMode) {
            locationLabel.setVisible(false);
            locationChoice.setVisible(false);
            locationHelpLabel.setVisible(false);
            return;
        }
        boolean isNgo = roleChoice.getSelectedIndex() == 0;
        locationLabel.setVisible(isNgo);
        locationChoice.setVisible(isNgo);
        locationHelpLabel.setVisible(isNgo);
        validate();
    }

    private void handleSubmit() {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText().trim();

        if (username.isEmpty()) {
            DialogHelper.showError(mainFrame, "Validation Error", "Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            DialogHelper.showError(mainFrame, "Validation Error", "Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        if (isRegisterMode) {
            final String name = nameField.getText().trim();
            final String phone = phoneField.getText().trim();
            final Role role = (roleChoice.getSelectedIndex() == 0) ? Role.NGO : Role.RESTAURANT;

            if (name.isEmpty()) {
                DialogHelper.showError(mainFrame, "Validation Error", "Please enter organization/entity name.");
                nameField.requestFocus();
                return;
            }
            if (phone.isEmpty()) {
                DialogHelper.showError(mainFrame, "Validation Error", "Please enter a valid phone number.");
                phoneField.requestFocus();
                return;
            }

            Integer locationId = null;
            if (role == Role.NGO) {
                int locIdx = locationChoice.getSelectedIndex();
                if (locIdx >= 0 && locIdx < locationsList.size()) {
                    locationId = locationsList.get(locIdx).getLocationId();
                } else {
                    DialogHelper.showError(mainFrame, "Validation Error", "Please select a valid base location.");
                    return;
                }
            }

            final Integer finalLocId = locationId;
            mainFrame.setStatusMessage("Registering account for '" + username + "'...", false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final User newUser = authService.register(username, password, role, name, phone, finalLocId);
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.showInfo(
                                    mainFrame,
                                    "Registration Successful",
                                    "Account registered successfully!\nWelcome to FoodBridge, " + newUser.getName() + "!"
                                );
                                onUserAuthenticated(newUser);
                            }
                        });
                    } catch (final Exception ex) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.showError(mainFrame, "Registration Failed", ex.getMessage());
                                mainFrame.setStatusMessage("Registration failed: " + ex.getMessage(), true);
                            }
                        });
                    }
                }
            }).start();

        } else {
            // Login Mode
            mainFrame.setStatusMessage("Authenticating '" + username + "'...", false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final User user = authService.login(username, password);
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (user != null) {
                                    onUserAuthenticated(user);
                                } else {
                                    DialogHelper.showError(
                                        mainFrame,
                                        "Authentication Failed",
                                        "Invalid username or password.\nPlease check your credentials and try again."
                                    );
                                    mainFrame.setStatusMessage("Invalid credentials.", true);
                                }
                            }
                        });
                    } catch (final Exception ex) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.showError(mainFrame, "Login Error", ex.getMessage());
                                mainFrame.setStatusMessage("Login error: " + ex.getMessage(), true);
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void onUserAuthenticated(User user) {
        usernameField.setText("");
        passwordField.setText("");
        nameField.setText("");
        phoneField.setText("");

        mainFrame.setActiveUser(user);

        if (user.getRole() == Role.RESTAURANT) {
            mainFrame.showScreen("RESTAURANT");
        } else if (user.getRole() == Role.NGO) {
            mainFrame.showScreen("NGO");
        }
    }
}
