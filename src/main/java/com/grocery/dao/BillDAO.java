package com.grocery.dao;

import com.grocery.model.Bill;
import com.grocery.model.BillItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── Bill CRUD ────────────────────────────────────────────────────────────

    public int saveBill(Bill bill) throws SQLException {
        String sql = """
            INSERT INTO Bills (bill_number, customer_id, customer_name, subtotal, discount,
                gst_rate, gst_amount, grand_total, payment_method, notes)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """;
        conn().setAutoCommit(false);
        try {
            int billId;
            try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, bill.getBillNumber());
                if (bill.getCustomerId() > 0) ps.setInt(2, bill.getCustomerId());
                else ps.setNull(2, Types.INTEGER);
                ps.setString(3, bill.getCustomerName());
                ps.setDouble(4, bill.getSubtotal());
                ps.setDouble(5, bill.getDiscount());
                ps.setDouble(6, bill.getGstRate());
                ps.setDouble(7, bill.getGstAmount());
                ps.setDouble(8, bill.getGrandTotal());
                ps.setString(9, bill.getPaymentMethod());
                ps.setString(10, bill.getNotes());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                billId = keys.next() ? keys.getInt(1) : -1;
            }

            // Save bill items
            String itemSql = """
                INSERT INTO BillItems (bill_id, product_id, product_name, quantity, unit, unit_price, total_price)
                VALUES (?,?,?,?,?,?,?)
            """;
            try (PreparedStatement ips = conn().prepareStatement(itemSql)) {
                for (BillItem item : bill.getItems()) {
                    ips.setInt(1, billId);
                    ips.setInt(2, item.getProductId());
                    ips.setString(3, item.getProductName());
                    ips.setDouble(4, item.getQuantity());
                    ips.setString(5, item.getUnit());
                    ips.setDouble(6, item.getUnitPrice());
                    ips.setDouble(7, item.getTotalPrice());
                    ips.addBatch();
                }
                ips.executeBatch();
            }

            // Decrease stock for each item
            String stockSql = "UPDATE Products SET quantity = quantity - ? WHERE id = ?";
            try (PreparedStatement sps = conn().prepareStatement(stockSql)) {
                for (BillItem item : bill.getItems()) {
                    sps.setDouble(1, item.getQuantity());
                    sps.setInt(2, item.getProductId());
                    sps.addBatch();
                }
                sps.executeBatch();
            }

            // Record inventory OUT transactions
            String invSql = """
                INSERT INTO Inventory (product_id, transaction_type, quantity, note)
                VALUES (?, 'OUT', ?, ?)
            """;
            try (PreparedStatement ivp = conn().prepareStatement(invSql)) {
                for (BillItem item : bill.getItems()) {
                    ivp.setInt(1, item.getProductId());
                    ivp.setDouble(2, item.getQuantity());
                    ivp.setString(3, "Bill #" + bill.getBillNumber());
                    ivp.addBatch();
                }
                ivp.executeBatch();
            }

            conn().commit();
            return billId;
        } catch (SQLException e) {
            conn().rollback();
            throw e;
        } finally {
            conn().setAutoCommit(true);
        }
    }

    public List<Bill> findRecent(int limit) throws SQLException {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM Bills ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBillRow(rs));
        }
        return list;
    }

    public List<Bill> findByDateRange(String from, String to) throws SQLException {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM Bills WHERE DATE(bill_date) BETWEEN ? AND ? ORDER BY bill_date DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBillRow(rs));
        }
        return list;
    }

    /** Search all bills by customer name or bill number */
    public List<Bill> search(String query) throws SQLException {
        List<Bill> list = new ArrayList<>();
        String q = "%" + query.toLowerCase() + "%";
        String sql = """
            SELECT * FROM Bills
            WHERE LOWER(customer_name) LIKE ? OR LOWER(bill_number) LIKE ?
            ORDER BY id DESC LIMIT 200
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBillRow(rs));
        }
        return list;
    }

    /** Search bills by customer name or bill number within a date range */
    public List<Bill> searchInDateRange(String query, String from, String to) throws SQLException {
        List<Bill> list = new ArrayList<>();
        String q = "%" + query.toLowerCase() + "%";
        String sql = """
            SELECT * FROM Bills
            WHERE DATE(bill_date) BETWEEN ? AND ?
            AND (LOWER(customer_name) LIKE ? OR LOWER(bill_number) LIKE ?)
            ORDER BY bill_date DESC
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from); ps.setString(2, to);
            ps.setString(3, q);   ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBillRow(rs));
        }
        return list;
    }

    public List<BillItem> findItemsByBillId(int billId) throws SQLException {
        List<BillItem> list = new ArrayList<>();
        String sql = "SELECT * FROM BillItems WHERE bill_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BillItem item = new BillItem();
                item.setId(rs.getInt("id"));
                item.setBillId(rs.getInt("bill_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getDouble("quantity"));
                item.setUnit(rs.getString("unit"));
                item.setUnitPrice(rs.getDouble("unit_price"));
                item.setTotalPrice(rs.getDouble("total_price"));
                list.add(item);
            }
        }
        return list;
    }

    // ── Dashboard stats ──────────────────────────────────────────────────────

    public double getTodaySales() throws SQLException {
        String sql = "SELECT COALESCE(SUM(grand_total),0) FROM Bills WHERE DATE(bill_date)=DATE('now','localtime')";
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.getDouble(1);
        }
    }

    public double getTodayProfit() throws SQLException {
        String sql = """
            SELECT COALESCE(SUM((bi.unit_price - p.purchase_price) * bi.quantity), 0)
            FROM BillItems bi
            JOIN Bills b ON bi.bill_id = b.id
            JOIN Products p ON bi.product_id = p.id
            WHERE DATE(b.bill_date) = DATE('now','localtime')
        """;
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.getDouble(1);
        }
    }

    /**
     * Peek at the next bill number WITHOUT incrementing the counter.
     * Use this for display only. Call generateBillNumber() only when actually saving.
     */
    public String peekNextBillNumber() throws SQLException {
        String sql = "SELECT value FROM Settings WHERE key='bill_counter'";
        int counter = 1;
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) counter = Integer.parseInt(rs.getString("value"));
        }
        return String.format("BILL-%05d", counter);
    }

    /**
     * Generate the next bill number AND increment the counter in Settings.
     * Call this ONLY when the bill is actually being saved to the database.
     */
    public String generateBillNumber() throws SQLException {
        String sql = "SELECT value FROM Settings WHERE key='bill_counter'";
        int counter = 1;
        try (Statement s = conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) counter = Integer.parseInt(rs.getString("value"));
        }
        // Increment counter only now (bill is being saved)
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE Settings SET value=? WHERE key='bill_counter'")) {
            ps.setString(1, String.valueOf(counter + 1));
            ps.execute();
        }
        return String.format("BILL-%05d", counter);
    }


    // ── Report queries ───────────────────────────────────────────────────────

    public double getSalesForPeriod(String from, String to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(grand_total),0) FROM Bills WHERE DATE(bill_date) BETWEEN ? AND ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from); ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            return rs.getDouble(1);
        }
    }

    public double getProfitForPeriod(String from, String to) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM((bi.unit_price - p.purchase_price) * bi.quantity), 0)
            FROM BillItems bi
            JOIN Bills b ON bi.bill_id = b.id
            JOIN Products p ON bi.product_id = p.id
            WHERE DATE(b.bill_date) BETWEEN ? AND ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from); ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            return rs.getDouble(1);
        }
    }

    public int getBillCountForPeriod(String from, String to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Bills WHERE DATE(bill_date) BETWEEN ? AND ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, from); ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            return rs.getInt(1);
        }
    }

    private Bill mapBillRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setBillNumber(rs.getString("bill_number"));
        b.setCustomerId(rs.getInt("customer_id"));
        b.setCustomerName(rs.getString("customer_name"));
        b.setSubtotal(rs.getDouble("subtotal"));
        b.setDiscount(rs.getDouble("discount"));
        b.setGstRate(rs.getDouble("gst_rate"));
        b.setGstAmount(rs.getDouble("gst_amount"));
        b.setGrandTotal(rs.getDouble("grand_total"));
        b.setPaymentMethod(rs.getString("payment_method"));
        b.setBillDate(rs.getString("bill_date"));
        b.setNotes(rs.getString("notes"));
        return b;
    }
}
