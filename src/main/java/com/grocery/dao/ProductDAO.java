package com.grocery.dao;

import com.grocery.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.*, s.name AS supplier_name
            FROM Products p
            LEFT JOIN Suppliers s ON p.supplier_id = s.id
            ORDER BY p.name
        """;
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Product> search(String query) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.*, s.name AS supplier_name
            FROM Products p
            LEFT JOIN Suppliers s ON p.supplier_id = s.id
            WHERE p.name LIKE ? OR p.barcode LIKE ? OR p.category LIKE ?
            ORDER BY p.name
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String q = "%" + query + "%";
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Product> findLowStock() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT p.*, s.name AS supplier_name
            FROM Products p
            LEFT JOIN Suppliers s ON p.supplier_id = s.id
            WHERE p.quantity <= p.low_stock_threshold
            ORDER BY p.quantity ASC
        """;
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Product findById(int id) throws SQLException {
        String sql = """
            SELECT p.*, s.name AS supplier_name
            FROM Products p
            LEFT JOIN Suppliers s ON p.supplier_id = s.id
            WHERE p.id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public int insert(Product p) throws SQLException {
        String sql = """
            INSERT INTO Products (name, category, barcode, purchase_price, selling_price,
                quantity, unit, supplier_id, low_stock_threshold)
            VALUES (?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, p);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public boolean update(Product p) throws SQLException {
        String sql = """
            UPDATE Products SET name=?, category=?, barcode=?, purchase_price=?,
                selling_price=?, quantity=?, unit=?, supplier_id=?, low_stock_threshold=?
            WHERE id=?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            setParams(ps, p);
            ps.setInt(10, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM Products WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateQuantity(int productId, double newQuantity) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE Products SET quantity=? WHERE id=?")) {
            ps.setDouble(1, newQuantity);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public int getTotalCount() throws SQLException {
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Products")) {
            return rs.getInt(1);
        }
    }

    public int getLowStockCount() throws SQLException {
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM Products WHERE quantity <= low_stock_threshold")) {
            return rs.getInt(1);
        }
    }

    private void setParams(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getName());
        ps.setString(2, p.getCategory());
        ps.setString(3, p.getBarcode());
        ps.setDouble(4, p.getPurchasePrice());
        ps.setDouble(5, p.getSellingPrice());
        ps.setDouble(6, p.getQuantity());
        ps.setString(7, p.getUnit());
        if (p.getSupplierId() > 0) ps.setInt(8, p.getSupplierId());
        else ps.setNull(8, Types.INTEGER);
        ps.setDouble(9, p.getLowStockThreshold());
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setBarcode(rs.getString("barcode"));
        p.setPurchasePrice(rs.getDouble("purchase_price"));
        p.setSellingPrice(rs.getDouble("selling_price"));
        p.setQuantity(rs.getDouble("quantity"));
        p.setUnit(rs.getString("unit"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setLowStockThreshold(rs.getDouble("low_stock_threshold"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }
}
