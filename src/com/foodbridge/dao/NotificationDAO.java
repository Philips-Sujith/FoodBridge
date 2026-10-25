package com.foodbridge.dao;

import com.foodbridge.config.DBConnection;
import com.foodbridge.model.Notification;

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
 * Data Access Object for notifications table.
 * Strictly uses PreparedStatements for all database interactions.
 */
public class NotificationDAO {

    /**
     * Creates a new notification record.
     *
     * @param userId Target user ID
     * @param message Notification message
     * @return Generated notification ID
     * @throws SQLException on database error
     */
    public int createNotification(int userId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, is_read, created_time) " +
                     "VALUES (?, ?, FALSE, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now();
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setTimestamp(3, Timestamp.valueOf(now));

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
     * Retrieves unread notifications for a specific user, newest first.
     *
     * @param userId User ID
     * @return List of unread notifications
     * @throws SQLException on database error
     */
    public List<Notification> getUnreadNotificationsForUser(int userId) throws SQLException {
        String sql = "SELECT notification_id, user_id, message, is_read, created_time " +
                     "FROM notifications WHERE user_id = ? AND is_read = FALSE " +
                     "ORDER BY created_time DESC";

        List<Notification> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all notifications for a specific user, newest first.
     *
     * @param userId User ID
     * @return List of all notifications
     * @throws SQLException on database error
     */
    public List<Notification> getAllNotificationsForUser(int userId) throws SQLException {
        String sql = "SELECT notification_id, user_id, message, is_read, created_time " +
                     "FROM notifications WHERE user_id = ? " +
                     "ORDER BY created_time DESC";

        List<Notification> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
        }
        return list;
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId ID of notification
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    public boolean markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        }
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_time");
        return new Notification(
            rs.getInt("notification_id"),
            rs.getInt("user_id"),
            rs.getString("message"),
            rs.getBoolean("is_read"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
