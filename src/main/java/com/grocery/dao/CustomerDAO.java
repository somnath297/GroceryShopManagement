package com.grocery.dao;

import com.grocery.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM Customers ORDER BY name")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Customer> search(String query) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customers WHERE name LIKE ? OR mobile LIKE ? ORDER BY name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String q = "%" + query + "%";
            ps.setString(1, q); ps.setString(2, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Customer findById(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM Customers WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public int insert(Customer c) throws SQLException {
        String sql = "INSERT INTO Customers (name, mobile, address) VALUES (?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getMobile());
            ps.setString(3, c.getAddress());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public boolean update(Customer c) throws SQLException {
        String sql = "UPDATE Customers SET name=?, mobile=?, address=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getMobile());
            ps.setString(3, c.getAddress());
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM Customers WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalCount() throws SQLException {
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Customers")) {
            return rs.getInt(1);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("mobile"),
            rs.getString("address"),
            rs.getString("created_at")
        );
    }
}
