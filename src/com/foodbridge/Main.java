package com.foodbridge;

import com.foodbridge.config.DBConnection;
import com.foodbridge.config.DatabaseSetup;
import com.foodbridge.ui.ConsoleUI;

/**
 * Main application entry point for FoodBridge.
 */
public class Main {

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8.name()));
        } catch (Exception ignored) {}

        System.out.println("===================================================================");
        System.out.println("       Starting FoodBridge (Excess Food Wastage Prevention System) ");
        System.out.println("===================================================================");

        try {
            // Initialize database schema and ensure 50 seed locations are present
            DatabaseSetup.initializeDatabase();
            if ("mysql".equalsIgnoreCase(DBConnection.getActiveMode())) {
                System.out.println("\nDatabase:   Aiven MySQL");
                System.out.println("Connection: Successful");
                System.out.println("Mode:       MYSQL");
                System.out.println("[INFO] Connected using: MYSQL database.");
            } else {
                System.out.println("\nDatabase:    Local H2");
                System.out.println("Cloud MySQL: Unavailable");
                System.out.println("Connection:  Using local fallback");
                System.out.println("Mode:        EMBEDDED");
                System.out.println("[INFO] Connected using: H2 local database.");
            }
            System.out.println("[INFO] JDBC URL: " + DBConnection.getActiveUrl());
        } catch (Exception e) {
            System.err.println("\n[FATAL] Application startup halted due to database connection failure.");
            System.err.println("[FATAL] FoodBridge requires a valid, reachable database to run.");
            System.exit(1);
            return;
        }

        // Start console UI loop
        ConsoleUI ui = new ConsoleUI();
        ui.start();
    }
}
