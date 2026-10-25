package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodType;
import com.foodbridge.model.ListingStatus;
import com.foodbridge.model.Location;
import com.foodbridge.model.QuantityType;
import com.foodbridge.model.RestaurantListingDTO;

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
 * Data Access Object for food_listings table.
 * Strictly uses PreparedStatements for all database operations.
 */
public class FoodListingDAO {

    /**
     * Creates a new food listing.
     *
     * @param listing The FoodListing object to insert
     * @return Generated listing ID
     * @throws SQLException on database error
     */
    public int createListing(FoodListing listing) throws SQLException {
        String sql = "INSERT INTO food_listings (restaurant_id, food_name, food_type, category, " +
                     "quantity_type, quantity_value, location_id, best_before, posted_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now();
            listing.setPostedTime(now);
            if (listing.getStatus() == null) {
                listing.setStatus(ListingStatus.AVAILABLE);
            }

            ps.setInt(1, listing.getRestaurantId());
            ps.setString(2, listing.getFoodName());
            ps.setString(3, listing.getFoodType().name());
            ps.setString(4, listing.getCategory());
            ps.setString(5, listing.getQuantityType().name());
            ps.setInt(6, listing.getQuantityValue());
            ps.setInt(7, listing.getLocationId());
            ps.setTimestamp(8, Timestamp.valueOf(listing.getBestBefore()));
            ps.setTimestamp(9, Timestamp.valueOf(listing.getPostedTime()));
            ps.setString(10, listing.getStatus().name());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    listing.setListingId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves all listings posted by a specific restaurant,
     * including claiming NGO details if claimed.
     *
     * @param restaurantId Restaurant user ID
     * @return List of RestaurantListingDTO
     * @throws SQLException on database error
     */
    public List<RestaurantListingDTO> findListingsByRestaurant(int restaurantId) throws SQLException {
        String sql = "SELECT fl.listing_id, fl.restaurant_id, fl.food_name, fl.food_type, fl.category, " +
                     "fl.quantity_type, fl.quantity_value, fl.location_id, fl.best_before, fl.posted_time, fl.status, " +
                     "loc.location_name, u_ngo.name AS claimed_ngo_name, u_ngo.phone_number AS claimed_ngo_phone, " +
                     "c.claimed_time " +
                     "FROM food_listings fl " +
                     "JOIN locations loc ON fl.location_id = loc.location_id " +
                     "LEFT JOIN claims c ON fl.listing_id = c.listing_id " +
                     "LEFT JOIN users u_ngo ON c.ngo_id = u_ngo.user_id " +
                     "WHERE fl.restaurant_id = ? " +
                     "ORDER BY fl.posted_time DESC";

        List<RestaurantListingDTO> result = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, restaurantId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FoodListing listing = mapRowToFoodListing(rs);
                    String locationName = rs.getString("location_name");
                    String claimedNgoName = rs.getString("claimed_ngo_name");
                    String claimedNgoPhone = rs.getString("claimed_ngo_phone");
                    Timestamp claimedTs = rs.getTimestamp("claimed_time");
                    LocalDateTime claimedTime = claimedTs != null ? claimedTs.toLocalDateTime() : null;

                    result.add(new RestaurantListingDTO(listing, locationName, claimedNgoName, claimedNgoPhone, claimedTime));
                }
            }
        }
        return result;
    }

    /**
     * Updates an existing food listing if its status is still AVAILABLE.
     *
     * @param listing Updated listing object
     * @return Number of rows updated (1 on success, 0 if not found or already claimed/expired)
     * @throws SQLException on database error
     */
    public int updateListing(FoodListing listing) throws SQLException {
        String sql = "UPDATE food_listings SET food_name = ?, food_type = ?, category = ?, " +
                     "quantity_type = ?, quantity_value = ?, best_before = ? " +
                     "WHERE listing_id = ? AND restaurant_id = ? AND status = 'AVAILABLE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, listing.getFoodName());
            ps.setString(2, listing.getFoodType().name());
            ps.setString(3, listing.getCategory());
            ps.setString(4, listing.getQuantityType().name());
            ps.setInt(5, listing.getQuantityValue());
            ps.setTimestamp(6, Timestamp.valueOf(listing.getBestBefore()));
            ps.setInt(7, listing.getListingId());
            ps.setInt(8, listing.getRestaurantId());

            return ps.executeUpdate();
        }
    }

    /**
     * Deletes / cancels a food listing if its status is still AVAILABLE.
     *
     * @param listingId Listing ID to delete
     * @param restaurantId Restaurant user ID
     * @return Number of rows deleted (1 on success, 0 if not found or not AVAILABLE)
     * @throws SQLException on database error
     */
    public int deleteListing(int listingId, int restaurantId) throws SQLException {
        String sql = "DELETE FROM food_listings WHERE listing_id = ? AND restaurant_id = ? AND status = 'AVAILABLE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);
            ps.setInt(2, restaurantId);

            return ps.executeUpdate();
        }
    }

    /**
     * Batch updates status to 'EXPIRED' for all AVAILABLE listings whose best_before < now.
     *
     * @return Number of listings marked as EXPIRED
     * @throws SQLException on database error
     */
    public int expireOutdatedListings() throws SQLException {
        String sql = "UPDATE food_listings SET status = 'EXPIRED' " +
                     "WHERE status = 'AVAILABLE' AND best_before < ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate();
        }
    }

    /**
     * Helper holder for available listings with joined location and restaurant contact details.
     */
    public static class RawAvailableListingRow {
        public FoodListing listing;
        public Location location;
        public String restaurantName;
        public String restaurantPhone;
    }

    /**
     * Retrieves all active AVAILABLE listings joined with their location and restaurant contact info.
     *
     * @return List of RawAvailableListingRow
     * @throws SQLException on database error
     */
    public List<RawAvailableListingRow> findAvailableListingsWithDetails() throws SQLException {
        String sql = "SELECT fl.listing_id, fl.restaurant_id, fl.food_name, fl.food_type, fl.category, " +
                     "fl.quantity_type, fl.quantity_value, fl.location_id, fl.best_before, fl.posted_time, fl.status, " +
                     "loc.location_name, loc.latitude, loc.longitude, " +
                     "u.name AS restaurant_name, u.phone_number AS restaurant_phone " +
                     "FROM food_listings fl " +
                     "JOIN locations loc ON fl.location_id = loc.location_id " +
                     "JOIN users u ON fl.restaurant_id = u.user_id " +
                     "WHERE fl.status = 'AVAILABLE'";

        List<RawAvailableListingRow> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RawAvailableListingRow row = new RawAvailableListingRow();
                row.listing = mapRowToFoodListing(rs);
                row.location = new Location(
                    rs.getInt("location_id"),
                    rs.getString("location_name"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
                );
                row.restaurantName = rs.getString("restaurant_name");
                row.restaurantPhone = rs.getString("restaurant_phone");
                list.add(row);
            }
        }
        return list;
    }

    /**
     * Finds a listing by its ID.
     *
     * @param listingId Listing ID
     * @return FoodListing or null
     * @throws SQLException on database error
     */
    public FoodListing getListingById(int listingId) throws SQLException {
        String sql = "SELECT listing_id, restaurant_id, food_name, food_type, category, " +
                     "quantity_type, quantity_value, location_id, best_before, posted_time, status " +
                     "FROM food_listings WHERE listing_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFoodListing(rs);
                }
            }
        }
        return null;
    }

    private FoodListing mapRowToFoodListing(ResultSet rs) throws SQLException {
        Timestamp bestBeforeTs = rs.getTimestamp("best_before");
        Timestamp postedTs = rs.getTimestamp("posted_time");

        return new FoodListing(
            rs.getInt("listing_id"),
            rs.getInt("restaurant_id"),
            rs.getString("food_name"),
            FoodType.fromString(rs.getString("food_type")),
            rs.getString("category"),
            QuantityType.fromString(rs.getString("quantity_type")),
            rs.getInt("quantity_value"),
            rs.getInt("location_id"),
            bestBeforeTs != null ? bestBeforeTs.toLocalDateTime() : null,
            postedTs != null ? postedTs.toLocalDateTime() : null,
            ListingStatus.fromString(rs.getString("status"))
        );
    }
}
