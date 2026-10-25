package com.foodbridge.model;

/**
 * Metric of food quantity: BOXES or SERVES_PEOPLE.
 */
public enum QuantityType {
    BOXES("Boxes"),
    SERVES_PEOPLE("Serves People");

    private final String displayName;

    QuantityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static QuantityType fromString(String value) {
        if (value == null) return null;
        for (QuantityType qt : values()) {
            if (qt.name().equalsIgnoreCase(value.trim()) || 
                qt.displayName.equalsIgnoreCase(value.trim())) {
                return qt;
            }
        }
        return null;
    }
}
