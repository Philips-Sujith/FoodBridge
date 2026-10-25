package com.foodbridge.model;

/**
 * Type of food posted in listings: VEG or NON_VEG.
 */
public enum FoodType {
    VEG,
    NON_VEG;

    public static FoodType fromString(String value) {
        if (value == null) return null;
        for (FoodType ft : values()) {
            if (ft.name().equalsIgnoreCase(value.trim()) || 
                (ft == NON_VEG && value.trim().equalsIgnoreCase("NON-VEG"))) {
                return ft;
            }
        }
        return null;
    }
}
