package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Restaurant listing view.
 * Displays listing details along with claiming NGO info if already claimed.
 */
public class RestaurantListingDTO {
    private FoodListing listing;
    private String locationName;
    private String claimedNgoName;
    private String claimedNgoPhone;
    private LocalDateTime claimedTime;

    public RestaurantListingDTO() {
    }

    public RestaurantListingDTO(FoodListing listing, String locationName, String claimedNgoName, 
                                String claimedNgoPhone, LocalDateTime claimedTime) {
        this.listing = listing;
        this.locationName = locationName;
        this.claimedNgoName = claimedNgoName;
        this.claimedNgoPhone = claimedNgoPhone;
        this.claimedTime = claimedTime;
    }

    public FoodListing getListing() {
        return listing;
    }

    public void setListing(FoodListing listing) {
        this.listing = listing;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getClaimedNgoName() {
        return claimedNgoName;
    }

    public void setClaimedNgoName(String claimedNgoName) {
        this.claimedNgoName = claimedNgoName;
    }

    public String getClaimedNgoPhone() {
        return claimedNgoPhone;
    }

    public void setClaimedNgoPhone(String claimedNgoPhone) {
        this.claimedNgoPhone = claimedNgoPhone;
    }

    public LocalDateTime getClaimedTime() {
        return claimedTime;
    }

    public void setClaimedTime(LocalDateTime claimedTime) {
        this.claimedTime = claimedTime;
    }
}
