package com.foodbridge.gui;

import com.foodbridge.dao.ActivityLogDAO;
import com.foodbridge.dao.LocationDAO;
import com.foodbridge.gui.components.AwtTablePanel;
import com.foodbridge.gui.components.DialogHelper;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodType;
import com.foodbridge.model.ListingStatus;
import com.foodbridge.model.Location;
import com.foodbridge.model.QuantityType;
import com.foodbridge.model.RestaurantListingDTO;
import com.foodbridge.model.User;
import com.foodbridge.service.FoodListingService;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Restaurant Dashboard Screen using pure java.awt.
 * Provides:
 * 1. Form to post surplus food listings with VEG/NON_VEG checkbox group,
 *    quantity units, 50-location pickup selector, and deadline picker.
 * 2. Real-time table of "My Listings" with status highlighting (AVAILABLE, CLAIMED, EXPIRED)
 *    and claimed NGO contact information.
 * 3. Cancel/Delete action for active available listings.
 */
public class RestaurantPanel extends Panel {

    private final MainFrame mainFrame;
    private final FoodListingService listingService;
    private final LocationDAO locationDAO;
    private final ActivityLogDAO activityLogDAO;
    private final List<Location> locationsList = new ArrayList<>();

    private final CardLayout contentCardLayout;
    private final Panel contentContainer;

    // Sidebar buttons
    private final Button btnPostView;
    private final Button btnListView;
    private final Button btnRefresh;

    // Post Listing Form fields
    private final TextField foodNameField;
    private final CheckboxGroup foodTypeGroup;
    private final Checkbox vegCheckbox;
    private final Checkbox nonVegCheckbox;
    private final Choice categoryChoice;
    private final Choice quantityTypeChoice;
    private final TextField quantityValueField;
    private final Choice pickupLocationChoice;
    private final Choice bestBeforeChoice;

    // My Listings Table
    private final AwtTablePanel listingsTable;
    private final Label selectionDetailLabel;
    private RestaurantListingDTO selectedListingDTO;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    public RestaurantPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.listingService = new FoodListingService();
        this.locationDAO = new LocationDAO();
        this.activityLogDAO = new ActivityLogDAO();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_LIGHT);

        loadLocations();

        // 1. Left Sidebar
        Panel sidebar = new Panel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 500));

        Panel navList = new Panel(new GridLayout(6, 1, 6, 8));
        navList.setBackground(Theme.BG_SIDEBAR);

        Label sidebarTitle = new Label("  RESTAURANT PORTAL");
        sidebarTitle.setFont(Theme.FONT_SMALL_BOLD);
        sidebarTitle.setForeground(Theme.TEXT_MUTED);
        navList.add(sidebarTitle);

        btnPostView = new Button("  Post New Food");
        Theme.stylePrimaryButton(btnPostView);
        btnPostView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchView("POST");
            }
        });
        navList.add(btnPostView);

        btnListView = new Button("  My Food Listings");
        Theme.styleSecondaryButton(btnListView);
        btnListView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchView("LIST");
                loadRestaurantListings();
            }
        });
        navList.add(btnListView);

        btnRefresh = new Button("  Refresh Data");
        Theme.styleSecondaryButton(btnRefresh);
        btnRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadRestaurantListings();
            }
        });
        navList.add(btnRefresh);

        Panel sidebarWrapper = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        sidebarWrapper.setBackground(Theme.BG_SIDEBAR);
        sidebarWrapper.add(navList);
        sidebar.add(sidebarWrapper, BorderLayout.NORTH);

        add(sidebar, BorderLayout.WEST);

        // 2. Right Content Area (CardLayout)
        contentCardLayout = new CardLayout();
        contentContainer = new Panel(contentCardLayout);
        contentContainer.setBackground(Theme.BG_LIGHT);

        // --- View 1: Post Listing Form ---
        Panel postCard = new Panel(new BorderLayout(10, 10));
        postCard.setBackground(Theme.BG_LIGHT);

        Panel postHeader = new Panel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        postHeader.setBackground(Theme.BG_WHITE);
        Label postTitle = new Label("Publish Excess Food Listing");
        postTitle.setFont(Theme.FONT_SUBTITLE);
        postTitle.setForeground(Theme.PRIMARY_DARK);
        postHeader.add(postTitle);
        postCard.add(postHeader, BorderLayout.NORTH);

        Panel formCard = new Panel(new GridBagLayout());
        formCard.setBackground(Theme.BG_WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Food Name
        Label nameLbl = new Label("Food Item Name:");
        nameLbl.setFont(Theme.FONT_BODY_BOLD);
        foodNameField = new TextField(30);
        foodNameField.setFont(Theme.FONT_BODY);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(nameLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(foodNameField, gbc);
        row++;

        // Food Type (VEG / NON_VEG CheckboxGroup)
        Label typeLbl = new Label("Food Type:");
        typeLbl.setFont(Theme.FONT_BODY_BOLD);
        foodTypeGroup = new CheckboxGroup();
        vegCheckbox = new Checkbox("Vegetarian (VEG)", foodTypeGroup, true);
        vegCheckbox.setFont(Theme.FONT_BODY_BOLD);
        vegCheckbox.setForeground(Theme.STATUS_AVAILABLE);
        nonVegCheckbox = new Checkbox("Non-Vegetarian (NON_VEG)", foodTypeGroup, false);
        nonVegCheckbox.setFont(Theme.FONT_BODY_BOLD);
        nonVegCheckbox.setForeground(Theme.STATUS_EXPIRED);

        Panel typePanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        typePanel.setBackground(Theme.BG_WHITE);
        typePanel.add(vegCheckbox);
        typePanel.add(nonVegCheckbox);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(typeLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(typePanel, gbc);
        row++;

        // Category
        Label catLbl = new Label("Food Category:");
        catLbl.setFont(Theme.FONT_BODY_BOLD);
        categoryChoice = new Choice();
        categoryChoice.setFont(Theme.FONT_BODY);
        categoryChoice.add("Cooked Meal");
        categoryChoice.add("Breakfast & Tiffin");
        categoryChoice.add("Bakery & Breads");
        categoryChoice.add("Full Buffet Excess");
        categoryChoice.add("Beverages & Juices");
        categoryChoice.add("Snacks & Sweets");

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(catLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(categoryChoice, gbc);
        row++;

        // Quantity Type & Value
        Label qtyLbl = new Label("Quantity Available:");
        qtyLbl.setFont(Theme.FONT_BODY_BOLD);

        Panel qtyPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        qtyPanel.setBackground(Theme.BG_WHITE);

        quantityValueField = new TextField("30", 8);
        quantityValueField.setFont(Theme.FONT_BODY);

        quantityTypeChoice = new Choice();
        quantityTypeChoice.setFont(Theme.FONT_BODY);
        quantityTypeChoice.add("BOXES (Packaged containers)");
        quantityTypeChoice.add("SERVES_PEOPLE (Individual meals)");

        qtyPanel.add(quantityValueField);
        qtyPanel.add(quantityTypeChoice);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(qtyLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(qtyPanel, gbc);
        row++;

        // Pickup Location (from 50 localities)
        Label locLbl = new Label("Pickup Locality:");
        locLbl.setFont(Theme.FONT_BODY_BOLD);
        pickupLocationChoice = new Choice();
        pickupLocationChoice.setFont(Theme.FONT_BODY);
        for (Location loc : locationsList) {
            pickupLocationChoice.add(loc.getLocationName() + " (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
        }

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(locLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(pickupLocationChoice, gbc);
        row++;

        // Best Before Deadline
        Label deadlineLbl = new Label("Pickup Deadline:");
        deadlineLbl.setFont(Theme.FONT_BODY_BOLD);
        bestBeforeChoice = new Choice();
        bestBeforeChoice.setFont(Theme.FONT_BODY);
        bestBeforeChoice.add("+2 Hours from now (Immediate Pickup)");
        bestBeforeChoice.add("+4 Hours from now (Standard Lunch/Dinner)");
        bestBeforeChoice.add("+6 Hours from now");
        bestBeforeChoice.add("+8 Hours from now");
        bestBeforeChoice.add("+12 Hours from now");
        bestBeforeChoice.add("+24 Hours from now");

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formCard.add(deadlineLbl, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(bestBeforeChoice, gbc);
        row++;

        // Submit Button
        Button submitListingBtn = new Button("Publish Food Listing to NGOs");
        Theme.stylePrimaryButton(submitListingBtn);
        submitListingBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePostListing();
            }
        });

        Panel submitPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 15));
        submitPanel.setBackground(Theme.BG_WHITE);
        submitPanel.add(submitListingBtn);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        formCard.add(submitPanel, gbc);

        postCard.add(formCard, BorderLayout.CENTER);
        contentContainer.add(postCard, "POST");

        // --- View 2: My Listings Table ---
        Panel listCard = new Panel(new BorderLayout(10, 10));
        listCard.setBackground(Theme.BG_LIGHT);

        Panel listHeader = new Panel(new BorderLayout());
        listHeader.setBackground(Theme.BG_WHITE);

        Label listTitle = new Label("  My Food Listings & Real-Time Pickup Status");
        listTitle.setFont(Theme.FONT_SUBTITLE);
        listTitle.setForeground(Theme.PRIMARY_DARK);
        listHeader.add(listTitle, BorderLayout.WEST);

        Panel listActions = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        listActions.setBackground(Theme.BG_WHITE);

        Button cancelListingBtn = new Button("Cancel Selected Listing");
        Theme.styleDangerButton(cancelListingBtn);
        cancelListingBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCancelSelectedListing();
            }
        });
        listActions.add(cancelListingBtn);
        listHeader.add(listActions, BorderLayout.EAST);

        listCard.add(listHeader, BorderLayout.NORTH);

        // Table
        String[] columns = new String[]{"ID", "Food Name", "Type", "Category", "Quantity", "Pickup Site", "Best Before", "Status", "Claimed By"};
        listingsTable = new AwtTablePanel(columns);
        listingsTable.setSelectionListener(new AwtTablePanel.RowSelectionListener() {
            @Override
            public void onRowSelected(int rowIndex, Object rowData) {
                if (rowData instanceof RestaurantListingDTO) {
                    selectedListingDTO = (RestaurantListingDTO) rowData;
                    updateSelectionDetail();
                }
            }
        });
        listCard.add(listingsTable, BorderLayout.CENTER);

        // Bottom selection detail panel
        Panel detailPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        detailPanel.setBackground(Theme.BG_WHITE);
        selectionDetailLabel = new Label("Select a listing row above to view full details.");
        selectionDetailLabel.setFont(Theme.FONT_SMALL);
        selectionDetailLabel.setForeground(Theme.TEXT_MUTED);
        detailPanel.add(selectionDetailLabel);
        listCard.add(detailPanel, BorderLayout.SOUTH);

        contentContainer.add(listCard, "LIST");

        add(contentContainer, BorderLayout.CENTER);
    }

    private void switchView(String viewName) {
        contentCardLayout.show(contentContainer, viewName);
        if ("POST".equals(viewName)) {
            Theme.stylePrimaryButton(btnPostView);
            Theme.styleSecondaryButton(btnListView);
        } else {
            Theme.styleSecondaryButton(btnPostView);
            Theme.stylePrimaryButton(btnListView);
        }
        contentContainer.validate();
    }

    private void loadLocations() {
        try {
            locationsList.clear();
            locationsList.addAll(locationDAO.getAllLocations());
        } catch (SQLException e) {
            System.err.println("[GUI WARN] RestaurantPanel could not load locations: " + e.getMessage());
        }
    }

    public void loadData() {
        switchView("LIST");
        loadRestaurantListings();
    }

    private void loadRestaurantListings() {
        final User user = mainFrame.getActiveUser();
        if (user == null) return;

        mainFrame.setStatusMessage("Refreshing listings for " + user.getName() + "...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<RestaurantListingDTO> list = listingService.getRestaurantListings(user.getUserId());
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            renderListings(list);
                            mainFrame.setStatusMessage("Loaded " + list.size() + " listings.", false);
                        }
                    });
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.setStatusMessage("Error loading listings: " + ex.getMessage(), true);
                        }
                    });
                }
            }
        }).start();
    }

    private void renderListings(List<RestaurantListingDTO> list) {
        listingsTable.clearRows();
        selectedListingDTO = null;
        selectionDetailLabel.setText("Showing " + list.size() + " listings. Click a row to view NGO claimer details.");

        for (RestaurantListingDTO dto : list) {
            FoodListing l = dto.getListing();
            String idStr = "#" + l.getListingId();
            String nameStr = l.getFoodName();
            String typeStr = l.getFoodType().name();
            String catStr = l.getCategory();
            String qtyStr = l.getQuantityValue() + " " + l.getQuantityType().name();
            String locStr = dto.getLocationName();
            String bbStr = (l.getBestBefore() != null) ? l.getBestBefore().format(TIME_FMT) : "N/A";
            String statusStr = l.getStatus().name();
            String claimerStr = (dto.getClaimedNgoName() != null) ? dto.getClaimedNgoName() : "-";

            String[] rowVals = new String[]{idStr, nameStr, typeStr, catStr, qtyStr, locStr, bbStr, statusStr, claimerStr};
            listingsTable.addRow(rowVals, dto);
        }
    }

    private void updateSelectionDetail() {
        if (selectedListingDTO == null) return;
        FoodListing l = selectedListingDTO.getListing();
        StringBuilder sb = new StringBuilder();
        sb.append("Selected: #").append(l.getListingId()).append(" ").append(l.getFoodName())
          .append(" | Status: ").append(l.getStatus().name());

        if (selectedListingDTO.getClaimedNgoName() != null) {
            sb.append(" | Claimed by NGO: ").append(selectedListingDTO.getClaimedNgoName())
              .append(" (Phone: ").append(selectedListingDTO.getClaimedNgoPhone()).append(")")
              .append(" at ").append(selectedListingDTO.getClaimedTime() != null ? selectedListingDTO.getClaimedTime().format(TIME_FMT) : "");
        }
        selectionDetailLabel.setText(sb.toString());
        selectionDetailLabel.setForeground(Theme.TEXT_DARK);
    }

    private void handlePostListing() {
        final User user = mainFrame.getActiveUser();
        if (user == null) return;

        final String foodName = foodNameField.getText().trim();
        if (foodName.isEmpty()) {
            DialogHelper.showError(mainFrame, "Validation Error", "Food name cannot be empty.");
            foodNameField.requestFocus();
            return;
        }

        final FoodType foodType = vegCheckbox.getState() ? FoodType.VEG : FoodType.NON_VEG;
        final String category = categoryChoice.getSelectedItem();

        int qty;
        try {
            qty = Integer.parseInt(quantityValueField.getText().trim());
            if (qty <= 0) {
                DialogHelper.showError(mainFrame, "Validation Error", "Quantity must be greater than zero.");
                quantityValueField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            DialogHelper.showError(mainFrame, "Validation Error", "Please enter a valid whole number for quantity.");
            quantityValueField.requestFocus();
            return;
        }
        final int finalQty = qty;

        final QuantityType quantityType = (quantityTypeChoice.getSelectedIndex() == 0)
            ? QuantityType.BOXES : QuantityType.SERVES_PEOPLE;

        int locIdx = pickupLocationChoice.getSelectedIndex();
        if (locIdx < 0 || locIdx >= locationsList.size()) {
            DialogHelper.showError(mainFrame, "Validation Error", "Please select a pickup locality.");
            return;
        }
        final int locationId = locationsList.get(locIdx).getLocationId();
        final String locationName = locationsList.get(locIdx).getLocationName();

        // Calculate deadline
        int hoursToAdd = 4;
        switch (bestBeforeChoice.getSelectedIndex()) {
            case 0: hoursToAdd = 2; break;
            case 1: hoursToAdd = 4; break;
            case 2: hoursToAdd = 6; break;
            case 3: hoursToAdd = 8; break;
            case 4: hoursToAdd = 12; break;
            case 5: hoursToAdd = 24; break;
        }
        final LocalDateTime bestBefore = LocalDateTime.now().plusHours(hoursToAdd);

        mainFrame.setStatusMessage("Publishing food listing...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FoodListing created = listingService.createListing(
                        user.getUserId(), foodName, foodType, category, quantityType, finalQty, locationId, bestBefore
                    );

                    // Audit log
                    try {
                        activityLogDAO.logActivity(user.getUserId(), "POST_LISTING", "Posted listing #" + created.getListingId() + " (" + foodName + ") in " + locationName);
                    } catch (Exception ignored) {}

                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showInfo(
                                mainFrame,
                                "Listing Published",
                                "Your excess food listing has been published successfully!\nLocal NGOs within range can now discover and claim it."
                            );
                            foodNameField.setText("");
                            quantityValueField.setText("30");
                            switchView("LIST");
                            loadRestaurantListings();
                        }
                    });
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showError(mainFrame, "Posting Error", ex.getMessage());
                            mainFrame.setStatusMessage("Failed to post listing: " + ex.getMessage(), true);
                        }
                    });
                }
            }
        }).start();
    }

    private void handleCancelSelectedListing() {
        final User user = mainFrame.getActiveUser();
        if (user == null || selectedListingDTO == null) {
            DialogHelper.showError(mainFrame, "Selection Required", "Please click on a listing in the table first.");
            return;
        }

        final FoodListing l = selectedListingDTO.getListing();
        if (l.getStatus() != ListingStatus.AVAILABLE) {
            DialogHelper.showError(
                mainFrame,
                "Cannot Cancel",
                "Listing #" + l.getListingId() + " cannot be cancelled because its status is already " + l.getStatus() + "."
            );
            return;
        }

        boolean confirm = DialogHelper.showConfirm(
            mainFrame,
            "Confirm Cancellation",
            "Are you sure you want to cancel listing #" + l.getListingId() + " (" + l.getFoodName() + ")?"
        );
        if (!confirm) return;

        mainFrame.setStatusMessage("Cancelling listing #" + l.getListingId() + "...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = listingService.cancelListing(l.getListingId(), user.getUserId());
                    if (success) {
                        try {
                            activityLogDAO.logActivity(user.getUserId(), "CANCEL_LISTING", "Cancelled listing #" + l.getListingId());
                        } catch (Exception ignored) {}

                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                DialogHelper.showInfo(mainFrame, "Listing Cancelled", "Listing #" + l.getListingId() + " was cancelled.");
                                loadRestaurantListings();
                            }
                        });
                    }
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showError(mainFrame, "Cancellation Error", ex.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
}
