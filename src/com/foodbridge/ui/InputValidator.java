package com.foodbridge.ui;

import com.foodbridge.model.FoodType;
import com.foodbridge.model.QuantityType;
import com.foodbridge.model.Role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Robust input reader utility.
 * Prevents application crashes on malformed inputs and provides clear re-prompting.
 */
public class InputValidator {

    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InputValidator(Scanner scanner) {
        this.scanner = scanner;
    }

    private String nextCleanLine() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().replace("\uFEFF", "").trim();
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextCleanLine();
            if (input == null) {
                return "";
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("  [!] Input cannot be blank. Please try again.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = nextCleanLine();
            if (input == null) {
                return min;
            }
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Value must be between %d and %d. Please try again.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid whole number.");
            }
        }
    }

    public int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = nextCleanLine();
            if (input == null) {
                return 1;
            }
            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("  [!] Number must be greater than zero. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid positive integer.");
            }
        }
    }

    public Role readRole(String prompt) {
        System.out.println("\n" + prompt);
        System.out.println("  +--------------------------------------------------------+");
        System.out.println("  |  [1]  NGO / Orphanage / Relief Org (Food Claimer)      |");
        System.out.println("  |  [2]  Restaurant / Hotel / Caterer (Food Publisher)    |");
        System.out.println("  +--------------------------------------------------------+");
        int choice = readInt("Select Role (1-2): ", 1, 2);
        return (choice == 1) ? Role.NGO : Role.RESTAURANT;
    }

    public FoodType readFoodType(String prompt) {
        System.out.println("\n" + prompt);
        System.out.println("  +--------------------------------------------------------+");
        System.out.println("  |  [1]  VEG     (Vegetarian Dishes)                      |");
        System.out.println("  |  [2]  NON_VEG (Non-Vegetarian Dishes)                  |");
        System.out.println("  +--------------------------------------------------------+");
        int choice = readInt("Select Food Type (1-2): ", 1, 2);
        return (choice == 1) ? FoodType.VEG : FoodType.NON_VEG;
    }

    public QuantityType readQuantityType(String prompt) {
        System.out.println("\n" + prompt);
        System.out.println("  +--------------------------------------------------------+");
        System.out.println("  |  [1]  Boxes         (Packaged containers)              |");
        System.out.println("  |  [2]  Serves People (Number of individual meals)       |");
        System.out.println("  +--------------------------------------------------------+");
        int choice = readInt("Select Quantity Unit (1-2): ", 1, 2);
        return (choice == 1) ? QuantityType.BOXES : QuantityType.SERVES_PEOPLE;
    }

    /**
     * Reads a future date-time. Supports either format 'yyyy-MM-dd HH:mm'
     * or shortcut '+N' for N hours from now.
     */
    public LocalDateTime readFutureDateTime(String prompt) {
        LocalDateTime now = LocalDateTime.now();
        while (true) {
            System.out.println(prompt);
            System.out.println("  Format options: 'yyyy-MM-dd HH:mm' (e.g., 2026-09-09 14:00)");
            System.out.println("  OR Quick shortcut: enter '+4' for 4 hours from now, '+12' for 12 hours from now");
            System.out.print("Enter Pickup Deadline: ");
            String input = scanner.nextLine().trim();

            if (input.startsWith("+")) {
                try {
                    int hours = Integer.parseInt(input.substring(1).trim());
                    if (hours > 0) {
                        return now.plusHours(hours);
                    }
                } catch (NumberFormatException ignored) {}
            }

            try {
                LocalDateTime ldt = LocalDateTime.parse(input, DATE_FORMATTER);
                if (ldt.isAfter(now)) {
                    return ldt;
                }
                System.out.println("  [!] The best-before time must be in the future! Current time: " + now.format(DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                System.out.println("  [!] Invalid format. Please use 'yyyy-MM-dd HH:mm' or '+hours' shortcut.");
            }
        }
    }

    public void pause() {
        System.out.print("\nPress [ENTER] to continue...");
        scanner.nextLine();
    }
}
