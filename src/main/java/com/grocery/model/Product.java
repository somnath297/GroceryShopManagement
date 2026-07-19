package com.grocery.model;

public class Product {
    private int id;
    private String name;
    private String category;
    private String barcode;
    private double purchasePrice;
    private double sellingPrice;
    private double quantity;
    private String unit;
    private int supplierId;
    private String supplierName;
    private double lowStockThreshold;
    private String createdAt;

    public Product() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public double getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(double lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() { return quantity <= lowStockThreshold; }

    public double getProfit() { return sellingPrice - purchasePrice; }

    @Override
    public String toString() { return name; }
}
