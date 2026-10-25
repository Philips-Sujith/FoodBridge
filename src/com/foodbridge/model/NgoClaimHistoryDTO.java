package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an NGO's claim history item.
 * Includes claim timestamp, restaurant name, restaurant phone number,
 * food details, and listing status.
 */
public class NgoClaimHistoryDTO {
    private int claimId;
    private LocalDateTime claimedTime;
    private int listingId;
    private String foodName;
    private FoodType foodType;
    private String category;
    private QuantityType quantityType;
    private int quantityValue;
    private LocalDateTime bestBefore;
    private String restaurantName;
    private String restaurantPhone;
    private String locationName;
    private ListingStatus listingStatus;

    public NgoClaimHistoryDTO() {
    }

    public NgoClaimHistoryDTO(int claimId, LocalDateTime claimedTime, int listingId, 
                             String foodName, FoodType foodType, String category, 
                             QuantityType quantityType, int quantityValue, 
                             LocalDateTime bestBefore, String restaurantName, 
                             String restaurantPhone, String locationName, 
                             ListingStatus listingStatus) {
        this.claimId = claimId;
        this.claimedTime = claimedTime;
        this.listingId = listingId;
        this.foodName = foodName;
        this.foodType = foodType;
        this.category = category;
        this.quantityType = quantityType;
        this.quantityValue = quantityValue;
        this.bestBefore = bestBefore;
        this.restaurantName = restaurantName;
        this.restaurantPhone = restaurantPhone;
        this.locationName = locationName;
        this.listingStatus = listingStatus;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public LocalDateTime getClaimedTime() {
        return claimedTime;
    }

    public void setClaimedTime(LocalDateTime claimedTime) {
        this.claimedTime = claimedTime;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public FoodType getFoodType() {
        return foodType;
    }

    public void setFoodType(FoodType foodType) {
        this.foodType = foodType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public QuantityType getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(QuantityType quantityType) {
        this.quantityType = quantityType;
    }

    public int getQuantityValue() {
        return quantityValue;
    }

    public void setQuantityValue(int quantityValue) {
        this.quantityValue = quantityValue;
    }

    public LocalDateTime getBestBefore() {
        return bestBefore;
    }

    public void setBestBefore(LocalDateTime bestBefore) {
        this.bestBefore = bestBefore;
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

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public ListingStatus getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(ListingStatus listingStatus) {
        this.listingStatus = listingStatus;
    }
}
