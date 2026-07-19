package com.grocery.dao;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public String get(String key) throws SQLException {
        return get(key, "");
    }

    public String get(String key, String defaultValue) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT value FROM Settings WHERE key=?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("value") : defaultValue;
        }
    }

    public void set(String key, String value) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT OR REPLACE INTO Settings (key, value) VALUES (?,?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.execute();
        }
    }

    public Map<String, String> getAllSettings() throws SQLException {
        Map<String, String> map = new LinkedHashMap<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT key, value FROM Settings ORDER BY key")) {
            while (rs.next()) map.put(rs.getString("key"), rs.getString("value"));
        }
        return map;
    }
}
