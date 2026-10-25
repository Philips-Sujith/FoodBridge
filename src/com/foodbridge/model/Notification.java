package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Entity representing an in-app notification for a user.
 */
public class Notification {

    private int notificationId;
    private int userId;
    private String message;
    private boolean read;
    private LocalDateTime createdTime;

    public Notification() {
    }

    public Notification(int notificationId, int userId, String message, boolean read, LocalDateTime createdTime) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.message = message;
        this.read = read;
        this.createdTime = createdTime;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + notificationId +
                ", userId=" + userId +
                ", message='" + message + '\'' +
                ", isRead=" + read +
                ", createdTime=" + createdTime +
                '}';
    }
}
