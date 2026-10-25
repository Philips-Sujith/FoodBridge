package com.foodbridge.model;

/**
 * Status of a food listing in the system.
 */
public enum ListingStatus {
    AVAILABLE,
    CLAIMED,
    EXPIRED;

    public static ListingStatus fromString(String value) {
        if (value == null) return null;
        for (ListingStatus s : values()) {
            if (s.name().equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        return null;
    }
}
