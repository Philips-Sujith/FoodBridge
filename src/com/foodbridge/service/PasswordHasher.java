package com.foodbridge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility for cryptographically secure salted SHA-256 password hashing.
 * Provides transparent backward-compatibility with legacy plain-text passwords.
 */
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Prevent instantiation
    }

    /**
     * Generates a 16-byte random salt and computes a SHA-256 hash.
     * Format: "saltHex:hashHex" (32 hex salt chars : 64 hex hash chars).
     *
     * @param plainPassword Plain-text password to hash
     * @return Formatted salt and hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = bytesToHex(salt);
        String hashHex = computeHash(saltHex, plainPassword);
        return saltHex + ":" + hashHex;
    }

    /**
     * Verifies a plain-text password against a stored password.
     * Supports both salted SHA-256 hashes ("salt:hash") and legacy plain-text passwords.
     * Uses constant-time comparison to protect against timing attacks.
     *
     * @param plainPassword  The candidate password entered by the user
     * @param storedPassword The password string retrieved from the database
     * @return true if valid, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }

        if (isHashed(storedPassword)) {
            String[] parts = storedPassword.split(":");
            String saltHex = parts[0];
            String expectedHashHex = parts[1];
            String computedHashHex = computeHash(saltHex, plainPassword);
            return MessageDigest.isEqual(
                expectedHashHex.getBytes(StandardCharsets.UTF_8),
                computedHashHex.getBytes(StandardCharsets.UTF_8)
            );
        }

        // Backward-compatible fallback for legacy plain-text passwords
        return storedPassword.equals(plainPassword);
    }

    /**
     * Determines whether a stored password is in the salted SHA-256 format.
     *
     * @param storedPassword The password string to inspect
     * @return true if format is "saltHex(32):hashHex(64)", false otherwise
     */
    public static boolean isHashed(String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        String[] parts = storedPassword.split(":");
        return parts.length == 2 && parts[0].length() == 32 && parts[1].length() == 64;
    }

    private static String computeHash(String saltHex, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltHex.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
