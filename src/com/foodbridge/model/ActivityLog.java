package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry of system activity.
 */
public class ActivityLog {

    private int logId;
    private Integer userId;
    private String action;
    private String details;
    private LocalDateTime createdTime;

    public ActivityLog() {
    }

    public ActivityLog(int logId, Integer userId, String action, String details, LocalDateTime createdTime) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.createdTime = createdTime;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "logId=" + logId +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
}
