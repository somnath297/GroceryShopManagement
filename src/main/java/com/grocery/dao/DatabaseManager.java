package com.grocery.dao;

import java.io.File;
import java.security.MessageDigest;
import java.sql.*;

/**
 * Singleton database manager.
 * Creates grocery.db in %APPDATA%\GroceryShop\ on first launch,
 * creates all tables, and seeds the default admin user and settings.
 * The end user never needs to touch a database.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private static final String APP_DIR;
    private static final String DB_PATH;

    static {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home");
        }
        APP_DIR = appData + File.separator + "GroceryShop";
        DB_PATH = APP_DIR + File.separator + "grocery.db";
    }

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public static String getAppDir() { return APP_DIR; }
    public static String getDbPath() { return DB_PATH; }

    public Connection getConnection() { return connection; }

    // ── Initialisation ──────────────────────────────────────────────────────

    public void initialize() throws Exception {
        createDirectories();
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        createTables();
        seedDefaultData();
    }

    private void createDirectories() {
        new File(APP_DIR).mkdirs();
        new File(APP_DIR + File.separator + "Backups").mkdirs();
        new File(APP_DIR + File.separator + "Reports").mkdirs();
        new File(APP_DIR + File.separator + "Logs").mkdirs();
    }

    // ── Table creation ───────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    UNIQUE NOT NULL,
                    password_hash TEXT    NOT NULL,
                    role          TEXT    DEFAULT 'ADMIN',
                    created_at    TEXT    DEFAULT (datetime('now','localtime'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Suppliers (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    name       TEXT NOT NULL,
                    phone      TEXT,
                    address    TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Products (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    name                TEXT    NOT NULL,
                    category            TEXT,
                    barcode             TEXT,
                    purchase_price      REAL    DEFAULT 0,
                    selling_price       REAL    DEFAULT 0,
                    quantity            REAL    DEFAULT 0,
                    unit                TEXT    DEFAULT 'pcs',
                    supplier_id         INTEGER,
                    low_stock_threshold REAL    DEFAULT 10,
                    created_at          TEXT    DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (supplier_id) REFERENCES Suppliers(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Customers (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    name       TEXT NOT NULL,
                    mobile     TEXT,
                    address    TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Bills (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_number    TEXT    UNIQUE NOT NULL,
                    customer_id    INTEGER,
                    customer_name  TEXT,
                    subtotal       REAL    DEFAULT 0,
                    discount       REAL    DEFAULT 0,
                    gst_rate       REAL    DEFAULT 0,
                    gst_amount     REAL    DEFAULT 0,
                    grand_total    REAL    DEFAULT 0,
                    payment_method TEXT    DEFAULT 'Cash',
                    bill_date      TEXT    DEFAULT (datetime('now','localtime')),
                    notes          TEXT,
                    FOREIGN KEY (customer_id) REFERENCES Customers(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS BillItems (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_id      INTEGER NOT NULL,
                    product_id   INTEGER NOT NULL,
                    product_name TEXT    NOT NULL,
                    quantity     REAL    NOT NULL,
                    unit         TEXT,
                    unit_price   REAL    NOT NULL,
                    total_price  REAL    NOT NULL,
                    FOREIGN KEY (bill_id)    REFERENCES Bills(id),
                    FOREIGN KEY (product_id) REFERENCES Products(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Inventory (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id       INTEGER NOT NULL,
                    transaction_type TEXT    NOT NULL,
                    quantity         REAL    NOT NULL,
                    note             TEXT,
                    transaction_date TEXT    DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (product_id) REFERENCES Products(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Settings (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Backups (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    filename    TEXT NOT NULL,
                    backup_date TEXT DEFAULT (datetime('now','localtime')),
                    file_path   TEXT NOT NULL
                )
            """);
        }
    }

    // ── Seed data ────────────────────────────────────────────────────────────

    private void seedDefaultData() throws SQLException {
        // Default admin user (only if no users exist)
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Users")) {
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Users (username, password_hash, role) VALUES (?,?,?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, hashPassword("admin123"));
                    ps.setString(3, "ADMIN");
                    ps.execute();
                }
            }
        }

        // Default settings
        insertSettingIfAbsent("shop_name",           "My Grocery Shop");
        insertSettingIfAbsent("owner_name",          "Shop Owner");
        insertSettingIfAbsent("address",             "Village, District, State");
        insertSettingIfAbsent("phone",               "");
        insertSettingIfAbsent("gst_number",          "");
        insertSettingIfAbsent("currency",            "₹");
        insertSettingIfAbsent("gst_rate",            "0");
        insertSettingIfAbsent("low_stock_threshold", "10");
        insertSettingIfAbsent("backup_location",     APP_DIR + File.separator + "Backups");
        insertSettingIfAbsent("theme",               "dark");
        insertSettingIfAbsent("bill_counter",        "1");
        insertSettingIfAbsent("print_shop_name",     "true");
    }

    private void insertSettingIfAbsent(String key, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO Settings (key, value) VALUES (?,?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.execute();
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
