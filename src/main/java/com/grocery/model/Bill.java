package com.grocery.model;

import java.util.ArrayList;
import java.util.List;

public class Bill {
    private int id;
    private String billNumber;
    private int customerId;
    private String customerName;
    private double subtotal;
    private double discount;
    private double gstRate;
    private double gstAmount;
    private double grandTotal;
    private String paymentMethod;
    private String billDate;
    private String notes;
    private List<BillItem> items;

    public Bill() {
        this.items = new ArrayList<>();
        this.paymentMethod = "Cash";
        this.gstRate = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getGstRate() { return gstRate; }
    public void setGstRate(double gstRate) { this.gstRate = gstRate; }

    public double getGstAmount() { return gstAmount; }
    public void setGstAmount(double gstAmount) { this.gstAmount = gstAmount; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBillDate() { return billDate; }
    public void setBillDate(String billDate) { this.billDate = billDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
    public void addItem(BillItem item) { this.items.add(item); }

    /** Recalculate totals from current items */
    public void recalculate() {
        subtotal = items.stream().mapToDouble(BillItem::getTotalPrice).sum();
        double afterDiscount = subtotal - discount;
        gstAmount = afterDiscount * (gstRate / 100.0);
        grandTotal = afterDiscount + gstAmount;
    }
}
