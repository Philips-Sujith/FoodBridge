package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.ActivityLog;

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
 * Data Access Object for activity_log table.
 * Records and retrieves platform events with PreparedStatements.
 */
public class ActivityLogDAO {

    /**
     * Logs an activity or platform event.
     *
     * @param userId User who performed the action (or null for system actions)
     * @param action Category or code of the action (e.g. USER_LOGIN, CLAIM_FOOD)
     * @param details Descriptive message
     * @return Generated log ID
     * @throws SQLException on database error
     */
    public int logActivity(Integer userId, String action, String details) throws SQLException {
        String sql = "INSERT INTO activity_log (user_id, action, details, created_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (userId != null && userId > 0) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }

            ps.setString(2, action);
            ps.setString(3, details);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves the most recent activity logs across the entire platform.
     *
     * @param limit Maximum number of records to return
     * @return List of ActivityLog records, newest first
     * @throws SQLException on database error
     */
    public List<ActivityLog> getRecentLogs(int limit) throws SQLException {
        String sql = "SELECT log_id, user_id, action, details, created_time " +
                     "FROM activity_log ORDER BY created_time DESC LIMIT ?";

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToActivityLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Retrieves activity logs for a specific user.
     *
     * @param userId Target user ID
     * @return List of ActivityLog records, newest first
     * @throws SQLException on database error
     */
    public List<ActivityLog> getLogsByUser(int userId) throws SQLException {
        String sql = "SELECT log_id, user_id, action, details, created_time " +
                     "FROM activity_log WHERE user_id = ? ORDER BY created_time DESC";

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRowToActivityLog(rs));
                }
            }
        }
        return logs;
    }

    private ActivityLog mapRowToActivityLog(ResultSet rs) throws SQLException {
        int uId = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : uId;
        Timestamp ts = rs.getTimestamp("created_time");

        return new ActivityLog(
            rs.getInt("log_id"),
            userId,
            rs.getString("action"),
            rs.getString("details"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
