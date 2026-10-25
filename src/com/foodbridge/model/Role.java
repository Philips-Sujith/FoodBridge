package com.foodbridge.model;

/**
 * Role of users in FoodBridge system.
 * NGO: Claimers (NGOs, Orphanages, Relief Groups)
 * RESTAURANT: Publishers (Restaurants, Hotels, Caterers)
 */
public enum Role {
    NGO,
    RESTAURANT;

    public static Role fromString(String value) {
        if (value == null) return null;
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        return null;
    }
}
