package com.foodbridge.service;

import com.foodbridge.dao.FoodListingDAO;
import com.foodbridge.dao.LocationDAO;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Location;
import com.foodbridge.model.NearbyListingDTO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MatchingService handles geospatial distance calculations and sorting.
 * Employs the great-circle Haversine formula to find listings near an NGO,
 * sorts them nearest-first (ascending distance), and flags listings within 10km.
 */
public class MatchingService {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double IN_RANGE_THRESHOLD_KM = 10.0;

    private final FoodListingDAO foodListingDAO;
    private final LocationDAO locationDAO;

    public MatchingService() {
        this.foodListingDAO = new FoodListingDAO();
        this.locationDAO = new LocationDAO();
    }

    public MatchingService(FoodListingDAO foodListingDAO, LocationDAO locationDAO) {
        this.foodListingDAO = foodListingDAO;
        this.locationDAO = locationDAO;
    }

    /**
     * Calculates the great-circle distance between two points on the Earth's surface
     * specified by decimal degrees (latitude and longitude) using the Haversine formula.
     *
     * Academic Explanation:
     * 1. Convert decimal degree differences to radians:
     *      dLat = toRadians(lat2 - lat1)
     *      dLon = toRadians(lon2 - lon1)
     * 2. Compute the square of half the chord length between the points:
     *      a = sin²(dLat / 2) + cos(lat1) * cos(lat2) * sin²(dLon / 2)
     * 3. Compute the angular distance in radians using the two-argument arctangent:
     *      c = 2 * atan2(√a, √(1 - a))
     * 4. Multiply by the Earth's volumetric mean radius (6,371 km) to yield distance in km:
     *      distance = R * c
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Step 1: Convert latitude & longitude differences from degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Convert starting and ending latitudes to radians for spherical trigonometry
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        // Step 2: Haversine 'a' term: sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlong/2)
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(radLat1) * Math.cos(radLat2) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        // Step 3: Haversine 'c' term: 2 * atan2(√a, √(1 - a))
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        // Step 4: Multiply by Earth radius (6371.0 km) to obtain surface distance
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Pulls all AVAILABLE listings, computes the distance to the NGO's registered location,
     * tags items <= 10km as in-range, and sorts the list ascending by distance.
     * Also automatically performs an expiry check prior to querying.
     *
     * @param ngoLocationId Registered location ID of the querying NGO
     * @return List of NearbyListingDTO sorted nearest-first
     * @throws SQLException on database error
     */
    public List<NearbyListingDTO> getNearbyListingsForNgo(int ngoLocationId) throws SQLException {
        // 1. Fetch NGO's location coordinates
        Location ngoLocation = locationDAO.getLocationById(ngoLocationId);
        if (ngoLocation == null) {
            throw new IllegalArgumentException("NGO location not found for ID: " + ngoLocationId);
        }

        // 2. Perform automated expiry cleanup before fetching
        foodListingDAO.expireOutdatedListings();

        // 3. Retrieve all currently available listings
        List<FoodListingDAO.RawAvailableListingRow> rawRows = foodListingDAO.findAvailableListingsWithDetails();
        List<NearbyListingDTO> nearbyList = new ArrayList<>();

        // 4. Calculate distance for each listing and tag IN RANGE
        for (FoodListingDAO.RawAvailableListingRow row : rawRows) {
            double distance = calculateDistance(
                ngoLocation.getLatitude(),
                ngoLocation.getLongitude(),
                row.location.getLatitude(),
                row.location.getLongitude()
            );

            // Round distance to 2 decimal places for clean display
            double roundedDistance = Math.round(distance * 100.0) / 100.0;
            boolean inRange = roundedDistance <= IN_RANGE_THRESHOLD_KM;

            NearbyListingDTO dto = new NearbyListingDTO(
                row.listing,
                row.location,
                roundedDistance,
                inRange,
                row.restaurantName,
                row.restaurantPhone
            );
            nearbyList.add(dto);
        }

        // 5. Sort ascending by distance (nearest first)
        nearbyList.sort(Comparator.comparingDouble(NearbyListingDTO::getDistanceKm));

        return nearbyList;
    }
}
