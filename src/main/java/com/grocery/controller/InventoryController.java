package com.grocery.controller;

import com.grocery.dao.DatabaseManager;
import com.grocery.dao.ProductDAO;
import com.grocery.model.Product;
import com.grocery.util.AlertUtil;
import com.grocery.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class InventoryController {

    @FXML private TableView<Product> stockTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colQty;
    @FXML private TableColumn<Product, String> colUnit;
    @FXML private TableColumn<Product, Double> colThreshold;
    @FXML private TextField searchField;
    @FXML private CheckBox cbLowStockOnly;

    @FXML private ComboBox<Product> cmbProduct;
    @FXML private TextField fldAddQty;
    @FXML private TextField fldNote;
    @FXML private Label statusLabel;

    @FXML private Label lblTotalProducts;
    @FXML private Label lblLowStockCount;
    @FXML private Label lblOutOfStock;

    private final ProductDAO productDAO = new ProductDAO();

    @FXML
    public void initialize() {
        setupTable();
        loadProducts();
        statusLabel.setVisible(false);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        cbLowStockOnly.selectedProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colThreshold.setCellValueFactory(new PropertyValueFactory<>("lowStockThreshold"));

        stockTable.setRowFactory(tv -> new TableRow<>() {
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().removeAll("row-danger", "row-warning");
                if (!empty && p != null) {
                    if (p.getQuantity() <= 0) getStyleClass().add("row-danger");
                    else if (p.isLowStock()) getStyleClass().add("row-warning");
                }
            }
        });
    }

    private void loadProducts() {
        try {
            var products = productDAO.findAll();
            stockTable.getItems().setAll(products);
            cmbProduct.getItems().setAll(products);

            long total = products.size();
            long low = products.stream().filter(Product::isLowStock).count();
            long out = products.stream().filter(p -> p.getQuantity() <= 0).count();

            lblTotalProducts.setText(String.valueOf(total));
            lblLowStockCount.setText(String.valueOf(low));
            lblOutOfStock.setText(String.valueOf(out));
        } catch (Exception e) {
            AlertUtil.showError("Error", "Could not load inventory.");
        }
    }

    private void applyFilter() {
        try {
            String q = searchField.getText().trim().toLowerCase();
            boolean lowOnly = cbLowStockOnly.isSelected();
            var products = productDAO.findAll();
            var filtered = products.stream()
                .filter(p -> q.isEmpty() || p.getName().toLowerCase().contains(q))
                .filter(p -> !lowOnly || p.isLowStock())
                .toList();
            stockTable.getItems().setAll(filtered);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAddStock(ActionEvent e) {
        Product product = cmbProduct.getValue();
        if (product == null) { showStatus("Please select a product.", true); return; }

        String err = ValidationUtil.requirePositiveDouble(fldAddQty.getText(), "quantity");
        if (err != null) { showStatus(err, true); return; }

        double qty = ValidationUtil.parseDouble(fldAddQty.getText());
        if (qty <= 0) { showStatus("Quantity must be greater than zero.", true); return; }

        try {
            double newQty = product.getQuantity() + qty;
            productDAO.updateQuantity(product.getId(), newQty);

            // Record inventory IN transaction
            String note = fldNote.getText().isBlank() ? "Stock added" : fldNote.getText().trim();
            Connection conn = DatabaseManager.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Inventory (product_id, transaction_type, quantity, note) VALUES (?,?,?,?)")) {
                ps.setInt(1, product.getId());
                ps.setString(2, "IN");
                ps.setDouble(3, qty);
                ps.setString(4, note);
                ps.execute();
            }

            showStatus("Stock updated! " + product.getName() + ": " +
                product.getQuantity() + " → " + newQty + " " + product.getUnit(), false);
            cmbProduct.setValue(null);
            fldAddQty.clear();
            fldNote.clear();
            loadProducts();
        } catch (Exception ex) {
            showStatus("Could not update stock. Please try again.", true);
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh(ActionEvent e) {
        loadProducts();
        showStatus("Inventory refreshed.", false);
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        statusLabel.setVisible(true);
    }
}
