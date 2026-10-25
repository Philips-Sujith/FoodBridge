package com.foodbridge.service;

import com.foodbridge.dao.LocationDAO;
import com.foodbridge.dao.UserDAO;
import com.foodbridge.model.Location;
import com.foodbridge.model.Role;
import com.foodbridge.model.User;

import java.sql.SQLException;

/**
 * AuthService handles user registration, validation, and login authentication.
 */
public class AuthService {

    private final UserDAO userDAO;
    private final LocationDAO locationDAO;
    private final com.foodbridge.dao.ActivityLogDAO activityLogDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.locationDAO = new LocationDAO();
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    public AuthService(UserDAO userDAO, LocationDAO locationDAO) {
        this.userDAO = userDAO;
        this.locationDAO = locationDAO;
        this.activityLogDAO = new com.foodbridge.dao.ActivityLogDAO();
    }

    /**
     * Registers a new user in the system after validating inputs.
     * NGO users require a fixed base locationId (1-50).
     * RESTAURANT / HOTEL / CATERER users have nullable locationId (always stored as NULL).
     *
     * @param username Unique username
     * @param password Password
     * @param role NGO or RESTAURANT
     * @param name Organization or Restaurant display name
     * @param phoneNumber Contact phone number
     * @param locationId Selected locality ID (required for NGO, ignored/null for Restaurant)
     * @return Newly created User object
     * @throws IllegalArgumentException if validation fails or username is taken
     * @throws SQLException on database error
     */
    public User register(String username, String password, Role role, String name, 
                         String phoneNumber, Integer locationId) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (role == null) {
            throw new IllegalArgumentException("User role must be specified (NGO or RESTAURANT).");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        // Check if username already taken
        if (userDAO.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Username '" + username + "' is already registered. Please choose another.");
        }

        Integer effectiveLocationId = null;
        if (role == Role.NGO) {
            if (locationId == null || locationId <= 0) {
                throw new IllegalArgumentException("NGO registration requires selecting a base location.");
            }
            Location loc = locationDAO.getLocationById(locationId);
            if (loc == null) {
                throw new IllegalArgumentException("Invalid location ID: " + locationId + ". Must select from predefined list.");
            }
            effectiveLocationId = locationId;
        } else if (role == Role.RESTAURANT) {
            // Restaurants operate across various venues and select pickup location dynamically per listing
            effectiveLocationId = null;
        }

        String hashedPassword = PasswordHasher.hashPassword(password.trim());
        User newUser = new User(0, username.trim(), hashedPassword, role, name.trim(), phoneNumber.trim(), effectiveLocationId);
        int generatedId = userDAO.createUser(newUser);
        newUser.setUserId(generatedId);

        try {
            activityLogDAO.logActivity(generatedId, "USER_REGISTER", "Registered role: " + role.name() + " (" + username.trim() + ")");
        } catch (Exception ignored) {}

        return newUser;
    }

    /**
     * Authenticates a user by matching credentials.
     * Supports salted SHA-256 hashes and automatically upgrades legacy plain-text passwords.
     *
     * @param username Username
     * @param password Password
     * @return User object on successful authentication, null on invalid credentials
     * @throws SQLException on database error
     */
    public User login(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return null; // User does not exist
        }

        if (PasswordHasher.verifyPassword(password.trim(), user.getPassword())) {
            // Transparently upgrade legacy plain-text password to salted SHA-256
            if (!PasswordHasher.isHashed(user.getPassword())) {
                try {
                    String newHashed = PasswordHasher.hashPassword(password.trim());
                    userDAO.updatePassword(user.getUserId(), newHashed);
                    user.setPassword(newHashed);
                } catch (Exception ignored) {
                    // Do not block authentication if background upgrade fails
                }
            }

            try {
                activityLogDAO.logActivity(user.getUserId(), "USER_LOGIN", "User logged in: " + user.getUsername());
            } catch (Exception ignored) {}

            return user;
        }

        return null; // Password mismatch
    }

}

