package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.Role;
import com.foodbridge.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for users table.
 * All queries are strictly parameterized using PreparedStatement.
 */
public class UserDAO {

    /**
     * Inserts a new user record into the database.
     * Note on security: For this assignment scope, plain-text passwords are used.
     * In a production deployment, strong hashing (e.g. BCrypt / Argon2) would be applied.
     *
     * @param user User object to persist
     * @return Generated user ID
     * @throws SQLException on database access error or constraint violation
     */
    public int createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, name, phone_number, location_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getName());
            ps.setString(5, user.getPhoneNumber());
            if (user.getLocationId() == null || user.getLocationId() <= 0) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, user.getLocationId());
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    user.setUserId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    /**
     * Checks if a username is already taken.
     *
     * @param username Username to check
     * @return true if exists, false otherwise
     * @throws SQLException on database access error
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Finds a user by their username.
     *
     * @param username Username of the user
     * @return User object or null if not found
     * @throws SQLException on database access error
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password, role, name, phone_number, location_id " +
                     "FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user by user_id.
     *
     * @param userId Primary key ID
     * @return User object or null if not found
     * @throws SQLException on database access error
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, password, role, name, phone_number, location_id " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Updates a user's password (e.g. during security migration).
     *
     * @param userId Primary key ID
     * @param newPassword New password (typically salted SHA-256 hash)
     * @return true if updated, false otherwise
     * @throws SQLException on database access error
     */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int locId = rs.getInt("location_id");
        Integer locationId = rs.wasNull() ? null : locId;

        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password"),
            Role.fromString(rs.getString("role")),
            rs.getString("name"),
            rs.getString("phone_number"),
            locationId
        );
    }
}

