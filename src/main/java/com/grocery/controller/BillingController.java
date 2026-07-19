package com.grocery.controller;

import com.grocery.dao.CustomerDAO;
import com.grocery.dao.ProductDAO;
import com.grocery.dao.SettingsDAO;
import com.grocery.model.Bill;
import com.grocery.model.BillItem;
import com.grocery.model.Customer;
import com.grocery.model.Product;
import com.grocery.service.BillingService;
import com.grocery.util.AlertUtil;
import com.grocery.util.CurrencyUtil;
import com.grocery.util.PrintUtil;
import com.grocery.util.ValidationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class BillingController {

    @FXML private TextField productSearchField;
    @FXML private ListView<Product> productSearchList;
    @FXML private TextField quantityField;

    @FXML private TableView<BillItem> cartTable;
    @FXML private TableColumn<BillItem, String> colProduct;
    @FXML private TableColumn<BillItem, Double> colQty;
    @FXML private TableColumn<BillItem, String> colUnit;
    @FXML private TableColumn<BillItem, Double> colRate;
    @FXML private TableColumn<BillItem, Double> colTotal;

    @FXML private ComboBox<Customer> cmbCustomer;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblGst;
    @FXML private Label lblGrandTotal;
    @FXML private TextField fldDiscount;
    @FXML private TextField fldGstRate;

    @FXML private ToggleGroup paymentGroup;
    @FXML private RadioButton rbCash;
    @FXML private RadioButton rbUpi;
    @FXML private RadioButton rbCard;

    @FXML private Label lblBillNumber;
    @FXML private Label lblBillDate;

    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BillingService billingService = new BillingService();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    private ObservableList<BillItem> cartItems = FXCollections.observableArrayList();
    private Product selectedProduct;
    private Bill currentBill;

    // Prevents search listener from firing when we set text programmatically
    private boolean suppressSearch = false;

    @FXML
    public void initialize() {
        setupCartTable();
        loadCustomers();
        initBillDisplay(); // Only peeks — does NOT increment counter

        // Product search listener — fires on every keystroke
        productSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSearch) return;
            searchProducts(newVal);
        });

        // ── Bug Fix: clicking a product fills search field + hides list + focuses qty ──
        productSearchList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldProduct, clickedProduct) -> {
                if (clickedProduct == null) return;
                selectedProduct = clickedProduct;

                suppressSearch = true;
                productSearchField.setText(clickedProduct.getName() +
                    "   [Stock: " + clickedProduct.getQuantity() + " " + clickedProduct.getUnit() +
                    " | Price: " + CurrencyUtil.format(clickedProduct.getSellingPrice()) + "]");
                suppressSearch = false;

                productSearchList.getItems().clear();
                productSearchList.setVisible(false);
                productSearchList.setManaged(false);

                Platform.runLater(() -> {
                    quantityField.requestFocus();
                    quantityField.selectAll();
                });
            }
        );

        fldDiscount.textProperty().addListener((obs, o, n) -> recalculate());
        fldGstRate.textProperty().addListener((obs, o, n) -> recalculate());

        double gst = billingService.getDefaultGstRate();
        fldGstRate.setText(String.valueOf(gst));
    }

    // ── Bug Fix: Only PEEKS at bill number — counter NOT incremented ──────────
    private void initBillDisplay() {
        try {
            String billNo = billingService.peekNextBillNumber();
            currentBill = new Bill();
            currentBill.setBillNumber(billNo);
            currentBill.setGstRate(billingService.getDefaultGstRate());
            lblBillNumber.setText("Bill #: " + billNo);
            lblBillDate.setText("Date: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupCartTable() {
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colRate.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        colRate.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });
        colTotal.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });

        cartTable.setItems(cartItems);

        cartTable.setRowFactory(tv -> {
            TableRow<BillItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    if (AlertUtil.confirm("Remove Item",
                            "Remove \"" + row.getItem().getProductName() + "\" from the bill?")) {
                        cartItems.remove(row.getItem());
                        recalculate();
                    }
                }
            });
            return row;
        });
    }

    private void loadCustomers() {
        try {
            List<Customer> customers = customerDAO.findAll();
            cmbCustomer.getItems().clear();
            cmbCustomer.getItems().add(null);
            cmbCustomer.getItems().addAll(customers);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void searchProducts(String query) {
        try {
            if (query == null || query.isBlank()) {
                productSearchList.getItems().clear();
                productSearchList.setVisible(false);
                productSearchList.setManaged(false);
                return;
            }
            List<Product> results = productDAO.search(query);
            if (results.isEmpty()) {
                productSearchList.getItems().clear();
                productSearchList.setVisible(false);
                productSearchList.setManaged(false);
            } else {
                productSearchList.getItems().setAll(results);
                productSearchList.setVisible(true);
                productSearchList.setManaged(true);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAddToCart(ActionEvent e) {
        if (selectedProduct == null) {
            AlertUtil.showWarning("No Product Selected",
                "Please search for a product and click on it to select it first.");
            return;
        }
        String qtyStr = quantityField.getText();
        String err = ValidationUtil.requirePositiveDouble(qtyStr, "quantity");
        if (err != null) { AlertUtil.showWarning("Invalid Quantity", err); return; }

        double qty = ValidationUtil.parseDouble(qtyStr);
        if (qty <= 0) { AlertUtil.showWarning("Invalid Quantity", "Quantity must be greater than zero."); return; }
        if (qty > selectedProduct.getQuantity()) {
            AlertUtil.showWarning("Insufficient Stock",
                "Only " + selectedProduct.getQuantity() + " " + selectedProduct.getUnit() +
                " of \"" + selectedProduct.getName() + "\" available.");
            return;
        }

        for (BillItem item : cartItems) {
            if (item.getProductId() == selectedProduct.getId()) {
                double newQty = item.getQuantity() + qty;
                if (newQty > selectedProduct.getQuantity()) {
                    AlertUtil.showWarning("Insufficient Stock", "Cannot add more than available stock.");
                    return;
                }
                item.setQuantity(newQty);
                cartTable.refresh();
                recalculate();
                clearProductSelection();
                return;
            }
        }

        BillItem item = new BillItem(
            selectedProduct.getId(),
            selectedProduct.getName(),
            qty,
            selectedProduct.getUnit(),
            selectedProduct.getSellingPrice()
        );
        cartItems.add(item);
        recalculate();
        clearProductSelection();
        Platform.runLater(() -> productSearchField.requestFocus());
    }

    private void recalculate() {
        double subtotal = cartItems.stream().mapToDouble(BillItem::getTotalPrice).sum();
        double discount = ValidationUtil.parseDouble(fldDiscount.getText());
        double gstRate  = ValidationUtil.parseDouble(fldGstRate.getText());
        double afterDiscount = Math.max(0, subtotal - discount);
        double gstAmt = afterDiscount * (gstRate / 100.0);
        double grandTotal = afterDiscount + gstAmt;

        lblSubtotal.setText(CurrencyUtil.format(subtotal));
        lblDiscount.setText("- " + CurrencyUtil.format(discount));
        lblGst.setText(CurrencyUtil.format(gstAmt) + " (" + gstRate + "%)");
        lblGrandTotal.setText(CurrencyUtil.format(grandTotal));
    }

    @FXML
    private void handleGenerateBill(ActionEvent e) {
        if (cartItems.isEmpty()) {
            AlertUtil.showWarning("Empty Cart", "Please add at least one product to the bill.");
            return;
        }

        // ── Bug Fix: Generate (increment) bill number ONLY at save time ──
        try {
            String actualBillNo = billingService.generateBillNumber();
            currentBill.setBillNumber(actualBillNo);
            lblBillNumber.setText("Bill #: " + actualBillNo);
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Could not generate bill number. Please try again.");
            return;
        }

        currentBill.getItems().clear();
        currentBill.getItems().addAll(cartItems);
        Customer customer = cmbCustomer.getValue();
        if (customer != null) {
            currentBill.setCustomerId(customer.getId());
            currentBill.setCustomerName(customer.getName());
        } else {
            currentBill.setCustomerName("Walk-in Customer");
        }
        currentBill.setDiscount(ValidationUtil.parseDouble(fldDiscount.getText()));
        currentBill.setGstRate(ValidationUtil.parseDouble(fldGstRate.getText()));

        RadioButton selectedPayment = (RadioButton) paymentGroup.getSelectedToggle();
        currentBill.setPaymentMethod(selectedPayment != null ? selectedPayment.getText() : "Cash");
        currentBill.setBillDate(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        currentBill.recalculate();

        try {
            int billId = billingService.completeSale(currentBill);
            currentBill.setId(billId);

            boolean doPrint = AlertUtil.confirm("Bill Saved ✅",
                "Bill #" + currentBill.getBillNumber() + " saved!\n" +
                "Grand Total: " + CurrencyUtil.format(currentBill.getGrandTotal()) +
                "\n\nPrint invoice?");
            if (doPrint) PrintUtil.printBill(currentBill);

            handleNewBill(null);
        } catch (Exception ex) {
            AlertUtil.showError("Bill Error", "Could not save the bill. Please try again.");
            ex.printStackTrace();
            currentBill.getItems().clear();
        }
    }

    @FXML
    private void handlePrint(ActionEvent e) {
        if (cartItems.isEmpty()) { AlertUtil.showWarning("Empty Cart", "No items to print."); return; }
        PrintUtil.printBill(buildPreviewBill());
    }

    @FXML
    private void handleSaveHtml(ActionEvent e) {
        if (cartItems.isEmpty()) { AlertUtil.showWarning("Empty Cart", "No items to save."); return; }
        Stage stage = (Stage) cartTable.getScene().getWindow();
        PrintUtil.saveAsHtml(buildPreviewBill(), stage);
    }

    @FXML
    private void handleNewBill(ActionEvent e) {
        cartItems.clear();
        cmbCustomer.setValue(null);
        fldDiscount.setText("0");
        fldGstRate.setText(String.valueOf(billingService.getDefaultGstRate()));
        clearProductSelection();
        recalculate();
        initBillDisplay(); // Only peeks — counter NOT incremented
    }

    @FXML
    private void handleRemoveSelected(ActionEvent e) {
        BillItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cartItems.remove(selected);
            recalculate();
        }
    }

    private void clearProductSelection() {
        selectedProduct = null;
        suppressSearch = true;
        productSearchField.clear();
        suppressSearch = false;
        productSearchList.getItems().clear();
        productSearchList.setVisible(false);
        productSearchList.setManaged(false);
        quantityField.setText("1");
    }

    private Bill buildPreviewBill() {
        Bill b = new Bill();
        b.setBillNumber(currentBill.getBillNumber());
        b.setCustomerName(cmbCustomer.getValue() != null ?
            cmbCustomer.getValue().getName() : "Walk-in Customer");
        b.getItems().addAll(cartItems);
        b.setDiscount(ValidationUtil.parseDouble(fldDiscount.getText()));
        b.setGstRate(ValidationUtil.parseDouble(fldGstRate.getText()));
        b.setBillDate(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        RadioButton rb = (RadioButton) paymentGroup.getSelectedToggle();
        b.setPaymentMethod(rb != null ? rb.getText() : "Cash");
        b.recalculate();
        return b;
    }
}
