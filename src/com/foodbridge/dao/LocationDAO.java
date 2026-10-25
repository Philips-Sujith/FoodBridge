package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for locations table.
 * Uses PreparedStatements exclusively for all queries.
 */
public class LocationDAO {

    /**
     * Retrieves all predefined locations ordered by location_id.
     *
     * @return List of Location objects
     * @throws SQLException on database access error
     */
    public List<Location> getAllLocations() throws SQLException {
        String sql = "SELECT location_id, location_name, latitude, longitude FROM locations ORDER BY location_id ASC";
        List<Location> locations = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                locations.add(mapRowToLocation(rs));
            }
        }
        return locations;
    }

    /**
     * Retrieves a single location by its primary key ID.
     *
     * @param locationId ID of the location
     * @return Location object or null if not found
     * @throws SQLException on database access error
     */
    public Location getLocationById(int locationId) throws SQLException {
        String sql = "SELECT location_id, location_name, latitude, longitude FROM locations WHERE location_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, locationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLocation(rs);
                }
            }
        }
        return null;
    }

    private Location mapRowToLocation(ResultSet rs) throws SQLException {
        return new Location(
            rs.getInt("location_id"),
            rs.getString("location_name"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude")
        );
    }
}
