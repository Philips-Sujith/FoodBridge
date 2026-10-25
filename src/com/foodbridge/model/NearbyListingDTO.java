package com.foodbridge.model;

/**
 * Data Transfer Object for NGO nearby food listing view.
 * Combines listing details, location, calculated Haversine distance,
 * in-range (10km) flag, and publisher contact details for pickup coordination.
 */
public class NearbyListingDTO {
    private FoodListing listing;
    private Location location;
    private double distanceKm;
    private boolean inRange;
    private String restaurantName;
    private String restaurantPhone;

    public NearbyListingDTO() {
    }

    public NearbyListingDTO(FoodListing listing, Location location, double distanceKm, 
                            boolean inRange, String restaurantName, String restaurantPhone) {
        this.listing = listing;
        this.location = location;
        this.distanceKm = distanceKm;
        this.inRange = inRange;
        this.restaurantName = restaurantName;
        this.restaurantPhone = restaurantPhone;
    }

    public FoodListing getListing() {
        return listing;
    }

    public void setListing(FoodListing listing) {
        this.listing = listing;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isInRange() {
        return inRange;
    }

    public void setInRange(boolean inRange) {
        this.inRange = inRange;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getRestaurantPhone() {
        return restaurantPhone;
    }

    public void setRestaurantPhone(String restaurantPhone) {
        this.restaurantPhone = restaurantPhone;
    }
}
