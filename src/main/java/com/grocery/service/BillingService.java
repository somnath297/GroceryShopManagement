package com.grocery.service;

import com.grocery.dao.BillDAO;
import com.grocery.dao.SettingsDAO;
import com.grocery.model.Bill;
import com.grocery.model.BillItem;

import java.sql.SQLException;

public class BillingService {

    private final BillDAO billDAO = new BillDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    /**
     * Peek at the next bill number WITHOUT incrementing the counter.
     * Safe to call repeatedly — does NOT waste bill numbers.
     */
    public String peekNextBillNumber() throws SQLException {
        return billDAO.peekNextBillNumber();
    }

    /**
     * Generate the next bill number (increments counter).
     * Call this ONLY when actually saving a bill.
     */
    public String generateBillNumber() throws SQLException {
        return billDAO.generateBillNumber();
    }


    /**
     * Complete a sale: save the bill, decrement stock, record inventory OUT.
     * @return generated bill ID
     */
    public int completeSale(Bill bill) throws SQLException {
        // Ensure totals are up to date
        bill.recalculate();
        return billDAO.saveBill(bill);
    }

    /** Get the configured GST rate (0 by default). */
    public double getDefaultGstRate() {
        try {
            return Double.parseDouble(settingsDAO.get("gst_rate", "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Get the shop currency symbol. */
    public String getCurrency() {
        try {
            return settingsDAO.get("currency", "₹");
        } catch (Exception e) {
            return "₹";
        }
    }
}
