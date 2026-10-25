package com.foodbridge.service;

import com.foodbridge.dao.ClaimDAO;
import com.foodbridge.dao.FoodListingDAO;
import com.foodbridge.model.NgoClaimHistoryDTO;

import java.sql.SQLException;
import java.util.List;

/**
 * ClaimService coordinates claiming operations and claim history retrieval.
 */
public class ClaimService {

    private final ClaimDAO claimDAO;
    private final FoodListingDAO foodListingDAO;
    private final com.foodbridge.dao.NotificationDAO notificationDAO;
    private final com.foodbridge.dao.ActivityLogDAO activityLogDAO;

    public ClaimService() {
        this.claimDAO = new ClaimDAO();
        this.foodListingDAO = new FoodListingDAO();
        this.notificationDAO = new com.foodbridge.dao.NotificationDAO();
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    public ClaimService(ClaimDAO claimDAO, FoodListingDAO foodListingDAO) {
        this.claimDAO = claimDAO;
        this.foodListingDAO = foodListingDAO;
        this.notificationDAO = new com.foodbridge.dao.NotificationDAO();
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    /**
     * Claims a food listing with concurrency/race condition safety.
     * Generates real-time notifications for both the claiming NGO and publisher restaurant.
     *
     * @param listingId Listing ID to claim
     * @param ngoId User ID of claiming NGO
     * @return ClaimResult containing success flag and restaurant contact info
     * @throws SQLException on database error
     */
    public ClaimDAO.ClaimResult claimListing(int listingId, int ngoId) throws SQLException {
        // Run expiry sweep first
        foodListingDAO.expireOutdatedListings();

        // Perform atomic claim
        ClaimDAO.ClaimResult result = claimDAO.claimListing(listingId, ngoId);

        if (result.isSuccess()) {
            try {
                // 1. Audit log
                activityLogDAO.logActivity(ngoId, "CLAIM_FOOD", "Claimed listing #" + listingId + " (" + result.getFoodName() + ")");

                // 2. Notify the restaurant publisher
                if (result.getRestaurantId() > 0) {
                    notificationDAO.createNotification(
                        result.getRestaurantId(),
                        "Your food listing '" + result.getFoodName() + "' was claimed by an NGO! Please prepare for pickup."
                    );
                }

                // 3. Notify the claiming NGO
                notificationDAO.createNotification(
                    ngoId,
                    "Successfully claimed '" + result.getFoodName() + "' from " + result.getRestaurantName() + " (" + result.getRestaurantPhone() + ")."
                );
            } catch (Exception ignored) {
                // Ensure secondary notification failures never interrupt the primary transaction
            }
        }

        return result;
    }


    /**
     * Retrieves the pickup history for an NGO.
     *
     * @param ngoId User ID of the NGO
     * @return List of NgoClaimHistoryDTO
     * @throws SQLException on database error
     */
    public List<NgoClaimHistoryDTO> getNgoClaimHistory(int ngoId) throws SQLException {
        return claimDAO.findClaimsByNgo(ngoId);
    }
}
