package com.grocery.controller;

import com.grocery.dao.ProductDAO;
import com.grocery.dao.SupplierDAO;
import com.grocery.model.Product;
import com.grocery.model.Supplier;
import com.grocery.util.AlertUtil;
import com.grocery.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class SupplierController {

    @FXML private TableView<Supplier> supplierTable;
    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String>  colName;
    @FXML private TableColumn<Supplier, String>  colPhone;
    @FXML private TableColumn<Supplier, String>  colAddress;
    @FXML private TextField searchField;

    @FXML private TextField fldName;
    @FXML private TextField fldPhone;
    @FXML private TextField fldAddress;
    @FXML private Label formStatusLabel;
    @FXML private Button btnDelete;
    @FXML private Label formTitleLabel;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colProdName;
    @FXML private TableColumn<Product, String> colProdCategory;
    @FXML private TableColumn<Product, Double> colProdQty;
    @FXML private TableColumn<Product, String> colProdUnit;

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private Supplier selectedSupplier;

    @FXML
    public void initialize() {
        setupTables();
        loadSuppliers();
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);

        supplierTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, s) -> { if (s != null) populateForm(s); });
        searchField.textProperty().addListener((obs, o, n) -> handleSearch());
    }

    private void setupTables() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colProdUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
    }

    private void loadSuppliers() {
        try { supplierTable.getItems().setAll(supplierDAO.findAll()); }
        catch (Exception e) { AlertUtil.showError("Error", "Could not load suppliers."); }
    }

    private void handleSearch() {
        try {
            String q = searchField.getText().trim();
            supplierTable.getItems().setAll(q.isEmpty() ?
                supplierDAO.findAll() : supplierDAO.search(q));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateForm(Supplier s) {
        selectedSupplier = s;
        formTitleLabel.setText("Edit Supplier");
        fldName.setText(s.getName());
        fldPhone.setText(s.getPhone() != null ? s.getPhone() : "");
        fldAddress.setText(s.getAddress() != null ? s.getAddress() : "");
        btnDelete.setDisable(false);
        loadSuppliedProducts(s.getId());
    }

    private void loadSuppliedProducts(int supplierId) {
        try {
            List<Product> all = productDAO.findAll();
            productTable.getItems().setAll(
                all.stream().filter(p -> p.getSupplierId() == supplierId).toList());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleSave(ActionEvent e) {
        String err = ValidationUtil.requireNonEmpty(fldName.getText(), "supplier name");
        if (err != null) { showStatus(err, true); return; }

        Supplier s = selectedSupplier != null ? selectedSupplier : new Supplier();
        s.setName(fldName.getText().trim());
        s.setPhone(fldPhone.getText().trim());
        s.setAddress(fldAddress.getText().trim());

        try {
            if (selectedSupplier == null) {
                supplierDAO.insert(s);
                showStatus("Supplier added successfully.", false);
            } else {
                supplierDAO.update(s);
                showStatus("Supplier updated successfully.", false);
            }
            handleClear(null);
            loadSuppliers();
        } catch (Exception ex) {
            showStatus("Could not save supplier.", true);
        }
    }

    @FXML private void handleDelete(ActionEvent e) {
        if (selectedSupplier == null) return;
        if (!AlertUtil.confirm("Delete Supplier", "Delete \"" + selectedSupplier.getName() + "\"?")) return;
        try {
            supplierDAO.delete(selectedSupplier.getId());
            AlertUtil.showSuccess("Deleted", "Supplier deleted successfully.");
            handleClear(null);
            loadSuppliers();
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Could not delete supplier.");
        }
    }

    @FXML private void handleClear(ActionEvent e) {
        selectedSupplier = null;
        formTitleLabel.setText("Add New Supplier");
        fldName.clear(); fldPhone.clear(); fldAddress.clear();
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);
        productTable.getItems().clear();
        supplierTable.getSelectionModel().clearSelection();
    }

    private void showStatus(String msg, boolean isError) {
        formStatusLabel.setText(msg);
        formStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        formStatusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        formStatusLabel.setVisible(true);
    }
}
