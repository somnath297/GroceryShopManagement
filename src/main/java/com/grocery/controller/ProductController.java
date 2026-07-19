package com.grocery.controller;

import com.grocery.dao.ProductDAO;
import com.grocery.dao.SupplierDAO;
import com.grocery.model.Product;
import com.grocery.model.Supplier;
import com.grocery.util.AlertUtil;
import com.grocery.util.CurrencyUtil;
import com.grocery.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ProductController {

    // Table
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String>  colName;
    @FXML private TableColumn<Product, String>  colCategory;
    @FXML private TableColumn<Product, String>  colBarcode;
    @FXML private TableColumn<Product, Double>  colPurchase;
    @FXML private TableColumn<Product, Double>  colSelling;
    @FXML private TableColumn<Product, Double>  colQty;
    @FXML private TableColumn<Product, String>  colUnit;
    @FXML private TableColumn<Product, String>  colSupplier;
    @FXML private TextField searchField;

    // Form fields
    @FXML private TextField fldName;
    @FXML private TextField fldCategory;
    @FXML private TextField fldBarcode;
    @FXML private TextField fldPurchasePrice;
    @FXML private TextField fldSellingPrice;
    @FXML private TextField fldQuantity;
    @FXML private ComboBox<String> cmbUnit;
    @FXML private ComboBox<Supplier> cmbSupplier;
    @FXML private TextField fldLowStockThreshold;
    @FXML private Label formStatusLabel;
    @FXML private Button btnSave;
    @FXML private Button btnClear;
    @FXML private Button btnDelete;
    @FXML private Label formTitleLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private Product selectedProduct;

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        loadProducts();
        loadSuppliers();

        // Listen for table selection
        productTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                if (selected != null) populateForm(selected);
            });

        // Search listener
        searchField.textProperty().addListener((obs, o, n) -> handleSearch());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        colPurchase.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colSelling.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        // Currency formatting for price columns
        colPurchase.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });
        colSelling.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });

        // Highlight low-stock rows
        productTable.setRowFactory(tv -> new TableRow<>() {
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().remove("row-danger");
                getStyleClass().remove("row-warning");
                if (!empty && p != null && p.isLowStock()) {
                    getStyleClass().add("row-danger");
                }
            }
        });
    }

    private void setupForm() {
        cmbUnit.setItems(FXCollections.observableArrayList(
            "pcs", "kg", "g", "litre", "ml", "packet", "box", "dozen", "bag", "bottle"
        ));
        cmbUnit.setValue("pcs");
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);
    }

    private void loadProducts() {
        try {
            productTable.getItems().setAll(productDAO.findAll());
        } catch (Exception e) {
            AlertUtil.showError("Error", "Could not load products.");
        }
    }

    private void loadSuppliers() {
        try {
            List<Supplier> suppliers = supplierDAO.findAll();
            cmbSupplier.getItems().clear();
            cmbSupplier.getItems().add(null); // "no supplier" option
            cmbSupplier.getItems().addAll(suppliers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSearch() {
        try {
            String q = searchField.getText().trim();
            if (q.isEmpty()) {
                productTable.getItems().setAll(productDAO.findAll());
            } else {
                productTable.getItems().setAll(productDAO.search(q));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateForm(Product p) {
        selectedProduct = p;
        formTitleLabel.setText("Edit Product");
        fldName.setText(p.getName());
        fldCategory.setText(p.getCategory() != null ? p.getCategory() : "");
        fldBarcode.setText(p.getBarcode() != null ? p.getBarcode() : "");
        fldPurchasePrice.setText(String.valueOf(p.getPurchasePrice()));
        fldSellingPrice.setText(String.valueOf(p.getSellingPrice()));
        fldQuantity.setText(String.valueOf(p.getQuantity()));
        cmbUnit.setValue(p.getUnit() != null ? p.getUnit() : "pcs");
        fldLowStockThreshold.setText(String.valueOf(p.getLowStockThreshold()));

        // Select supplier
        if (p.getSupplierId() > 0) {
            cmbSupplier.getItems().stream()
                .filter(s -> s != null && s.getId() == p.getSupplierId())
                .findFirst()
                .ifPresent(cmbSupplier::setValue);
        } else {
            cmbSupplier.setValue(null);
        }
        btnDelete.setDisable(false);
    }

    @FXML
    private void handleSave(ActionEvent e) {
        String err = validateForm();
        if (err != null) { showStatus(err, true); return; }

        Product p = selectedProduct != null ? selectedProduct : new Product();
        fillFromForm(p);

        try {
            if (selectedProduct == null) {
                productDAO.insert(p);
                showStatus("Product added successfully.", false);
            } else {
                productDAO.update(p);
                showStatus("Product updated successfully.", false);
            }
            handleClear(null);
            loadProducts();
        } catch (Exception ex) {
            showStatus("Could not save product. Please try again.", true);
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleDelete(ActionEvent e) {
        if (selectedProduct == null) return;
        if (!AlertUtil.confirm("Delete Product",
                "Delete \"" + selectedProduct.getName() + "\"? This cannot be undone.")) return;
        try {
            productDAO.delete(selectedProduct.getId());
            AlertUtil.showSuccess("Deleted", "Product deleted successfully.");
            handleClear(null);
            loadProducts();
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Could not delete product. It may be used in existing bills.");
        }
    }

    @FXML
    private void handleClear(ActionEvent e) {
        selectedProduct = null;
        formTitleLabel.setText("Add New Product");
        fldName.clear(); fldCategory.clear(); fldBarcode.clear();
        fldPurchasePrice.clear(); fldSellingPrice.clear(); fldQuantity.clear();
        fldLowStockThreshold.setText("10");
        cmbUnit.setValue("pcs"); cmbSupplier.setValue(null);
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);
        productTable.getSelectionModel().clearSelection();
    }

    private String validateForm() {
        String e;
        if ((e = ValidationUtil.requireNonEmpty(fldName.getText(), "product name")) != null) return e;
        if ((e = ValidationUtil.requirePositiveDouble(fldPurchasePrice.getText(), "purchase price")) != null) return e;
        if ((e = ValidationUtil.requirePositiveDouble(fldSellingPrice.getText(), "selling price")) != null) return e;
        if ((e = ValidationUtil.requirePositiveDouble(fldQuantity.getText(), "quantity")) != null) return e;
        return null;
    }

    private void fillFromForm(Product p) {
        p.setName(fldName.getText().trim());
        p.setCategory(fldCategory.getText().trim());
        p.setBarcode(fldBarcode.getText().trim());
        p.setPurchasePrice(ValidationUtil.parseDouble(fldPurchasePrice.getText()));
        p.setSellingPrice(ValidationUtil.parseDouble(fldSellingPrice.getText()));
        p.setQuantity(ValidationUtil.parseDouble(fldQuantity.getText()));
        p.setUnit(cmbUnit.getValue());
        p.setLowStockThreshold(fldLowStockThreshold.getText().isBlank() ? 10 :
                ValidationUtil.parseDouble(fldLowStockThreshold.getText()));
        Supplier sup = cmbSupplier.getValue();
        p.setSupplierId(sup != null ? sup.getId() : 0);
    }

    private void showStatus(String msg, boolean isError) {
        formStatusLabel.setText(msg);
        formStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        formStatusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        formStatusLabel.setVisible(true);
    }
}
