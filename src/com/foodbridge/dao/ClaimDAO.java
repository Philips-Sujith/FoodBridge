package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.FoodType;
import com.foodbridge.model.ListingStatus;
import com.foodbridge.model.NgoClaimHistoryDTO;
import com.foodbridge.model.QuantityType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for claims table.
 * Handles race-condition-safe atomic claiming using transactions
 * and retrieves NGO claim pickup history.
 */
public class ClaimDAO {

    /**
     * Result of an atomic claim attempt.
     */
    public static class ClaimResult {
        private final boolean success;
        private final String message;
        private final String restaurantName;
        private final String restaurantPhone;
        private final String foodName;
        private final int restaurantId;

        public ClaimResult(boolean success, String message, String restaurantName, String restaurantPhone, String foodName) {
            this(success, message, restaurantName, restaurantPhone, foodName, 0);
        }

        public ClaimResult(boolean success, String message, String restaurantName, String restaurantPhone, String foodName, int restaurantId) {
            this.success = success;
            this.message = message;
            this.restaurantName = restaurantName;
            this.restaurantPhone = restaurantPhone;
            this.foodName = foodName;
            this.restaurantId = restaurantId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public String getRestaurantPhone() {
            return restaurantPhone;
        }

        public String getFoodName() {
            return foodName;
        }

        public int getRestaurantId() {
            return restaurantId;
        }
    }

    /**
     * Attempts to claim a food listing atomically inside a transaction.
     * Prevents race conditions by conditionally updating status from 'AVAILABLE' to 'CLAIMED'.
     *
     * @param listingId ID of listing to claim
     * @param ngoId ID of the claiming NGO
     * @return ClaimResult with success status and restaurant contact details
     * @throws SQLException on database error
     */
    public ClaimResult claimListing(int listingId, int ngoId) throws SQLException {
        String lockAndCheckSql = "SELECT fl.status, fl.food_name, fl.restaurant_id, u.name AS rest_name, u.phone_number AS rest_phone " +
                                 "FROM food_listings fl " +
                                 "JOIN users u ON fl.restaurant_id = u.user_id " +
                                 "WHERE fl.listing_id = ? FOR UPDATE";
        String insertSql = "INSERT INTO claims (listing_id, ngo_id, claimed_time) VALUES (?, ?, ?)";
        String updateSql = "UPDATE food_listings SET status = 'CLAIMED' WHERE listing_id = ? AND status = 'AVAILABLE'";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // BEGIN TRANSACTION

            // 1. Check listing availability with row-level exclusive lock (FOR UPDATE)
            String status = null;
            String foodName = "Food Item";
            String restaurantName = "Unknown";
            String restaurantPhone = "N/A";
            int restaurantId = 0;

            try (PreparedStatement psCheck = conn.prepareStatement(lockAndCheckSql)) {
                psCheck.setInt(1, listingId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        status = rs.getString("status");
                        foodName = rs.getString("food_name");
                        restaurantId = rs.getInt("restaurant_id");
                        restaurantName = rs.getString("rest_name");
                        restaurantPhone = rs.getString("rest_phone");
                    } else {
                        conn.rollback();
                        return new ClaimResult(false, "Listing #" + listingId + " does not exist.", null, null, null);
                    }
                }
            }

            // Verify status is AVAILABLE
            if (!"AVAILABLE".equalsIgnoreCase(status)) {
                conn.rollback();
                return new ClaimResult(false, "Sorry, this listing has already been claimed.", null, null, null);
            }

            // 2. Insert into claims table
            LocalDateTime now = LocalDateTime.now();
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setInt(1, listingId);
                psInsert.setInt(2, ngoId);
                psInsert.setTimestamp(3, Timestamp.valueOf(now));
                psInsert.executeUpdate();
            }

            // 3. Update listing status to CLAIMED
            int rowsUpdated;
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setInt(1, listingId);
                rowsUpdated = psUpdate.executeUpdate();
            }

            if (rowsUpdated == 0) {
                conn.rollback();
                return new ClaimResult(false, "Sorry, this listing has already been claimed.", null, null, null);
            }

            // 4. COMMIT TRANSACTION
            conn.commit();
            return new ClaimResult(true, "Successfully claimed!", restaurantName, restaurantPhone, foodName, restaurantId);


        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ERROR] Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[WARN] Failed to reset autoCommit/close: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves all claimed pickups for an NGO.
     *
     * @param ngoId User ID of the NGO
     * @return List of NgoClaimHistoryDTO
     * @throws SQLException on database error
     */
    public List<NgoClaimHistoryDTO> findClaimsByNgo(int ngoId) throws SQLException {
        String sql = "SELECT c.claim_id, c.claimed_time, " +
                     "fl.listing_id, fl.food_name, fl.food_type, fl.category, " +
                     "fl.quantity_type, fl.quantity_value, fl.best_before, fl.status, " +
                     "u_rest.name AS restaurant_name, u_rest.phone_number AS restaurant_phone, " +
                     "loc.location_name " +
                     "FROM claims c " +
                     "JOIN food_listings fl ON c.listing_id = fl.listing_id " +
                     "JOIN users u_rest ON fl.restaurant_id = u_rest.user_id " +
                     "JOIN locations loc ON fl.location_id = loc.location_id " +
                     "WHERE c.ngo_id = ? " +
                     "ORDER BY c.claimed_time DESC";

        List<NgoClaimHistoryDTO> history = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ngoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp claimedTs = rs.getTimestamp("claimed_time");
                    Timestamp bestBeforeTs = rs.getTimestamp("best_before");

                    NgoClaimHistoryDTO dto = new NgoClaimHistoryDTO(
                        rs.getInt("claim_id"),
                        claimedTs != null ? claimedTs.toLocalDateTime() : null,
                        rs.getInt("listing_id"),
                        rs.getString("food_name"),
                        FoodType.fromString(rs.getString("food_type")),
                        rs.getString("category"),
                        QuantityType.fromString(rs.getString("quantity_type")),
                        rs.getInt("quantity_value"),
                        bestBeforeTs != null ? bestBeforeTs.toLocalDateTime() : null,
                        rs.getString("restaurant_name"),
                        rs.getString("restaurant_phone"),
                        rs.getString("location_name"),
                        ListingStatus.fromString(rs.getString("status"))
                    );
                    history.add(dto);
                }
            }
        }
        return history;
    }
}
