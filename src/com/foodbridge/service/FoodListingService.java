package com.foodbridge.service;

import com.foodbridge.dao.FoodListingDAO;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodType;
import com.foodbridge.model.ListingStatus;
import com.foodbridge.model.QuantityType;
import com.foodbridge.model.RestaurantListingDTO;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FoodListingService coordinates business logic for food listings.
 */
public class FoodListingService {

    private final FoodListingDAO foodListingDAO;
    private final com.foodbridge.dao.NotificationDAO notificationDAO;
    private final com.foodbridge.dao.ActivityLogDAO activityLogDAO;

    public FoodListingService() {
        this.foodListingDAO = new FoodListingDAO();
        this.notificationDAO = new com.foodbridge.dao.NotificationDAO();
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    public FoodListingService(FoodListingDAO foodListingDAO) {
        this.foodListingDAO = foodListingDAO;
        this.notificationDAO = new com.foodbridge.dao.NotificationDAO();
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    /**
     * Posts a new food listing.
     *
     * @param restaurantId ID of the posting restaurant
     * @param foodName Food title
     * @param foodType VEG or NON_VEG
     * @param category Category name
     * @param quantityType BOXES or SERVES_PEOPLE
     * @param quantityValue Numerical amount
     * @param locationId Restaurant's location ID
     * @param bestBefore Expiry / pickup deadline
     * @return Created FoodListing object
     * @throws SQLException on database error
     */
    public FoodListing createListing(int restaurantId, String foodName, FoodType foodType,
                                     String category, QuantityType quantityType, int quantityValue,
                                     int locationId, LocalDateTime bestBefore) throws SQLException {
        if (foodName == null || foodName.trim().isEmpty()) {
            throw new IllegalArgumentException("Food name cannot be empty.");
        }
        if (foodType == null) {
            throw new IllegalArgumentException("Food type must be VEG or NON_VEG.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }
        if (quantityType == null) {
            throw new IllegalArgumentException("Quantity type must be BOXES or SERVES_PEOPLE.");
        }
        if (quantityValue <= 0) {
            throw new IllegalArgumentException("Quantity value must be greater than zero.");
        }
        if (bestBefore == null || bestBefore.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Best-before time must be in the future.");
        }

        FoodListing listing = new FoodListing(
            0, restaurantId, foodName.trim(), foodType, category.trim(),
            quantityType, quantityValue, locationId, bestBefore,
            LocalDateTime.now(), ListingStatus.AVAILABLE
        );

        int id = foodListingDAO.createListing(listing);
        listing.setListingId(id);

        try {
            activityLogDAO.logActivity(restaurantId, "POST_LISTING", "Posted listing #" + id + " (" + foodName.trim() + ")");
            notificationDAO.createNotification(restaurantId, "Your food listing '" + foodName.trim() + "' is now live for nearby NGOs.");
        } catch (Exception ignored) {}

        return listing;
    }

    /**
     * Retrieves all listings posted by a restaurant, including claimer information.
     */
    public List<RestaurantListingDTO> getRestaurantListings(int restaurantId) throws SQLException {
        // First run expiry sweep to update outdated listings
        foodListingDAO.expireOutdatedListings();
        return foodListingDAO.findListingsByRestaurant(restaurantId);
    }

    /**
     * Edits an existing listing, strictly enforcing that status must be AVAILABLE.
     */
    public boolean updateListing(int listingId, int restaurantId, String foodName, FoodType foodType,
                                 String category, QuantityType quantityType, int quantityValue,
                                 LocalDateTime bestBefore) throws SQLException {
        FoodListing existing = foodListingDAO.getListingById(listingId);
        if (existing == null) {
            throw new IllegalArgumentException("Listing #" + listingId + " does not exist.");
        }
        if (existing.getRestaurantId() != restaurantId) {
            throw new IllegalArgumentException("You do not own listing #" + listingId + ".");
        }
        if (existing.getStatus() != ListingStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot edit listing #" + listingId + " because its status is " + existing.getStatus() + ".");
        }

        existing.setFoodName(foodName.trim());
        existing.setFoodType(foodType);
        existing.setCategory(category.trim());
        existing.setQuantityType(quantityType);
        existing.setQuantityValue(quantityValue);
        existing.setBestBefore(bestBefore);

        int updated = foodListingDAO.updateListing(existing);
        return updated > 0;
    }

    /**
     * Deletes / cancels an existing listing, strictly enforcing that status must be AVAILABLE.
     */
    public boolean cancelListing(int listingId, int restaurantId) throws SQLException {
        FoodListing existing = foodListingDAO.getListingById(listingId);
        if (existing == null) {
            throw new IllegalArgumentException("Listing #" + listingId + " does not exist.");
        }
        if (existing.getRestaurantId() != restaurantId) {
            throw new IllegalArgumentException("You do not own listing #" + listingId + ".");
        }
        if (existing.getStatus() != ListingStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot cancel listing #" + listingId + " because its status is " + existing.getStatus() + ".");
        }

        int deleted = foodListingDAO.deleteListing(listingId, restaurantId);
        if (deleted > 0) {
            try {
                activityLogDAO.logActivity(restaurantId, "CANCEL_LISTING", "Cancelled listing #" + listingId);
            } catch (Exception ignored) {}
        }
        return deleted > 0;
    }


    /**
     * Batch expires outdated listings whose deadline has passed.
     */
    public int expireOutdatedListings() throws SQLException {
        return foodListingDAO.expireOutdatedListings();
    }
}
