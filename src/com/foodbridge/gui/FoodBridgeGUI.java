package com.foodbridge.gui;

import com.foodbridge.config.DBConnection;
import com.foodbridge.config.DatabaseSetup;
import com.foodbridge.model.Role;
import com.foodbridge.model.User;

import java.awt.EventQueue;

/**
 * Main launcher entry point for the FoodBridge AWT Graphical User Interface.
 * Pure Java AWT: Zero Swing, zero JavaFX, zero external GUI frameworks.
 */
public class FoodBridgeGUI {

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("       Starting FoodBridge Graphical Interface (Pure AWT)          ");
        System.out.println("===================================================================");

        // 1. Initialize database connection and schema
        try {
            DatabaseSetup.initializeDatabase();
            System.out.println("[GUI] Connected using " + DBConnection.getActiveMode().toUpperCase() + " mode.");
        } catch (Exception e) {
            System.err.println("[GUI WARN] Database setup encountered an error: " + e.getMessage());
        }

        // 2. Launch MainFrame on the AWT Event Dispatch Thread
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    MainFrame frame = new MainFrame();
                    frame.initScreens();
                    frame.setVisible(true);

                    System.out.println("[GUI] MainFrame (1000x650) launched successfully.");
                } catch (Exception ex) {
                    System.err.println("[GUI FATAL] Failed to launch MainFrame: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }
}
