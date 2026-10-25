package com.foodbridge.gui;

import com.foodbridge.dao.ActivityLogDAO;
import com.foodbridge.dao.ClaimDAO;
import com.foodbridge.dao.LocationDAO;
import com.foodbridge.gui.components.AwtTablePanel;
import com.foodbridge.gui.components.DialogHelper;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Location;
import com.foodbridge.model.NearbyListingDTO;
import com.foodbridge.model.NgoClaimHistoryDTO;
import com.foodbridge.model.User;
import com.foodbridge.service.ClaimService;
import com.foodbridge.service.MatchingService;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NGO / Food Claimer Dashboard screen using pure java.awt.
 * Features:
 * 1. "Nearby Listings" view sorted nearest-first via Haversine distance.
 * 2. Highlighted [IN RANGE] indicator for listings within 10 km.
 * 3. Atomic race-condition safe Claim action with modal confirmation dialog.
 * 4. "My Claims History" view with restaurant pickup contact details.
 */
public class NgoPanel extends Panel {

    private final MainFrame mainFrame;
    private final MatchingService matchingService;
    private final ClaimService claimService;
    private final LocationDAO locationDAO;
    private final ActivityLogDAO activityLogDAO;

    private final CardLayout contentCardLayout;
    private final Panel contentContainer;

    // Sidebar navigation buttons
    private final Button btnNearbyView;
    private final Button btnClaimsView;
    private final Button btnRefresh;

    // Nearby Listings view components
    private final Label ngoBaseLocationLabel;
    private final AwtTablePanel nearbyTable;
    private final Label selectedNearbyDetailLabel;
    private NearbyListingDTO selectedNearbyListing;

    // My Claims view components
    private final AwtTablePanel claimsTable;
    private final Label selectedClaimDetailLabel;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    public NgoPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.matchingService = new MatchingService();
        this.claimService = new ClaimService();
        this.locationDAO = new LocationDAO();
        this.activityLogDAO = new ActivityLogDAO();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_LIGHT);

        // 1. Left Sidebar
        Panel sidebar = new Panel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 500));

        Panel navList = new Panel(new GridLayout(6, 1, 6, 8));
        navList.setBackground(Theme.BG_SIDEBAR);

        Label sidebarTitle = new Label("  NGO / CLAIMER PORTAL");
        sidebarTitle.setFont(Theme.FONT_SMALL_BOLD);
        sidebarTitle.setForeground(Theme.TEXT_MUTED);
        navList.add(sidebarTitle);

        btnNearbyView = new Button("  Nearby Food Listings");
        Theme.stylePrimaryButton(btnNearbyView);
        btnNearbyView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchView("NEARBY");
                loadNearbyListings();
            }
        });
        navList.add(btnNearbyView);

        btnClaimsView = new Button("  My Claimed Pickups");
        Theme.styleSecondaryButton(btnClaimsView);
        btnClaimsView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchView("CLAIMS");
                loadClaimHistory();
            }
        });
        navList.add(btnClaimsView);

        btnRefresh = new Button("  Refresh Listings");
        Theme.styleSecondaryButton(btnRefresh);
        btnRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadNearbyListings();
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

        // --- View 1: Nearby Listings ---
        Panel nearbyCard = new Panel(new BorderLayout(10, 10));
        nearbyCard.setBackground(Theme.BG_LIGHT);

        // Top info bar
        Panel topBar = new Panel(new BorderLayout());
        topBar.setBackground(Theme.BG_WHITE);

        ngoBaseLocationLabel = new Label("  NGO Base Location: Loading...");
        ngoBaseLocationLabel.setFont(Theme.FONT_BODY_BOLD);
        ngoBaseLocationLabel.setForeground(Theme.PRIMARY_DARK);
        topBar.add(ngoBaseLocationLabel, BorderLayout.WEST);

        Panel topActions = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        topActions.setBackground(Theme.BG_WHITE);

        Button claimBtn = new Button("Claim Selected Food");
        Theme.stylePrimaryButton(claimBtn);
        claimBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleClaimSelected();
            }
        });
        topActions.add(claimBtn);

        topBar.add(topActions, BorderLayout.EAST);
        nearbyCard.add(topBar, BorderLayout.NORTH);

        // Nearby Listings Table
        String[] columns = new String[]{"ID", "Food Item", "Type", "Quantity", "Pickup Site", "Distance", "Range Status", "Best Before", "Restaurant Name"};
        nearbyTable = new AwtTablePanel(columns);
        nearbyTable.setSelectionListener(new AwtTablePanel.RowSelectionListener() {
            @Override
            public void onRowSelected(int rowIndex, Object rowData) {
                if (rowData instanceof NearbyListingDTO) {
                    selectedNearbyListing = (NearbyListingDTO) rowData;
                    updateNearbySelectionDetail();
                }
            }
        });
        nearbyCard.add(nearbyTable, BorderLayout.CENTER);

        // Selection details panel at bottom
        Panel detailPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        detailPanel.setBackground(Theme.BG_WHITE);
        selectedNearbyDetailLabel = new Label("Select a food listing to view distance and claim details.");
        selectedNearbyDetailLabel.setFont(Theme.FONT_SMALL);
        selectedNearbyDetailLabel.setForeground(Theme.TEXT_MUTED);
        detailPanel.add(selectedNearbyDetailLabel);
        nearbyCard.add(detailPanel, BorderLayout.SOUTH);

        contentContainer.add(nearbyCard, "NEARBY");

        // --- View 2: My Claims History ---
        Panel claimsCard = new Panel(new BorderLayout(10, 10));
        claimsCard.setBackground(Theme.BG_LIGHT);

        Panel claimsHeader = new Panel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        claimsHeader.setBackground(Theme.BG_WHITE);
        Label claimsTitle = new Label("My Claimed Food Pickups & Restaurant Contacts");
        claimsTitle.setFont(Theme.FONT_SUBTITLE);
        claimsTitle.setForeground(Theme.PRIMARY_DARK);
        claimsHeader.add(claimsTitle);
        claimsCard.add(claimsHeader, BorderLayout.NORTH);

        String[] claimCols = new String[]{"Claim ID", "Food Item", "Type", "Quantity", "Pickup Site", "Claimed Time", "Restaurant Name", "Restaurant Phone"};
        claimsTable = new AwtTablePanel(claimCols);
        claimsTable.setSelectionListener(new AwtTablePanel.RowSelectionListener() {
            @Override
            public void onRowSelected(int rowIndex, Object rowData) {
                if (rowData instanceof NgoClaimHistoryDTO) {
                    NgoClaimHistoryDTO dto = (NgoClaimHistoryDTO) rowData;
                    selectedClaimDetailLabel.setText("Selected Claim #" + dto.getClaimId() + ": Call " + dto.getRestaurantName() + " at " + dto.getRestaurantPhone() + " for pickup at " + dto.getLocationName());
                }
            }
        });
        claimsCard.add(claimsTable, BorderLayout.CENTER);

        Panel claimDetailPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        claimDetailPanel.setBackground(Theme.BG_WHITE);
        selectedClaimDetailLabel = new Label("Pickups are reserved for your organization. Please coordinate collection promptly.");
        selectedClaimDetailLabel.setFont(Theme.FONT_SMALL);
        selectedClaimDetailLabel.setForeground(Theme.TEXT_MUTED);
        claimDetailPanel.add(selectedClaimDetailLabel);
        claimsCard.add(claimDetailPanel, BorderLayout.SOUTH);

        contentContainer.add(claimsCard, "CLAIMS");

        add(contentContainer, BorderLayout.CENTER);
    }

    private void switchView(String viewName) {
        contentCardLayout.show(contentContainer, viewName);
        if ("NEARBY".equals(viewName)) {
            Theme.stylePrimaryButton(btnNearbyView);
            Theme.styleSecondaryButton(btnClaimsView);
        } else {
            Theme.styleSecondaryButton(btnNearbyView);
            Theme.stylePrimaryButton(btnClaimsView);
        }
        contentContainer.validate();
    }

    public void loadData() {
        switchView("NEARBY");
        updateBaseLocationDisplay();
        loadNearbyListings();
    }

    private void updateBaseLocationDisplay() {
        User user = mainFrame.getActiveUser();
        if (user != null && user.getLocationId() != null) {
            try {
                Location loc = locationDAO.getLocationById(user.getLocationId());
                if (loc != null) {
                    ngoBaseLocationLabel.setText("  NGO Base Location: " + loc.getLocationName() + " (" + loc.getLatitude() + ", " + loc.getLongitude() + ") | Sorted Nearest-First");
                    return;
                }
            } catch (Exception ignored) {}
        }
        ngoBaseLocationLabel.setText("  NGO Base Location: Configured | Sorted Nearest-First");
    }

    private void loadNearbyListings() {
        final User user = mainFrame.getActiveUser();
        if (user == null || user.getLocationId() == null) return;

        mainFrame.setStatusMessage("Evaluating nearby listings with Haversine distance formula...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<NearbyListingDTO> nearbyList = matchingService.getNearbyListingsForNgo(user.getLocationId());
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            renderNearbyListings(nearbyList);
                            mainFrame.setStatusMessage("Found " + nearbyList.size() + " active listings sorted nearest-first.", false);
                        }
                    });
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.setStatusMessage("Error calculating nearby listings: " + ex.getMessage(), true);
                        }
                    });
                }
            }
        }).start();
    }

    private void renderNearbyListings(List<NearbyListingDTO> list) {
        nearbyTable.clearRows();
        selectedNearbyListing = null;
        selectedNearbyDetailLabel.setText("Showing " + list.size() + " listings. Listings within 10km are tagged [IN RANGE].");

        for (NearbyListingDTO dto : list) {
            FoodListing l = dto.getListing();
            String idStr = "#" + l.getListingId();
            String nameStr = l.getFoodName();
            String typeStr = l.getFoodType().name();
            String qtyStr = l.getQuantityValue() + " " + l.getQuantityType().name();
            String locStr = dto.getLocation().getLocationName();
            String distStr = dto.getDistanceKm() + " km";
            String rangeBadgeStr = dto.isInRange() ? "[IN RANGE]" : "Farther (>10km)";
            String bbStr = (l.getBestBefore() != null) ? l.getBestBefore().format(TIME_FMT) : "N/A";
            String restStr = dto.getRestaurantName();

            String[] rowVals = new String[]{idStr, nameStr, typeStr, qtyStr, locStr, distStr, rangeBadgeStr, bbStr, restStr};
            nearbyTable.addRow(rowVals, dto);
        }
    }

    private void updateNearbySelectionDetail() {
        if (selectedNearbyListing == null) return;
        FoodListing l = selectedNearbyListing.getListing();
        StringBuilder sb = new StringBuilder();
        sb.append("Selected: #").append(l.getListingId()).append(" ").append(l.getFoodName())
          .append(" | Distance: ").append(selectedNearbyListing.getDistanceKm()).append(" km")
          .append(selectedNearbyListing.isInRange() ? " [IN RANGE <= 10km]" : " [FARTHER > 10km]")
          .append(" | Publisher: ").append(selectedNearbyListing.getRestaurantName())
          .append(" (Phone: ").append(selectedNearbyListing.getRestaurantPhone()).append(")")
          .append(" at ").append(selectedNearbyListing.getLocation().getLocationName());

        selectedNearbyDetailLabel.setText(sb.toString());
        selectedNearbyDetailLabel.setForeground(Theme.TEXT_DARK);
    }

    private void handleClaimSelected() {
        final User user = mainFrame.getActiveUser();
        if (user == null || selectedNearbyListing == null) {
            DialogHelper.showError(mainFrame, "Selection Required", "Please click on a food listing in the table before clicking Claim.");
            return;
        }

        final FoodListing l = selectedNearbyListing.getListing();
        final NearbyListingDTO dto = selectedNearbyListing;

        String confirmMsg = "Do you want to claim this excess food listing?\n\n" +
            "Food Item: " + l.getFoodName() + " (" + l.getFoodType() + ")\n" +
            "Quantity: " + l.getQuantityValue() + " " + l.getQuantityType() + "\n" +
            "Pickup Site: " + dto.getLocation().getLocationName() + " (" + dto.getDistanceKm() + " km away)\n" +
            "Publisher: " + dto.getRestaurantName() + "\n\n" +
            "Claiming is an atomic transaction and reserves this listing for your organization.";

        boolean confirmed = DialogHelper.showConfirm(mainFrame, "Confirm Food Claim", confirmMsg);
        if (!confirmed) return;

        mainFrame.setStatusMessage("Executing atomic claim transaction...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ClaimDAO.ClaimResult result = claimService.claimListing(l.getListingId(), user.getUserId());

                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (result.isSuccess()) {
                                // Audit log
                                try {
                                    activityLogDAO.logActivity(user.getUserId(), "CLAIM_FOOD", "Claimed listing #" + l.getListingId() + " (" + result.getFoodName() + ")");
                                } catch (Exception ignored) {}

                                String successMsg = "Listing Claimed Successfully!\n\n" +
                                    "Food: " + result.getFoodName() + "\n" +
                                    "Restaurant: " + result.getRestaurantName() + "\n" +
                                    "Contact Phone: " + result.getRestaurantPhone() + "\n" +
                                    "Pickup Location: " + dto.getLocation().getLocationName() + "\n\n" +
                                    "Please call the restaurant to coordinate pickup timing.";

                                DialogHelper.showInfo(mainFrame, "Claim Confirmed", successMsg);

                                // Transition to My Claims view
                                switchView("CLAIMS");
                                loadClaimHistory();
                            } else {
                                DialogHelper.showError(mainFrame, "Claim Conflict", result.getMessage());
                                loadNearbyListings();
                            }
                        }
                    });
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showError(mainFrame, "Claiming Error", ex.getMessage());
                            mainFrame.setStatusMessage("Claim failed: " + ex.getMessage(), true);
                        }
                    });
                }
            }
        }).start();
    }

    private void loadClaimHistory() {
        final User user = mainFrame.getActiveUser();
        if (user == null) return;

        mainFrame.setStatusMessage("Loading pickup claim history...", false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<NgoClaimHistoryDTO> history = claimService.getNgoClaimHistory(user.getUserId());
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            renderClaimHistory(history);
                            mainFrame.setStatusMessage("Loaded " + history.size() + " claimed pickups.", false);
                        }
                    });
                } catch (final Exception ex) {
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.setStatusMessage("Error loading claim history: " + ex.getMessage(), true);
                        }
                    });
                }
            }
        }).start();
    }

    private void renderClaimHistory(List<NgoClaimHistoryDTO> list) {
        claimsTable.clearRows();
        selectedClaimDetailLabel.setText("Showing " + list.size() + " claimed pickups. Contact the restaurants below for pickup coordination.");

        for (NgoClaimHistoryDTO dto : list) {
            String claimIdStr = "#" + dto.getClaimId();
            String nameStr = dto.getFoodName();
            String typeStr = dto.getFoodType().name();
            String qtyStr = dto.getQuantityValue() + " " + dto.getQuantityType().name();
            String locStr = dto.getLocationName();
            String timeStr = (dto.getClaimedTime() != null) ? dto.getClaimedTime().format(TIME_FMT) : "N/A";
            String restNameStr = dto.getRestaurantName();
            String restPhoneStr = dto.getRestaurantPhone();

            String[] rowVals = new String[]{claimIdStr, nameStr, typeStr, qtyStr, locStr, timeStr, restNameStr, restPhoneStr};
            claimsTable.addRow(rowVals, dto);
        }
    }
}
