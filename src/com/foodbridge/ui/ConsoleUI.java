package com.foodbridge.ui;

import com.foodbridge.dao.ClaimDAO;
import com.foodbridge.dao.LocationDAO;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodType;
import com.foodbridge.model.ListingStatus;
import com.foodbridge.model.Location;
import com.foodbridge.model.NearbyListingDTO;
import com.foodbridge.model.NgoClaimHistoryDTO;
import com.foodbridge.model.QuantityType;
import com.foodbridge.model.RestaurantListingDTO;
import com.foodbridge.model.Role;
import com.foodbridge.model.User;
import com.foodbridge.service.AuthService;
import com.foodbridge.service.ClaimService;
import com.foodbridge.service.FoodListingService;
import com.foodbridge.service.MatchingService;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * ConsoleUI drives terminal menus, listing displays, and interactive workflows
 * with ANSI color enhancements (when supported), clean ASCII borders, and fixed-width tabular formatting.
 */
public class ConsoleUI {

    private final Scanner scanner;
    private final InputValidator input;
    private final AuthService authService;
    private final FoodListingService listingService;
    private final MatchingService matchingService;
    private final ClaimService claimService;
    private final LocationDAO locationDAO;

    private static final DateTimeFormatter DISPLAY_DT = DateTimeFormatter.ofPattern("dd-MMM HH:mm");

    // Detect whether terminal supports ANSI/VT escape sequences
    private static final boolean ANSI_SUPPORTED = detectAnsiSupport();

    // Terminal ANSI Escape Sequences (empty strings when ANSI is unsupported to avoid literal escape code leakage)
    public static final String RESET = ANSI_SUPPORTED ? "\u001B[0m" : "";
    public static final String BOLD = ANSI_SUPPORTED ? "\u001B[1m" : "";
    public static final String DIM = ANSI_SUPPORTED ? "\u001B[2m" : "";
    public static final String GREEN = ANSI_SUPPORTED ? "\u001B[32m" : "";
    public static final String BOLD_GREEN = ANSI_SUPPORTED ? "\u001B[1;32m" : "";
    public static final String CYAN = ANSI_SUPPORTED ? "\u001B[36m" : "";
    public static final String BOLD_CYAN = ANSI_SUPPORTED ? "\u001B[1;36m" : "";
    public static final String YELLOW = ANSI_SUPPORTED ? "\u001B[33m" : "";
    public static final String BOLD_YELLOW = ANSI_SUPPORTED ? "\u001B[1;33m" : "";
    public static final String RED = ANSI_SUPPORTED ? "\u001B[31m" : "";
    public static final String BOLD_RED = ANSI_SUPPORTED ? "\u001B[1;31m" : "";
    public static final String MAGENTA = ANSI_SUPPORTED ? "\u001B[35m" : "";
    public static final String BOLD_MAGENTA = ANSI_SUPPORTED ? "\u001B[1;35m" : "";
    public static final String BLUE = ANSI_SUPPORTED ? "\u001B[34m" : "";
    public static final String BOLD_BLUE = ANSI_SUPPORTED ? "\u001B[1;34m" : "";
    public static final String GRAY = ANSI_SUPPORTED ? "\u001B[90m" : "";

    private static boolean detectAnsiSupport() {
        // Explicit system property override (-Dfoodbridge.ansi=true/false)
        String prop = System.getProperty("foodbridge.ansi");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        // Environment variable override
        String env = System.getenv("FOODBRIDGE_ANSI");
        if (env != null) {
            return Boolean.parseBoolean(env);
        }
        // Windows Terminal (wt.exe) sets WT_SESSION
        if (System.getenv("WT_SESSION") != null) {
            return true;
        }
        // ConEmu / Cmder sets ConEmuANSI=ON
        if ("ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))) {
            return true;
        }
        // ANSICON sets ANSICON
        if (System.getenv("ANSICON") != null) {
            return true;
        }
        // Non-Windows systems with valid TERM
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            String term = System.getenv("TERM");
            return term != null && !term.equals("dumb");
        }
        // Standard Windows cmd.exe / PowerShell without WT_SESSION does not reliably support ANSI escapes
        return false;
    }

    public ConsoleUI() {
        // Ensure UTF-8 output where possible
        try {
            System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) {}

        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        this.input = new InputValidator(scanner);
        this.authService = new AuthService();
        this.listingService = new FoodListingService();
        this.matchingService = new MatchingService();
        this.claimService = new ClaimService();
        this.locationDAO = new LocationDAO();
    }

    /**
     * Entry point for the interactive console loop.
     */
    public void start() {
        boolean running = true;
        while (running) {
            printLaunchBanner();
            System.out.println(BOLD_CYAN + "+--------------------------------------------------------------+");
            System.out.println("|  " + BOLD + "MAIN MENU" + RESET + BOLD_CYAN + "                                                   |");
            System.out.println("+--------------------------------------------------------------+");
            System.out.println("|  " + RESET + "[1]  Login to Existing Account                             " + BOLD_CYAN + "|");
            System.out.println("|  " + RESET + "[2]  Register New Account (NGO or Restaurant)              " + BOLD_CYAN + "|");
            System.out.println("|  " + RESET + "[3]  Exit Application                                      " + BOLD_CYAN + "|");
            System.out.println("+--------------------------------------------------------------+" + RESET);

            int choice = input.readInt("Enter choice (1-3): ", 1, 3);
            switch (choice) {
                case 1:
                    transitionDivider("Opening Login Screen");
                    handleLogin();
                    break;
                case 2:
                    transitionDivider("Opening Registration Portal");
                    handleRegister();
                    break;
                case 3:
                    running = false;
                    System.out.println("\n" + BOLD_GREEN + "[OK] Thank you for using FoodBridge! Together we prevent food waste." + RESET);
                    break;
            }
        }
    }

    private void printLaunchBanner() {
        System.out.println();
        System.out.println(BOLD_CYAN + "+----------------------------------------------------------------------------------+");
        System.out.println("|                               " + BOLD + "F O O D B R I D G E" + RESET + BOLD_CYAN + "                                |");
        System.out.println("|             " + RESET + "Connecting Surplus Food to Local Communities * Zero Waste" + BOLD_CYAN + "            |");
        System.out.println("|        " + BOLD_YELLOW + "* 50 Regional Localities" + RESET + BOLD_CYAN + "  *  " + BOLD_GREEN + "* Real-Time Distance Matching" + RESET + BOLD_CYAN + "             |");
        System.out.println("+----------------------------------------------------------------------------------+" + RESET);
        System.out.println();
    }

    private void transitionDivider(String action) {
        System.out.println();
        System.out.println(GRAY + "-------------------------------------------------------------------------------" + RESET);
        if (action != null && !action.isEmpty()) {
            System.out.println(CYAN + "  -> " + action + "..." + RESET);
            System.out.println(GRAY + "-------------------------------------------------------------------------------" + RESET);
        }
        System.out.println();
    }

    private void printHeader(String title) {
        System.out.println("\n" + BOLD_CYAN + "=== " + title + " ===" + RESET);
    }

    // ==========================================================
    // AUTHENTICATION FLOWS
    // ==========================================================

    private void handleLogin() {
        printHeader("USER LOGIN");
        String username = input.readNonEmptyString("Username: ");
        String password = input.readNonEmptyString("Password: ");

        try {
            User user = authService.login(username, password);
            if (user == null) {
                System.out.println(BOLD_RED + "\n[X] Invalid username or password. Please try again." + RESET);
                input.pause();
                return;
            }

            String locDisplay = "Multi-Site Operations";
            if (user.getLocationId() != null && user.getLocationId() > 0) {
                Location userLoc = locationDAO.getLocationById(user.getLocationId());
                if (userLoc != null) {
                    locDisplay = userLoc.getLocationName();
                }
            }

            transitionDivider("Authenticating " + user.getName());
            System.out.println(BOLD_GREEN + "[OK] Login successful! Welcome back, " + user.getName() + "!" + RESET);
            System.out.println("  Role     : " + BOLD + user.getRole() + RESET);
            if (user.getRole() == Role.NGO) {
                System.out.println("  Base Loc : " + locDisplay);
            } else {
                System.out.println("  Status   : " + locDisplay + " (Pickup location selected dynamically per listing)");
            }
            input.pause();

            if (user.getRole() == Role.RESTAURANT) {
                runRestaurantMenu(user);
            } else if (user.getRole() == Role.NGO) {
                runNgoMenu(user, locDisplay);
            }

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "[X] Database error during login: " + e.getMessage() + RESET);
            input.pause();
        }
    }

    /**
     * Timing of location capture:
     * NGO users choose fixed base location at registration (stored in users.location_id).
     * RESTAURANT users do NOT choose location at registration (users.location_id remains NULL).
     */
    private void handleRegister() {
        printHeader("REGISTER NEW ACCOUNT");
        Role role = input.readRole("Select account role:");

        String rolePrompt = (role == Role.RESTAURANT) 
            ? "Restaurant / Hotel / Caterer Name: " 
            : "NGO / Orphanage / Relief Organization Name: ";
        String name = input.readNonEmptyString(rolePrompt);

        String username = input.readNonEmptyString("Choose Username: ");
        String password = input.readNonEmptyString("Choose Password: ");
        String phone = input.readNonEmptyString("Phone Number (for pickup coordination): ");

        Integer locationId = null;

        if (role == Role.NGO) {
            System.out.println("\n" + CYAN + "[NGO Registration] Please select your permanent base location." + RESET);
            System.out.println("  (This base location is used for Haversine distance matching to find nearest food)");
            int chosen = selectLocationFromMenu();
            if (chosen == -1) {
                System.out.println(BOLD_RED + "[X] Location selection was cancelled. Registration aborted." + RESET);
                input.pause();
                return;
            }
            locationId = chosen;
        } else {
            // Restaurant / Hotel / Caterer users do NOT set a fixed location at registration
            System.out.println("\n" + BOLD_YELLOW + "[Notice] Caterer / Restaurant:" + RESET);
            System.out.println("  You operate across various banquet halls, kitchens, and event sites.");
            System.out.println("  Registration location is omitted; you will select the pickup location");
            System.out.println("  dynamically for each specific food listing you post.");
        }

        try {
            User created = authService.register(username, password, role, name, phone, locationId);
            transitionDivider("Creating Account Record");
            System.out.println(BOLD_GREEN + "[OK] Account created successfully for '" + created.getUsername() + "' (" + created.getRole() + ")!" + RESET);
            System.out.println("  You may now log in from the main menu.");
        } catch (IllegalArgumentException e) {
            System.out.println(BOLD_YELLOW + "\n[X] Registration failed: " + e.getMessage() + RESET);
        } catch (SQLException e) {
            System.out.println(BOLD_RED + "\n[X] Database error during registration: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    /**
     * Renders a 2-column tabular view of the 50 predefined regional localities.
     */
    private int selectLocationFromMenu() {
        try {
            List<Location> locations = locationDAO.getAllLocations();
            if (locations.isEmpty()) {
                System.out.println(BOLD_RED + "[X] No locations found in database. Please run setup first." + RESET);
                return -1;
            }

            System.out.println();
            System.out.println(BOLD_CYAN + "+----------------------------------------------------------------------------------------+");
            System.out.println("|  " + BOLD + "PREDEFINED REGIONAL LOCALITIES (Select 1 to " + locations.size() + ")" + RESET + BOLD_CYAN + "                                         |");
            System.out.println("+------------------------------------------+---------------------------------------------+" + RESET);

            int half = (locations.size() + 1) / 2;
            for (int i = 0; i < half; i++) {
                Location loc1 = locations.get(i);
                String col1 = String.format("[%2d] %-22s (%5.2f, %5.2f)", 
                    loc1.getLocationId(), pad(loc1.getLocationName(), 22), loc1.getLatitude(), loc1.getLongitude());

                String col2 = "";
                if (i + half < locations.size()) {
                    Location loc2 = locations.get(i + half);
                    col2 = String.format("[%2d] %-22s (%5.2f, %5.2f)", 
                        loc2.getLocationId(), pad(loc2.getLocationName(), 22), loc2.getLatitude(), loc2.getLongitude());
                } else {
                    col2 = String.format("%-43s", " ");
                }
                System.out.println("| " + col1 + " | " + col2 + " |");
            }
            System.out.println(BOLD_CYAN + "+------------------------------------------+---------------------------------------------+" + RESET);

            return input.readInt("Enter Location Number (1-" + locations.size() + "): ", 1, locations.size());

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "[X] Failed to load locations: " + e.getMessage() + RESET);
            return -1;
        }
    }

    // ==========================================================
    // RESTAURANT / PUBLISHER MENU
    // ==========================================================

    private void runRestaurantMenu(User restaurant) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + BOLD_MAGENTA + "+--------------------------------------------------------------+");
            System.out.println("|  " + BOLD + "RESTAURANT PORTAL" + RESET + BOLD_MAGENTA + " | " + pad(restaurant.getName(), 38) + " |");
            System.out.println("|  " + DIM + "Role: Surplus Food Publisher (Multi-Site Operations)" + RESET + BOLD_MAGENTA + "        |");
            System.out.println("+--------------------------------------------------------------+");
            System.out.println("|  " + RESET + "[1]  Post New Surplus Food Listing                          " + BOLD_MAGENTA + "|");
            System.out.println("|  " + RESET + "[2]  View My Listings & Claimer Status                      " + BOLD_MAGENTA + "|");
            System.out.println("|  " + RESET + "[3]  Edit an Available Listing                              " + BOLD_MAGENTA + "|");
            System.out.println("|  " + RESET + "[4]  Cancel / Delete an Available Listing                   " + BOLD_MAGENTA + "|");
            System.out.println("|  " + RESET + "[5]  Logout to Main Menu                                    " + BOLD_MAGENTA + "|");
            System.out.println("+--------------------------------------------------------------+" + RESET);

            int choice = input.readInt("Enter choice (1-5): ", 1, 5);
            switch (choice) {
                case 1:
                    transitionDivider("Preparing Listing Form");
                    handlePostListing(restaurant);
                    break;
                case 2:
                    transitionDivider("Loading Published Listings");
                    handleViewRestaurantListings(restaurant);
                    break;
                case 3:
                    transitionDivider("Opening Listing Editor");
                    handleEditListing(restaurant);
                    break;
                case 4:
                    transitionDivider("Opening Listing Cancellation");
                    handleCancelListing(restaurant);
                    break;
                case 5:
                    inMenu = false;
                    transitionDivider("Logging Out");
                    System.out.println(BOLD_GREEN + "[OK] Successfully logged out from " + restaurant.getName() + "." + RESET);
                    break;
            }
        }
    }

    /**
     * When posting each listing, the restaurant selects the pickup location
     * from the 50-entry location menu. Stored in food_listings.location_id.
     */
    private void handlePostListing(User restaurant) {
        printHeader("POST NEW SURPLUS FOOD LISTING");
        String foodName = input.readNonEmptyString("Food Item Name (e.g. Sambar Rice & Veggies): ");
        FoodType foodType = input.readFoodType("Food Classification:");
        String category = input.readNonEmptyString("Category (e.g. Cooked Meal, Packaged, Bakery, Raw Produce): ");
        QuantityType quantityType = input.readQuantityType("Quantity Metric:");
        int quantityValue = input.readPositiveInt("Quantity Amount (" + quantityType.getDisplayName() + "): ");
        LocalDateTime bestBefore = input.readFutureDateTime("Pickup Deadline (Best-Before):");

        // Dynamic pickup location selection per listing
        System.out.println("\n" + CYAN + "[Pickup Site Selection] Select the pickup site for this specific surplus food batch:" + RESET);
        int pickupLocationId = selectLocationFromMenu();
        if (pickupLocationId == -1) {
            System.out.println(BOLD_RED + "[X] Food listing cancelled: Pickup location is required." + RESET);
            input.pause();
            return;
        }

        try {
            FoodListing created = listingService.createListing(
                restaurant.getUserId(),
                foodName,
                foodType,
                category,
                quantityType,
                quantityValue,
                pickupLocationId,
                bestBefore
            );

            Location loc = locationDAO.getLocationById(pickupLocationId);
            String locName = (loc != null) ? loc.getLocationName() : ("Location #" + pickupLocationId);

            transitionDivider("Publishing Food Listing");
            System.out.println(BOLD_GREEN + "[OK] Food listing successfully published with ID #" + created.getListingId() + "!" + RESET);
            System.out.println("  Food Item         : " + created.getFoodName() + " (" + created.getFoodType() + ")");
            System.out.println("  Quantity          : " + created.getQuantityValue() + " " + created.getQuantityType().getDisplayName());
            System.out.println("  Pickup Locality   : " + BOLD_CYAN + locName + RESET);
            System.out.println("  Pickup Deadline   : " + created.getBestBefore().format(DISPLAY_DT));
            System.out.println("  Status            : " + BOLD_GREEN + created.getStatus() + RESET);

        } catch (IllegalArgumentException e) {
            System.out.println(BOLD_YELLOW + "\n[X] Invalid listing parameters: " + e.getMessage() + RESET);
        } catch (SQLException e) {
            System.out.println(BOLD_RED + "\n[X] Database error posting listing: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    private void handleViewRestaurantListings(User restaurant) {
        printHeader("MY FOOD LISTINGS & CLAIM STATUS");
        try {
            List<RestaurantListingDTO> listings = listingService.getRestaurantListings(restaurant.getUserId());
            if (listings.isEmpty()) {
                System.out.println("\nYou have not posted any food listings yet.");
                input.pause();
                return;
            }

            ConsoleTable table = new ConsoleTable();
            table.addColumn("ID", 5, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Food Name", 26, ConsoleTable.Alignment.LEFT)
                 .addColumn("Type", 9, ConsoleTable.Alignment.CENTER)
                 .addColumn("Category", 14, ConsoleTable.Alignment.LEFT)
                 .addColumn("Quantity", 13, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Pickup Site", 16, ConsoleTable.Alignment.LEFT)
                 .addColumn("Best-Before", 14, ConsoleTable.Alignment.CENTER)
                 .addColumn("Status", 11, ConsoleTable.Alignment.CENTER)
                 .addColumn("Claim Details / Contact", 28, ConsoleTable.Alignment.LEFT);

            for (RestaurantListingDTO item : listings) {
                FoodListing fl = item.getListing();
                String qty = fl.getQuantityValue() + " " + (fl.getQuantityType() == QuantityType.BOXES ? "boxes" : "meals");

                String statusColor = BOLD_GREEN;
                if (fl.getStatus() == ListingStatus.CLAIMED) statusColor = BOLD_CYAN;
                if (fl.getStatus() == ListingStatus.EXPIRED) statusColor = BOLD_RED;

                String statusStr = statusColor + fl.getStatus().name() + RESET;
                String claimInfo = "-";
                if (fl.getStatus() == ListingStatus.CLAIMED && item.getClaimedNgoName() != null) {
                    claimInfo = item.getClaimedNgoName() + " (" + item.getClaimedNgoPhone() + ")";
                }

                String bestBeforeStr = fl.getBestBefore() != null ? fl.getBestBefore().format(DISPLAY_DT) : "-";

                table.addRow(
                    String.valueOf(fl.getListingId()),
                    fl.getFoodName(),
                    fl.getFoodType().name(),
                    fl.getCategory(),
                    qty,
                    item.getLocationName(),
                    bestBeforeStr,
                    statusStr,
                    claimInfo
                );
            }

            System.out.println();
            table.print();

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "[X] Database error retrieving listings: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    private void handleEditListing(User restaurant) {
        printHeader("EDIT AVAILABLE LISTING");
        int listingId = input.readPositiveInt("Enter Listing ID to edit: ");

        try {
            String newFoodName = input.readNonEmptyString("New Food Name: ");
            FoodType newFoodType = input.readFoodType("New Food Classification:");
            String newCategory = input.readNonEmptyString("New Category: ");
            QuantityType newQuantityType = input.readQuantityType("New Quantity Metric:");
            int newQuantityValue = input.readPositiveInt("New Quantity Value: ");
            LocalDateTime newBestBefore = input.readFutureDateTime("New Pickup Deadline:");

            boolean updated = listingService.updateListing(
                listingId, restaurant.getUserId(), newFoodName, newFoodType,
                newCategory, newQuantityType, newQuantityValue, newBestBefore
            );

            if (updated) {
                System.out.println(BOLD_GREEN + "\n[OK] Listing #" + listingId + " updated successfully!" + RESET);
            } else {
                System.out.println(BOLD_YELLOW + "\n[X] Listing #" + listingId + " could not be updated. Ensure it is still AVAILABLE." + RESET);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(BOLD_YELLOW + "\n[X] Update rejected: " + e.getMessage() + RESET);
        } catch (SQLException e) {
            System.out.println(BOLD_RED + "\n[X] Database error updating listing: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    private void handleCancelListing(User restaurant) {
        printHeader("CANCEL / DELETE AVAILABLE LISTING");
        int listingId = input.readPositiveInt("Enter Listing ID to cancel/delete: ");

        String confirm = input.readNonEmptyString("Are you sure you want to cancel listing #" + listingId + "? (y/n): ");
        if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
            System.out.println("Deletion cancelled.");
            input.pause();
            return;
        }

        try {
            boolean deleted = listingService.cancelListing(listingId, restaurant.getUserId());
            if (deleted) {
                System.out.println(BOLD_GREEN + "\n[OK] Listing #" + listingId + " was successfully cancelled and removed." + RESET);
            } else {
                System.out.println(BOLD_YELLOW + "\n[X] Could not cancel listing #" + listingId + ". It may already be claimed or expired." + RESET);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(BOLD_YELLOW + "\n[X] Deletion rejected: " + e.getMessage() + RESET);
        } catch (SQLException e) {
            System.out.println(BOLD_RED + "\n[X] Database error deleting listing: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    // ==========================================================
    // NGO / CLAIMER MENU
    // ==========================================================

    private void runNgoMenu(User ngo, String locationName) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + BOLD_BLUE + "+--------------------------------------------------------------+");
            System.out.println("|  " + BOLD + "NGO CLAIMER PORTAL" + RESET + BOLD_BLUE + " | " + pad(ngo.getName(), 37) + " |");
            System.out.println("|  " + DIM + "* Registered Base: " + pad(locationName, 39) + RESET + BOLD_BLUE + " |");
            System.out.println("+--------------------------------------------------------------+");
            System.out.println("|  " + RESET + "[1]  View Nearby Food Listings (Nearest-First Sort)         " + BOLD_BLUE + "|");
            System.out.println("|  " + RESET + "[2]  Claim a Food Listing for Pickup                        " + BOLD_BLUE + "|");
            System.out.println("|  " + RESET + "[3]  My Claimed Pickups (History)                           " + BOLD_BLUE + "|");
            System.out.println("|  " + RESET + "[4]  Logout to Main Menu                                    " + BOLD_BLUE + "|");
            System.out.println("+--------------------------------------------------------------+" + RESET);

            int choice = input.readInt("Enter choice (1-4): ", 1, 4);
            switch (choice) {
                case 1:
                    transitionDivider("Calculating Distances & Sorting Nearest-First");
                    handleViewNearbyListings(ngo);
                    break;
                case 2:
                    transitionDivider("Opening Claim Portal");
                    handleClaimListing(ngo);
                    break;
                case 3:
                    transitionDivider("Retrieving Pickup History");
                    handleNgoHistory(ngo);
                    break;
                case 4:
                    inMenu = false;
                    transitionDivider("Logging Out");
                    System.out.println(BOLD_GREEN + "[OK] Successfully logged out from " + ngo.getName() + "." + RESET);
                    break;
            }
        }
    }

    /**
     * Displays nearby listings sorted nearest-first based on food_listings.location_id.
     * Highlights in-range listings with [IN RANGE] in bold green vs plain distance for out-of-range ones.
     */
    private void handleViewNearbyListings(User ngo) {
        printHeader("NEARBY SURPLUS FOOD LISTINGS (HAVERSINE NEAREST-FIRST)");
        System.out.println("Listings within 10 km are highlighted as " + BOLD_GREEN + "[IN RANGE]" + RESET + ".");

        try {
            List<NearbyListingDTO> listings = matchingService.getNearbyListingsForNgo(ngo.getLocationId());
            if (listings.isEmpty()) {
                System.out.println("\nNo active surplus food listings are currently available.");
                input.pause();
                return;
            }

            ConsoleTable table = new ConsoleTable();
            table.addColumn("ID", 5, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Range Status", 13, ConsoleTable.Alignment.CENTER)
                 .addColumn("Distance", 10, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Food Name", 26, ConsoleTable.Alignment.LEFT)
                 .addColumn("Type", 9, ConsoleTable.Alignment.CENTER)
                 .addColumn("Category", 14, ConsoleTable.Alignment.LEFT)
                 .addColumn("Quantity", 13, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Pickup Site", 16, ConsoleTable.Alignment.LEFT)
                 .addColumn("Restaurant", 22, ConsoleTable.Alignment.LEFT)
                 .addColumn("Contact Ph", 13, ConsoleTable.Alignment.CENTER)
                 .addColumn("Best-Before", 14, ConsoleTable.Alignment.CENTER);

            for (NearbyListingDTO item : listings) {
                FoodListing fl = item.getListing();

                String rangeTag;
                String distanceFormatted;

                if (item.isInRange()) {
                    rangeTag = BOLD_GREEN + "[IN RANGE]" + RESET;
                    distanceFormatted = BOLD_CYAN + String.format("%.2f km", item.getDistanceKm()) + RESET;
                } else {
                    rangeTag = "";
                    distanceFormatted = String.format("%.2f km", item.getDistanceKm());
                }

                String qty = fl.getQuantityValue() + " " + (fl.getQuantityType() == QuantityType.BOXES ? "boxes" : "meals");
                String bestBeforeStr = fl.getBestBefore() != null ? fl.getBestBefore().format(DISPLAY_DT) : "-";

                table.addRow(
                    String.valueOf(fl.getListingId()),
                    rangeTag,
                    distanceFormatted,
                    fl.getFoodName(),
                    fl.getFoodType().name(),
                    fl.getCategory(),
                    qty,
                    item.getLocation().getLocationName(),
                    item.getRestaurantName(),
                    item.getRestaurantPhone(),
                    bestBeforeStr
                );
            }

            System.out.println();
            table.print();

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "[X] Database error fetching nearby listings: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    private void handleClaimListing(User ngo) {
        printHeader("CLAIM A SURPLUS FOOD LISTING");
        int listingId = input.readPositiveInt("Enter Listing ID you wish to claim: ");

        try {
            transitionDivider("Processing Claim via Atomic Transaction");
            ClaimDAO.ClaimResult result = claimService.claimListing(listingId, ngo.getUserId());

            if (result.isSuccess()) {
                System.out.println(BOLD_GREEN + "+------------------------------------------------------------------+");
                System.out.println("|              [OK] CLAIM SUCCESSFUL! PICKUP CONFIRMED             |");
                System.out.println("+------------------------------------------------------------------+" + RESET);
                System.out.println("  Food Item         : " + BOLD + result.getFoodName() + RESET);
                System.out.println("  Restaurant Name   : " + result.getRestaurantName());
                System.out.println("  Contact Phone     : " + BOLD_CYAN + result.getRestaurantPhone() + RESET);
                System.out.println("\n  " + BOLD_YELLOW + "-> Please call the coordinator immediately to finalize pickup!" + RESET);
                System.out.println("-------------------------------------------------------------------");
            } else {
                System.out.println(BOLD_RED + "\n[X] Claim failed: " + result.getMessage() + RESET);
            }

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "\n[X] Database error during claim: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    private void handleNgoHistory(User ngo) {
        printHeader("MY CLAIMED PICKUPS HISTORY");
        try {
            List<NgoClaimHistoryDTO> history = claimService.getNgoClaimHistory(ngo.getUserId());
            if (history.isEmpty()) {
                System.out.println("\nYou have not claimed any food pickups yet.");
                input.pause();
                return;
            }

            ConsoleTable table = new ConsoleTable();
            table.addColumn("Claim ID", 8, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Claimed Time", 14, ConsoleTable.Alignment.CENTER)
                 .addColumn("Food Item", 26, ConsoleTable.Alignment.LEFT)
                 .addColumn("Type", 9, ConsoleTable.Alignment.CENTER)
                 .addColumn("Quantity", 13, ConsoleTable.Alignment.RIGHT)
                 .addColumn("Restaurant", 22, ConsoleTable.Alignment.LEFT)
                 .addColumn("Contact Ph", 13, ConsoleTable.Alignment.CENTER)
                 .addColumn("Locality", 16, ConsoleTable.Alignment.LEFT)
                 .addColumn("Pickup Deadline", 15, ConsoleTable.Alignment.CENTER);

            for (NgoClaimHistoryDTO h : history) {
                String claimedTimeStr = h.getClaimedTime() != null ? h.getClaimedTime().format(DISPLAY_DT) : "-";
                String bestBeforeStr = h.getBestBefore() != null ? h.getBestBefore().format(DISPLAY_DT) : "-";
                String qty = h.getQuantityValue() + " " + (h.getQuantityType() == QuantityType.BOXES ? "boxes" : "meals");

                table.addRow(
                    String.valueOf(h.getClaimId()),
                    claimedTimeStr,
                    h.getFoodName(),
                    h.getFoodType().name(),
                    qty,
                    h.getRestaurantName(),
                    h.getRestaurantPhone(),
                    h.getLocationName(),
                    bestBeforeStr
                );
            }

            System.out.println();
            table.print();

        } catch (SQLException e) {
            System.out.println(BOLD_RED + "[X] Database error fetching claim history: " + e.getMessage() + RESET);
        }
        input.pause();
    }

    public static String pad(String s, int maxLen) {
        return ConsoleTable.formatCell(s, maxLen, ConsoleTable.Alignment.LEFT);
    }
}
