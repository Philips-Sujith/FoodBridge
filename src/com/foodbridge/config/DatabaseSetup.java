package com.foodbridge.config;

import com.foodbridge.service.PasswordHasher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseSetup initializes the database schema and seeds the 50 predefined
 * localities across Chennai and Kanchipuram if they do not already exist.
 */
public class DatabaseSetup {

    private static final Object[][] SEED_LOCATIONS = new Object[][]{
        {"Kanchipuram", 12.8342, 79.7036},
        {"Guindy", 13.0067, 80.2026},
        {"Tambaram", 12.9249, 80.1000},
        {"Chrompet", 12.9516, 80.1462},
        {"Velachery", 12.9815, 80.2180},
        {"T. Nagar", 13.0418, 80.2341},
        {"Anna Nagar", 13.0850, 80.2101},
        {"Porur", 13.0382, 80.1565},
        {"Sriperumbudur", 12.9667, 79.9400},
        {"Adyar", 13.0012, 80.2565},
        {"Mylapore", 13.0368, 80.2676},
        {"Alwarpet", 13.0336, 80.2505},
        {"Nungambakkam", 13.0569, 80.2425},
        {"Egmore", 13.0732, 80.2609},
        {"Triplicane", 13.0587, 80.2757},
        {"Royapettah", 13.0524, 80.2625},
        {"Thiruvanmiyur", 12.9830, 80.2594},
        {"Besant Nagar", 13.0003, 80.2667},
        {"Sholinganallur", 12.9010, 80.2279},
        {"Thoraipakkam", 12.9382, 80.2312},
        {"Perungudi", 12.9654, 80.2461},
        {"Medavakkam", 12.9171, 80.1923},
        {"Pallavaram", 12.9675, 80.1491},
        {"Meenambakkam", 12.9880, 80.1770},
        {"Saidapet", 13.0213, 80.2231},
        {"Koyambedu", 13.0694, 80.1948},
        {"Vadapalani", 13.0500, 80.2121},
        {"Ashok Nagar", 13.0354, 80.2117},
        {"K.K. Nagar", 13.0410, 80.1994},
        {"Kodambakkam", 13.0519, 80.2255},
        {"West Mambalam", 13.0363, 80.2241},
        {"Chetpet", 13.0722, 80.2415},
        {"Kilpauk", 13.0784, 80.2422},
        {"Purasawalkam", 13.0903, 80.2562},
        {"Perambur", 13.1143, 80.2425},
        {"Madhavaram", 13.1488, 80.2306},
        {"Kolathur", 13.1242, 80.2125},
        {"Ambattur", 13.0983, 80.1614},
        {"Avadi", 13.1147, 80.1018},
        {"Poonamallee", 13.0474, 80.0945},
        {"Kundrathur", 12.9977, 80.0970},
        {"Mangadu", 13.0440, 80.1200},
        {"Iyyappanthangal", 13.0347, 80.1342},
        {"Manapakkam", 13.0189, 80.1758},
        {"Ramapuram", 13.0317, 80.1814},
        {"Chengalpattu", 12.6841, 79.9836},
        {"Maraimalai Nagar", 12.7950, 80.0242},
        {"Singaperumal Koil", 12.7634, 80.0039},
        {"Walajabad", 12.7964, 79.8175},
        {"Uthiramerur", 12.6133, 79.7561}
    };

    /**
     * Initializes database tables and verifies seed data.
     */
    public static void initializeDatabase() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("[SETUP] Connected to " + DBConnection.getActiveMode().toUpperCase() + " database: " + DBConnection.getActiveUrl());

            boolean locationsExist = tableExists(conn, "locations");
            boolean usersExist = tableExists(conn, "users");
            boolean listingsExist = tableExists(conn, "food_listings");
            boolean claimsExist = tableExists(conn, "claims");
            boolean notificationsExist = tableExists(conn, "notifications");
            boolean activityLogExist = tableExists(conn, "activity_log");

            if (!locationsExist || !usersExist || !listingsExist || !claimsExist) {
                System.out.println("[SETUP] Required tables missing. Creating database schema...");
                createSchema(conn);
            } else {
                // Ensure users.location_id is nullable if existing table had NOT NULL constraint
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users ALTER COLUMN location_id DROP NOT NULL");
                } catch (SQLException ignored) {
                    // Try MySQL syntax if H2 syntax fails
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE users MODIFY location_id INT NULL");
                    } catch (SQLException ignored2) {
                        // Ignored if already nullable
                    }
                }
            }

            // Ensure password column is large enough for salted hashes (VARCHAR 128)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ALTER COLUMN password VARCHAR(128)");
            } catch (SQLException ignored) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE users MODIFY password VARCHAR(128) NOT NULL");
                } catch (SQLException ignored2) {}
            }

            // Ensure notifications table exists
            if (!notificationsExist && !tableExists(conn, "notifications")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                                 "  notification_id INT PRIMARY KEY AUTO_INCREMENT," +
                                 "  user_id INT NOT NULL," +
                                 "  message VARCHAR(255) NOT NULL," +
                                 "  is_read BOOLEAN NOT NULL DEFAULT FALSE," +
                                 "  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                 "  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                                 ")");
                }
            }

            // Ensure activity_log table exists
            if (!activityLogExist && !tableExists(conn, "activity_log")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS activity_log (" +
                                 "  log_id INT PRIMARY KEY AUTO_INCREMENT," +
                                 "  user_id INT NULL," +
                                 "  action VARCHAR(50) NOT NULL," +
                                 "  details VARCHAR(255) NOT NULL," +
                                 "  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                 "  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                                 ")");
                }
            }


            // Ensure 50 locations exist
            seedLocationsIfEmpty(conn);

            // Ensure demo users exist for evaluator quick-login (Cloud MySQL and Embedded H2)
            ensureDemoUsers(conn);

            // Ensure small realistic demo dataset exists for offline demonstration
            seedDemoDataIfMissing(conn);

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize database: " + e.getMessage());
            throw e;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        try (ResultSet rs = meta.getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(catalog, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        // Direct query fallback: 100% reliable across all MySQL configurations
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1");
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static void createSchema(Connection conn) throws SQLException {
        File schemaFile = new File("sql/schema.sql");
        if (schemaFile.exists()) {
            executeSqlFile(conn, schemaFile);
        } else {
            executeBuiltinSchema(conn);
        }
    }

    private static void executeSqlFile(Connection conn, File file) throws SQLException {
        List<String> statements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                int commentIdx = line.indexOf("--");
                if (commentIdx != -1) {
                    line = line.substring(0, commentIdx).trim();
                }
                if (line.isEmpty()) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String stmtStr = sb.toString().trim();
                    if (stmtStr.endsWith(";")) {
                        stmtStr = stmtStr.substring(0, stmtStr.length() - 1).trim();
                    }
                    if (!stmtStr.isEmpty()) {
                        statements.add(stmtStr);
                    }
                    sb.setLength(0);
                }
            }
        } catch (IOException e) {
            System.err.println("[WARN] Could not read sql/schema.sql, using built-in DDL: " + e.getMessage());
            executeBuiltinSchema(conn);
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
        System.out.println("[SETUP] Schema created successfully from sql/schema.sql.");
    }

    private static void executeBuiltinSchema(Connection conn) throws SQLException {
        String[] ddl = new String[]{
            "CREATE TABLE IF NOT EXISTS locations (" +
            "  location_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  location_name VARCHAR(100) NOT NULL UNIQUE," +
            "  latitude DOUBLE NOT NULL," +
            "  longitude DOUBLE NOT NULL" +
            ")",

            "CREATE TABLE IF NOT EXISTS users (" +
            "  user_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  username VARCHAR(50) UNIQUE NOT NULL," +
            "  password VARCHAR(128) NOT NULL," +
            "  role VARCHAR(20) NOT NULL," +
            "  name VARCHAR(100) NOT NULL," +
            "  phone_number VARCHAR(15) NOT NULL," +
            "  location_id INT NULL," +
            "  CONSTRAINT fk_user_loc FOREIGN KEY (location_id) REFERENCES locations(location_id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS food_listings (" +
            "  listing_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  restaurant_id INT NOT NULL," +
            "  food_name VARCHAR(100) NOT NULL," +
            "  food_type VARCHAR(20) NOT NULL," +
            "  category VARCHAR(50) NOT NULL," +
            "  quantity_type VARCHAR(20) NOT NULL," +
            "  quantity_value INT NOT NULL," +
            "  location_id INT NOT NULL," +
            "  best_before TIMESTAMP NOT NULL," +
            "  posted_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'," +
            "  CONSTRAINT fk_list_rest FOREIGN KEY (restaurant_id) REFERENCES users(user_id)," +
            "  CONSTRAINT fk_list_loc FOREIGN KEY (location_id) REFERENCES locations(location_id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS claims (" +
            "  claim_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  listing_id INT NOT NULL," +
            "  ngo_id INT NOT NULL," +
            "  claimed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  CONSTRAINT fk_claim_list FOREIGN KEY (listing_id) REFERENCES food_listings(listing_id)," +
            "  CONSTRAINT fk_claim_user FOREIGN KEY (ngo_id) REFERENCES users(user_id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS notifications (" +
            "  notification_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  user_id INT NOT NULL," +
            "  message VARCHAR(255) NOT NULL," +
            "  is_read BOOLEAN NOT NULL DEFAULT FALSE," +
            "  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS activity_log (" +
            "  log_id INT PRIMARY KEY AUTO_INCREMENT," +
            "  user_id INT NULL," +
            "  action VARCHAR(50) NOT NULL," +
            "  details VARCHAR(255) NOT NULL," +
            "  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql);
            }
        }
        System.out.println("[SETUP] Built-in schema initialized successfully.");
    }

    private static void seedLocationsIfEmpty(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM locations";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }

        if (count < 50) {
            System.out.println("[SETUP] Seeding " + SEED_LOCATIONS.length + " Tamil Nadu locations into database...");
            String insertSql = "INSERT INTO locations (location_name, latitude, longitude) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Object[] loc : SEED_LOCATIONS) {
                    ps.setString(1, (String) loc[0]);
                    ps.setDouble(2, (Double) loc[1]);
                    ps.setDouble(3, (Double) loc[2]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            System.out.println("[SETUP] 50 seed locations inserted successfully.");
        }
    }

    /**
     * Seeds a small, realistic mock dataset (approximately 5 food listings) into the local H2 database
     * for instant demonstration.
     * IDEMPOTENT: If demo records already exist, no new records are inserted on subsequent startups.
     * CLOUD-SAFE: Only executes in embedded H2 mode, never touching cloud MySQL.
     */
    private static void seedDemoDataIfMissing(Connection conn) {
        if (!"embedded".equalsIgnoreCase(DBConnection.getActiveMode())) {
            // Keep cloud production database clean; only seed for local demonstration
            return;
        }

        try {
            // Check / create demo restaurant accounts (role = RESTAURANT, location = null)
            int rest1Id = ensureDemoUser(conn, "demo_restaurant_1", "Password@123", "RESTAURANT", "Annapoorna Caterers", "9840112233", null);
            int rest2Id = ensureDemoUser(conn, "demo_restaurant_2", "Password@123", "RESTAURANT", "Sangeetha Veg & Tiffin", "9840223344", null);
            int rest3Id = ensureDemoUser(conn, "demo_restaurant_3", "Password@123", "RESTAURANT", "Anjappar Banquet Hall", "9840334455", null);

            // Check / create demo NGO account for effortless testing (base location = Guindy)
            int guindyLocId = getAnyValidLocationId(conn);
            ensureDemoUser(conn, "demo_ngo", "Password@123", "NGO", "Chennai Care Shelter", "9840998811", guindyLocId);

            // Check if demo listings already exist for these demo restaurants
            String checkSql = "SELECT COUNT(*) FROM food_listings WHERE restaurant_id IN (?, ?, ?)";
            int existingDemoCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, rest1Id);
                ps.setInt(2, rest2Id);
                ps.setInt(3, rest3Id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingDemoCount = rs.getInt(1);
                    }
                }
            }

            if (existingDemoCount > 0) {
                // Already seeded! Ensure unexpired demo listings that are AVAILABLE stay valid
                // for seamless offline demonstration at any future date
                String refreshSql = "UPDATE food_listings SET best_before = ? WHERE restaurant_id IN (?, ?, ?) AND status = 'AVAILABLE' AND best_before <= ?";
                try (PreparedStatement ps = conn.prepareStatement(refreshSql)) {
                    ps.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusHours(12)));
                    ps.setInt(2, rest1Id);
                    ps.setInt(3, rest2Id);
                    ps.setInt(4, rest3Id);
                    ps.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    ps.executeUpdate();
                }
                return; // IDEMPOTENT: do not insert duplicate listings
            }

            // Look up location IDs from the existing 50-location dataset to ensure a realistic distance spread:
            // 2 locations [IN RANGE] (<= 10 km from Guindy/Saidapet) and 3 locations FARTHER (> 10 km)
            int velacheryId = getLocationIdByName(conn, "Velachery");         // ~3.25 km from Guindy [IN RANGE]
            int tnagarId = getLocationIdByName(conn, "T. Nagar");             // ~5.23 km from Guindy [IN RANGE]
            int tambaramId = getLocationIdByName(conn, "Tambaram");           // ~14.20 km from Guindy [FARTHER]
            int sriperumbudurId = getLocationIdByName(conn, "Sriperumbudur"); // ~28.80 km from Guindy [FARTHER]
            int kanchipuramId = getLocationIdByName(conn, "Kanchipuram");     // ~57.38 km from Guindy [FARTHER]

            if (velacheryId <= 0) velacheryId = 5;
            if (tnagarId <= 0) tnagarId = 6;
            if (tambaramId <= 0) tambaramId = 3;
            if (sriperumbudurId <= 0) sriperumbudurId = 9;
            if (kanchipuramId <= 0) kanchipuramId = 1;

            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            String insertListingSql = "INSERT INTO food_listings " +
                "(restaurant_id, food_name, food_type, category, quantity_type, quantity_value, location_id, best_before, posted_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')";

            try (PreparedStatement ps = conn.prepareStatement(insertListingSql)) {
                // 1. Idli & Sambar (Velachery - ~3.25 km, IN RANGE)
                ps.setInt(1, rest2Id);
                ps.setString(2, "Idli & Sambar");
                ps.setString(3, "VEG");
                ps.setString(4, "Breakfast");
                ps.setString(5, "SERVES_PEOPLE");
                ps.setInt(6, 30);
                ps.setInt(7, velacheryId);
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now.plusHours(6)));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(now.minusHours(1)));
                ps.addBatch();

                // 2. Vegetable Biryani (T. Nagar - ~5.23 km, IN RANGE)
                ps.setInt(1, rest1Id);
                ps.setString(2, "Vegetable Biryani");
                ps.setString(3, "VEG");
                ps.setString(4, "Cooked Meal");
                ps.setString(5, "BOXES");
                ps.setInt(6, 40);
                ps.setInt(7, tnagarId);
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now.plusHours(10)));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(now.minusHours(1)));
                ps.addBatch();

                // 3. Chicken Rice (Tambaram - ~14.20 km, Farther)
                ps.setInt(1, rest3Id);
                ps.setString(2, "Chicken Rice");
                ps.setString(3, "NON_VEG");
                ps.setString(4, "Cooked Meal");
                ps.setString(5, "BOXES");
                ps.setInt(6, 25);
                ps.setInt(7, tambaramId);
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now.plusHours(8)));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(now.minusHours(2)));
                ps.addBatch();

                // 4. Chapati & Vegetable Kurma (Sriperumbudur - ~28.80 km, Farther)
                ps.setInt(1, rest1Id);
                ps.setString(2, "Chapati & Vegetable Kurma");
                ps.setString(3, "VEG");
                ps.setString(4, "Cooked Meal");
                ps.setString(5, "SERVES_PEOPLE");
                ps.setInt(6, 45);
                ps.setInt(7, sriperumbudurId);
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now.plusHours(12)));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(now.minusHours(2)));
                ps.addBatch();

                // 5. Lemon Rice (Kanchipuram - ~57.38 km, Farther)
                ps.setInt(1, rest2Id);
                ps.setString(2, "Lemon Rice");
                ps.setString(3, "VEG");
                ps.setString(4, "Cooked Meal");
                ps.setString(5, "BOXES");
                ps.setInt(6, 35);
                ps.setInt(7, kanchipuramId);
                ps.setTimestamp(8, java.sql.Timestamp.valueOf(now.plusHours(14)));
                ps.setTimestamp(9, java.sql.Timestamp.valueOf(now.minusHours(3)));
                ps.addBatch();

                ps.executeBatch();
            }

            System.out.println("[SETUP] Seeded 5 realistic demo food listings for offline demonstration.");

        } catch (SQLException e) {
            System.err.println("[WARN] Could not seed demo data: " + e.getMessage());
        }
    }

    /**
     * Ensures demo users required for evaluator demonstration exist and have valid credentials.
     * IDEMPOTENT: Safe to call on every startup across all database modes (Cloud MySQL and Embedded H2).
     * If a demo user already exists, checks if the password matches; if not, updates it to salted SHA-256.
     * Ensures the demo NGO user has a valid base location for distance calculation.
     * Real users are never overwritten or deleted.
     */
    public static void ensureDemoUsers(Connection conn) {
        try {
            ensureDemoUser(conn, "demo_restaurant_1", "Password@123", "RESTAURANT", "Annapoorna Caterers", "9840112233", null);
            ensureDemoUser(conn, "demo_restaurant_2", "Password@123", "RESTAURANT", "Sangeetha Veg & Tiffin", "9840223344", null);
            ensureDemoUser(conn, "demo_restaurant_3", "Password@123", "RESTAURANT", "Anjappar Banquet Hall", "9840334455", null);

            int guindyLocId = getAnyValidLocationId(conn);
            ensureDemoUser(conn, "demo_ngo", "Password@123", "NGO", "Chennai Care Shelter", "9840998811", guindyLocId);
        } catch (SQLException e) {
            System.err.println("[SETUP WARN] Could not ensure demo users: " + e.getMessage());
        }
    }

    public static int ensureDemoUser(Connection conn, String username, String plainPassword, String role, String name, String phone, Integer locationId) throws SQLException {
        String selectSql = "SELECT user_id, password, location_id FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String currentStoredPass = rs.getString("password");
                    int currentLocationId = rs.getInt("location_id");
                    boolean wasNull = rs.wasNull();

                    boolean passwordMatches = PasswordHasher.verifyPassword(plainPassword, currentStoredPass);
                    boolean locationNeedsFix = ("NGO".equalsIgnoreCase(role) && (wasNull || currentLocationId <= 0 && locationId != null));

                    if (!passwordMatches && locationNeedsFix) {
                        String updateSql = "UPDATE users SET password = ?, location_id = ? WHERE user_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setString(1, PasswordHasher.hashPassword(plainPassword));
                            updatePs.setInt(2, locationId);
                            updatePs.setInt(3, userId);
                            updatePs.executeUpdate();
                        }
                    } else if (!passwordMatches) {
                        String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setString(1, PasswordHasher.hashPassword(plainPassword));
                            updatePs.setInt(2, userId);
                            updatePs.executeUpdate();
                        }
                    } else if (locationNeedsFix) {
                        String updateSql = "UPDATE users SET location_id = ? WHERE user_id = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setInt(1, locationId);
                            updatePs.setInt(2, userId);
                            updatePs.executeUpdate();
                        }
                    }
                    return userId;
                }
            }
        }

        // Demo user does not exist: insert fresh record with salted SHA-256 hash
        String insertSql = "INSERT INTO users (username, password, role, name, phone_number, location_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordHasher.hashPassword(plainPassword));
            ps.setString(3, role);
            ps.setString(4, name);
            ps.setString(5, phone);
            if (locationId != null && locationId > 0) {
                ps.setInt(6, locationId);
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private static int getAnyValidLocationId(Connection conn) {
        int locId = getLocationIdByName(conn, "Guindy");
        if (locId > 0) return locId;
        String sql = "SELECT location_id FROM locations LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ignored) {}
        return 1;
    }

    private static int getLocationIdByName(Connection conn, String name) {
        String sql = "SELECT location_id FROM locations WHERE location_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}
        return -1;
    }
}
