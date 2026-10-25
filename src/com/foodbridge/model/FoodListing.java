package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Model representing a surplus food listing posted by a publisher.
 */
public class FoodListing {
    private int listingId;
    private int restaurantId;
    private String foodName;
    private FoodType foodType;
    private String category;
    private QuantityType quantityType;
    private int quantityValue;
    private int locationId;
    private LocalDateTime bestBefore;
    private LocalDateTime postedTime;
    private ListingStatus status;

    public FoodListing() {
    }

    public FoodListing(int listingId, int restaurantId, String foodName, FoodType foodType, 
                       String category, QuantityType quantityType, int quantityValue, 
                       int locationId, LocalDateTime bestBefore, LocalDateTime postedTime, 
                       ListingStatus status) {
        this.listingId = listingId;
        this.restaurantId = restaurantId;
        this.foodName = foodName;
        this.foodType = foodType;
        this.category = category;
        this.quantityType = quantityType;
        this.quantityValue = quantityValue;
        this.locationId = locationId;
        this.bestBefore = bestBefore;
        this.postedTime = postedTime;
        this.status = status;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
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

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public LocalDateTime getBestBefore() {
        return bestBefore;
    }

    public void setBestBefore(LocalDateTime bestBefore) {
        this.bestBefore = bestBefore;
    }

    public LocalDateTime getPostedTime() {
        return postedTime;
    }

    public void setPostedTime(LocalDateTime postedTime) {
        this.postedTime = postedTime;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }
}
