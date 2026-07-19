package com.grocery.controller;

import com.grocery.dao.BillDAO;
import com.grocery.dao.CustomerDAO;
import com.grocery.model.Bill;
import com.grocery.model.Customer;
import com.grocery.util.AlertUtil;
import com.grocery.util.CurrencyUtil;
import com.grocery.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class CustomerController {

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String>  colName;
    @FXML private TableColumn<Customer, String>  colMobile;
    @FXML private TableColumn<Customer, String>  colAddress;
    @FXML private TableColumn<Customer, String>  colDate;
    @FXML private TextField searchField;

    @FXML private TextField fldName;
    @FXML private TextField fldMobile;
    @FXML private TextField fldAddress;
    @FXML private Label formStatusLabel;
    @FXML private Button btnDelete;
    @FXML private Label formTitleLabel;

    @FXML private TableView<Bill> historyTable;
    @FXML private TableColumn<Bill, String> colBillNo;
    @FXML private TableColumn<Bill, Double> colAmount;
    @FXML private TableColumn<Bill, String> colBillDate;
    @FXML private TableColumn<Bill, String> colPayment;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BillDAO billDAO = new BillDAO();
    private Customer selectedCustomer;

    @FXML
    public void initialize() {
        setupTable();
        setupHistoryTable();
        loadCustomers();
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);

        customerTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, o, c) -> { if (c != null) populateForm(c); });
        searchField.textProperty().addListener((obs, o, n) -> handleSearch());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMobile.setCellValueFactory(new PropertyValueFactory<>("mobile"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void setupHistoryTable() {
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        colBillDate.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colAmount.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });
    }

    private void loadCustomers() {
        try { customerTable.getItems().setAll(customerDAO.findAll()); }
        catch (Exception e) { AlertUtil.showError("Error", "Could not load customers."); }
    }

    private void handleSearch() {
        try {
            String q = searchField.getText().trim();
            customerTable.getItems().setAll(q.isEmpty() ?
                customerDAO.findAll() : customerDAO.search(q));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateForm(Customer c) {
        selectedCustomer = c;
        formTitleLabel.setText("Edit Customer");
        fldName.setText(c.getName());
        fldMobile.setText(c.getMobile() != null ? c.getMobile() : "");
        fldAddress.setText(c.getAddress() != null ? c.getAddress() : "");
        btnDelete.setDisable(false);
        loadPurchaseHistory(c.getId());
    }

    private void loadPurchaseHistory(int customerId) {
        try {
            // Filter bills by customer ID from recent bills
            var allBills = billDAO.findRecent(200);
            var customerBills = allBills.stream()
                .filter(b -> b.getCustomerId() == customerId)
                .toList();
            historyTable.getItems().setAll(customerBills);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleSave(ActionEvent e) {
        String err = validate();
        if (err != null) { showStatus(err, true); return; }

        Customer c = selectedCustomer != null ? selectedCustomer : new Customer();
        c.setName(fldName.getText().trim());
        c.setMobile(fldMobile.getText().trim());
        c.setAddress(fldAddress.getText().trim());

        try {
            if (selectedCustomer == null) {
                customerDAO.insert(c);
                showStatus("Customer added successfully.", false);
            } else {
                customerDAO.update(c);
                showStatus("Customer updated successfully.", false);
            }
            handleClear(null);
            loadCustomers();
        } catch (Exception ex) {
            showStatus("Could not save customer. Please try again.", true);
        }
    }

    @FXML private void handleDelete(ActionEvent e) {
        if (selectedCustomer == null) return;
        if (!AlertUtil.confirm("Delete Customer", "Delete \"" + selectedCustomer.getName() + "\"?")) return;
        try {
            customerDAO.delete(selectedCustomer.getId());
            AlertUtil.showSuccess("Deleted", "Customer deleted successfully.");
            handleClear(null);
            loadCustomers();
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Could not delete customer.");
        }
    }

    @FXML private void handleClear(ActionEvent e) {
        selectedCustomer = null;
        formTitleLabel.setText("Add New Customer");
        fldName.clear(); fldMobile.clear(); fldAddress.clear();
        btnDelete.setDisable(true);
        formStatusLabel.setVisible(false);
        historyTable.getItems().clear();
        customerTable.getSelectionModel().clearSelection();
    }

    private String validate() {
        String e = ValidationUtil.requireNonEmpty(fldName.getText(), "customer name");
        if (e != null) return e;
        return ValidationUtil.requireMobile(fldMobile.getText());
    }

    private void showStatus(String msg, boolean isError) {
        formStatusLabel.setText(msg);
        formStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        formStatusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        formStatusLabel.setVisible(true);
    }
}
