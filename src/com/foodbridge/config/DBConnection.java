package com.foodbridge.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DBConnection manages the JDBC connection lifecycle for FoodBridge.
 *
 * Supported Configurations:
 * 1. Centralized Cloud MySQL: Connects multiple client instances (e.g. Laptop A and Laptop B)
 *    to a shared remote MySQL server (AWS RDS, GCP Cloud SQL, Azure, Aiven, etc.).
 * 2. Local MySQL: Connects to a standard local MySQL server (localhost:3306).
 * 3. Embedded H2 Database: Zero-configuration relational fallback (running in MySQL mode)
 *    to ensure seamless local evaluation.
 *
 * Security Note:
 * In production deployments, database credentials should be injected via a secrets manager
 * (such as AWS Secrets Manager, GCP Secret Manager, or HashiCorp Vault) or secure environment variables,
 * and database passwords must never be committed to source repositories or logged to console.
 */
public class DBConnection {

    private static final Properties properties = new Properties();
    private static String activeMode = "mysql";
    private static String activeUrl;
    private static String activeUser;
    private static String activePassword;
    private static String activeDriver;
    private static boolean initialized = false;
    private static boolean connectedSuccessfully = false;
    private static String lastFailureCategory = null;
    private static String lastFailureReason = null;

    // Pattern to extract host, port, and database name from standard JDBC MySQL URLs
    private static final Pattern MYSQL_URL_PATTERN = Pattern.compile("jdbc:mysql://([^:/\\?]+)(?::(\\d+))?(?:/([^\\?]+))?");

    private DBConnection() {
        // Private constructor to prevent direct instantiation
    }

    /**
     * Initializes database settings by loading db.properties with optional environment variable overrides.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        // 1. Load properties file from root directory if present
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream is = new FileInputStream(propFile)) {
                properties.load(is);
            } catch (IOException e) {
                System.err.println("[WARN] Could not read db.properties, using built-in defaults: " + e.getMessage());
            }
        }

        // 2. Determine operating mode ('mysql' or 'embedded')
        activeMode = getPropertyOrEnv("db.mode", "FOODBRIDGE_DB_MODE", "mysql").trim().toLowerCase();
        boolean fallbackAllowed = Boolean.parseBoolean(
            getPropertyOrEnv("fallback.to.embedded", "FOODBRIDGE_FALLBACK_EMBEDDED", "true").trim()
        );

        if ("embedded".equals(activeMode)) {
            configureEmbedded();
            System.out.println("Database:   Embedded Relational (H2 in MySQL mode)");
            System.out.println("Connection: Testing...");
            Throwable embEx = testConnectionWithException(activeDriver, activeUrl, activeUser, activePassword);
            if (embEx == null) {
                connectedSuccessfully = true;
                System.out.println("[OK] Connected to embedded database.");
            } else {
                connectedSuccessfully = false;
                lastFailureCategory = "Embedded Database Error";
                lastFailureReason = sanitizeErrorMessage(embEx.getMessage());
                System.err.println("[ERROR] Failed to start embedded H2 database: " + lastFailureReason);
                throw new RuntimeException("Embedded database initialization failed: " + lastFailureReason, embEx);
            }
        } else {
            configureMySQL();

            // Check if placeholder is still present in the configured URL
            if (isPlaceholderConfigured(activeUrl)) {
                System.out.println("[WARN] Cloud database placeholder detected in db.properties (" + getSanitizedUrl(activeUrl) + ").");
                System.out.println("[WARN] To connect to your cloud database, replace YOUR_CLOUD_HOST/USER/PASSWORD with valid credentials.");
                if (fallbackAllowed) {
                    System.out.println("[INFO] Falling back to embedded relational database (H2 in MySQL mode) for this session.");
                    configureEmbedded();
                    connectedSuccessfully = true;
                } else {
                    connectedSuccessfully = false;
                    throw new RuntimeException("Cloud MySQL database is unconfigured. Please update db.properties with your cloud host credentials.");
                }
            } else {
                // Parse connection parameters for pre-flight diagnostics
                String[] details = parseJdbcDetails(activeUrl);
                String host = details[0];
                String port = details[1];
                String dbName = details[2];
                String sslMode = extractSslMode(activeUrl);
                boolean isCloud = !isLocalHost(host);
                String dbLabel = isCloud ? "Aiven MySQL" : "Local MySQL";

                System.out.println("Database:   " + dbLabel);
                System.out.println("Host:       " + host);
                System.out.println("Port:       " + port);
                System.out.println("SSL:        " + sslMode);
                System.out.println("Connection: Testing...");

                // Test connection to configured MySQL instance (local or remote/cloud)
                Throwable connEx = testConnectionWithException(activeDriver, activeUrl, activeUser, activePassword);
                if (connEx == null) {
                    connectedSuccessfully = true;
                    if (isCloud) {
                        System.out.println("[OK] Connected to Aiven MySQL.");
                        System.out.println("[INFO] Using cloud database.");
                    } else {
                        System.out.println("[OK] Connected to local MySQL database.");
                        System.out.println("[INFO] Using local database.");
                    }
                } else {
                    connectedSuccessfully = false;
                    String[] diag = categorizeFailure(connEx, host, port, dbName, activeUser);
                    lastFailureCategory = diag[0];
                    lastFailureReason = diag[1];

                    System.err.println("[ERROR] Cannot reach " + (isCloud ? "cloud" : "MySQL") + " database.");
                    System.err.println("[ERROR] Host: " + host);
                    System.err.println("[ERROR] Port: " + port);
                    System.err.println("[ERROR] Category: " + lastFailureCategory);
                    System.err.println("[ERROR] Reason: " + lastFailureReason);

                    if (fallbackAllowed) {
                        System.out.println("[WARN] Cloud MySQL unavailable.");
                        System.out.println("[INFO] Using local H2 database.");
                        configureEmbedded();
                        Throwable embEx = testConnectionWithException(activeDriver, activeUrl, activeUser, activePassword);
                        if (embEx == null) {
                            connectedSuccessfully = true;
                            System.out.println("[OK] Local database connected.");
                        } else {
                            connectedSuccessfully = false;
                            throw new RuntimeException("Failed to initialize fallback embedded H2 database: " + embEx.getMessage(), embEx);
                        }
                    } else {
                        System.err.println("[ERROR] Automatic fallback to embedded H2 is disabled (fallback.to.embedded=false).");
                        System.err.println("[ERROR] Centralized cloud database is required. FoodBridge cannot start.");
                        throw new RuntimeException("Cannot reach cloud database at " + host + ":" + port + " (" + lastFailureCategory + "): " + lastFailureReason);
                    }
                }
            }
        }

        // Verify driver availability
        try {
            Class.forName(activeDriver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Required JDBC Driver class not found on classpath: " + activeDriver, e);
        }

        initialized = true;
    }

    private static void configureMySQL() {
        activeMode = "mysql";
        activeDriver = getPropertyOrEnv("mysql.driver", "FOODBRIDGE_MYSQL_DRIVER", "com.mysql.cj.jdbc.Driver");
        activeUrl = getPropertyOrEnv("mysql.url", "FOODBRIDGE_DB_URL",
            "jdbc:mysql://localhost:3306/foodbridge?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        activeUser = getPropertyOrEnv("mysql.user", "FOODBRIDGE_DB_USER", "root");
        activePassword = getPropertyOrEnv("mysql.password", "FOODBRIDGE_DB_PASSWORD", "");
    }

    private static void configureEmbedded() {
        activeMode = "embedded";
        activeDriver = getPropertyOrEnv("embedded.driver", "FOODBRIDGE_EMBEDDED_DRIVER", "org.h2.Driver");
        activeUrl = getPropertyOrEnv("embedded.url", "FOODBRIDGE_EMBEDDED_URL",
            "jdbc:h2:./data/foodbridge;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
        activeUser = getPropertyOrEnv("embedded.user", "FOODBRIDGE_EMBEDDED_USER", "sa");
        activePassword = getPropertyOrEnv("embedded.password", "FOODBRIDGE_EMBEDDED_PASSWORD", "");

        // Ensure data directory exists for embedded H2 database
        try {
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        } catch (Exception ignored) {}
    }

    private static Throwable testConnectionWithException(String driver, String url, String user, String pass) {
        try {
            Class.forName(driver);
            DriverManager.setLoginTimeout(8); // Set 8-second timeout for quick failure detection
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                if (conn != null && !conn.isClosed()) {
                    return null; // Success
                }
                return new SQLException("Connection was returned null or closed.");
            }
        } catch (Throwable t) {
            return t;
        }
    }

    private static String[] categorizeFailure(Throwable t, String host, String port, String dbName, String user) {
        String category = "Communications Failure";
        String reason = (t != null && t.getMessage() != null) ? t.getMessage() : "Unknown communications link failure";

        // Inspect causes in the exception chain
        Throwable current = t;
        while (current != null) {
            String clsName = current.getClass().getName();
            String msg = (current.getMessage() != null) ? current.getMessage().toLowerCase() : "";

            // A. DNS Resolution Failure
            if (current instanceof java.net.UnknownHostException ||
                clsName.contains("UnknownHostException") ||
                msg.contains("unknownhostexception") ||
                msg.contains("no such host") ||
                msg.contains("name or service not known")) {
                category = "DNS Resolution Failure";
                reason = "DNS could not resolve hostname '" + host + "'. Please check internet connectivity and hostname spelling.";
                break;
            }

            // B. Connection Refused
            if (current instanceof java.net.ConnectException ||
                clsName.contains("ConnectException") ||
                msg.contains("connection refused") ||
                msg.contains("connectexception")) {
                category = "Connection Refused";
                reason = "Connection refused at " + host + ":" + port + ". The database service may be stopped, or port " + port + " is blocked by your local network/firewall.";
                break;
            }

            // C. Timeout
            if (current instanceof java.net.SocketTimeoutException ||
                clsName.contains("SocketTimeoutException") ||
                msg.contains("timed out") ||
                msg.contains("timeout")) {
                category = "Connection Timeout";
                reason = "Connection attempt timed out connecting to " + host + ":" + port + ". Network routing or firewall is dropping packets.";
                break;
            }

            // D. SSL Failure
            if (current instanceof javax.net.ssl.SSLException ||
                clsName.contains("SSL") ||
                msg.contains("ssl") ||
                msg.contains("handshake") ||
                msg.contains("certificate") ||
                msg.contains("pkix")) {
                category = "SSL/TLS Handshake Failure";
                reason = "SSL/TLS handshake failed. Verify SSL certificate configuration and sslMode parameter.";
                break;
            }

            // E. Authentication / Access Denied & F. Database/Schema Error
            if (current instanceof SQLException) {
                SQLException sqle = (SQLException) current;
                if ((sqle.getSQLState() != null && sqle.getSQLState().startsWith("28")) ||
                    sqle.getErrorCode() == 1045 ||
                    msg.contains("access denied")) {
                    category = "Authentication / Access Denied";
                    reason = "Access denied for user '" + user + "'. Please verify database credentials in db.properties.";
                    break;
                }
                if (sqle.getErrorCode() == 1049 || msg.contains("unknown database")) {
                    category = "Database / Schema Error";
                    reason = "Target database '" + dbName + "' does not exist on MySQL server.";
                    break;
                }
            }

            current = current.getCause();
        }

        return new String[]{category, sanitizeErrorMessage(reason)};
    }

    private static String[] parseJdbcDetails(String url) {
        String host = "unknown";
        String port = "3306";
        String dbName = "defaultdb";
        if (url != null) {
            Matcher m = MYSQL_URL_PATTERN.matcher(url);
            if (m.find()) {
                if (m.group(1) != null) host = m.group(1);
                if (m.group(2) != null) port = m.group(2);
                if (m.group(3) != null) dbName = m.group(3);
            }
        }
        return new String[]{host, port, dbName};
    }

    private static String extractSslMode(String url) {
        if (url == null) return "DISABLED";
        if (url.contains("sslMode=REQUIRED") || url.contains("sslMode=VERIFY_CA") || url.contains("sslMode=VERIFY_IDENTITY") || url.contains("useSSL=true")) {
            return "REQUIRED";
        }
        return "DISABLED";
    }

    private static String sanitizeErrorMessage(String msg) {
        if (msg == null) return "";
        return msg.replaceAll("(?i)(password=)[^&;\\s]*", "$1******")
                  .replaceAll("(?i)(password[:=]\\s*)[^\\s,]+", "$1******");
    }

    private static String getPropertyOrEnv(String propKey, String envKey, String defaultValue) {
        // Environment variables take precedence (standard 12-factor cloud configuration)
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }
        return properties.getProperty(propKey, defaultValue).trim();
    }

    private static boolean isPlaceholderConfigured(String url) {
        return url != null && (
            url.contains("YOUR_CLOUD_HOST") ||
            url.contains("YOUR_HOST") ||
            url.contains("YOUR_CLOUD_USERNAME")
        );
    }

    private static String extractHost(String jdbcUrl) {
        return parseJdbcDetails(jdbcUrl)[0];
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    /**
     * Sanitizes JDBC URLs to prevent password or credential leakage in logs.
     */
    public static String getSanitizedUrl(String url) {
        if (url == null) return "";
        // Mask passwords in query string (e.g. password=secret)
        return url.replaceAll("(?i)(password=)[^&;]*", "$1******");
    }

    /**
     * Establishes and returns a new JDBC Connection.
     * Callers must close this connection using try-with-resources.
     *
     * @return Active Connection to the configured relational database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            init();
        }
        if (!connectedSuccessfully) {
            throw new SQLException("Cannot establish database connection (" + 
                (lastFailureCategory != null ? lastFailureCategory : "Error") + "): " + 
                (lastFailureReason != null ? lastFailureReason : "Database initialization failed."));
        }
        return DriverManager.getConnection(activeUrl, activeUser, activePassword);
    }

    public static boolean isConnectedSuccessfully() {
        return connectedSuccessfully;
    }

    public static String getActiveMode() {
        if (!initialized) {
            init();
        }
        return activeMode;
    }

    public static String getActiveUrl() {
        if (!initialized) {
            init();
        }
        return getSanitizedUrl(activeUrl);
    }

    public static String getActiveDriver() {
        if (!initialized) {
            init();
        }
        return activeDriver;
    }

    /**
     * Allows forcing re-initialization (e.g. in test suites).
     */
    public static synchronized void reset() {
        initialized = false;
        connectedSuccessfully = false;
        lastFailureCategory = null;
        lastFailureReason = null;
    }
}
