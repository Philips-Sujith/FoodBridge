-- ==========================================================
-- FoodBridge Database Schema and Seed Data
-- An Excess Food Wastage Prevention System
-- Compatible with MySQL and H2 (MySQL Mode)
-- ==========================================================

-- ----------------------------------------------------------
-- 1. locations
-- Predefined reference table with 50 real localities across
-- Chennai, Kanchipuram, and surrounding Tamil Nadu region.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS locations (
    location_id INT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(100) NOT NULL UNIQUE,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL
);

-- ----------------------------------------------------------
-- 2. users
-- Stores both NGO / Orphanage (Claimers) and
-- Restaurant / Hotel / Caterer (Publishers).
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    location_id INT NULL,
    CONSTRAINT fk_user_location FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

-- ----------------------------------------------------------
-- 3. food_listings
-- Surplus food posted by restaurants.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_listings (
    listing_id INT PRIMARY KEY AUTO_INCREMENT,
    restaurant_id INT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    food_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    quantity_type VARCHAR(20) NOT NULL,
    quantity_value INT NOT NULL,
    location_id INT NOT NULL,
    best_before TIMESTAMP NOT NULL,
    posted_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_listing_restaurant FOREIGN KEY (restaurant_id) REFERENCES users(user_id),
    CONSTRAINT fk_listing_location FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

-- ----------------------------------------------------------
-- 4. claims
-- Tracks pickups claimed by NGOs.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS claims (
    claim_id INT PRIMARY KEY AUTO_INCREMENT,
    listing_id INT NOT NULL,
    ngo_id INT NOT NULL,
    claimed_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_listing FOREIGN KEY (listing_id) REFERENCES food_listings(listing_id),
    CONSTRAINT fk_claim_ngo FOREIGN KEY (ngo_id) REFERENCES users(user_id)
);

-- ----------------------------------------------------------
-- 5. notifications
-- Real-time alerts for NGOs and Restaurants.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ----------------------------------------------------------
-- 6. activity_log
-- Audit log of platform events (listings, claims, logins).
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    action VARCHAR(50) NOT NULL,
    details VARCHAR(255) NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);


-- ----------------------------------------------------------
-- SEED DATA: 50 Real Localities in Chennai / Kanchipuram Belt
-- ----------------------------------------------------------
INSERT IGNORE INTO locations (location_name, latitude, longitude) VALUES
('Kanchipuram', 12.8342, 79.7036),
('Guindy', 13.0067, 80.2026),
('Tambaram', 12.9249, 80.1000),
('Chrompet', 12.9516, 80.1462),
('Velachery', 12.9815, 80.2180),
('T. Nagar', 13.0418, 80.2341),
('Anna Nagar', 13.0850, 80.2101),
('Porur', 13.0382, 80.1565),
('Sriperumbudur', 12.9667, 79.9400),
('Adyar', 13.0012, 80.2565),
('Mylapore', 13.0368, 80.2676),
('Alwarpet', 13.0336, 80.2505),
('Nungambakkam', 13.0569, 80.2425),
('Egmore', 13.0732, 80.2609),
('Triplicane', 13.0587, 80.2757),
('Royapettah', 13.0524, 80.2625),
('Thiruvanmiyur', 12.9830, 80.2594),
('Besant Nagar', 13.0003, 80.2667),
('Sholinganallur', 12.9010, 80.2279),
('Thoraipakkam', 12.9382, 80.2312),
('Perungudi', 12.9654, 80.2461),
('Medavakkam', 12.9171, 80.1923),
('Pallavaram', 12.9675, 80.1491),
('Meenambakkam', 12.9880, 80.1770),
('Saidapet', 13.0213, 80.2231),
('Koyambedu', 13.0694, 80.1948),
('Vadapalani', 13.0500, 80.2121),
('Ashok Nagar', 13.0354, 80.2117),
('K.K. Nagar', 13.0410, 80.1994),
('Kodambakkam', 13.0519, 80.2255),
('West Mambalam', 13.0363, 80.2241),
('Chetpet', 13.0722, 80.2415),
('Kilpauk', 13.0784, 80.2422),
('Purasawalkam', 13.0903, 80.2562),
('Perambur', 13.1143, 80.2425),
('Madhavaram', 13.1488, 80.2306),
('Kolathur', 13.1242, 80.2125),
('Ambattur', 13.0983, 80.1614),
('Avadi', 13.1147, 80.1018),
('Poonamallee', 13.0474, 80.0945),
('Kundrathur', 12.9977, 80.0970),
('Mangadu', 13.0440, 80.1200),
('Iyyappanthangal', 13.0347, 80.1342),
('Manapakkam', 13.0189, 80.1758),
('Ramapuram', 13.0317, 80.1814),
('Chengalpattu', 12.6841, 79.9836),
('Maraimalai Nagar', 12.7950, 80.0242),
('Singaperumal Koil', 12.7634, 80.0039),
('Walajabad', 12.7964, 79.8175),
('Uthiramerur', 12.6133, 79.7561);
