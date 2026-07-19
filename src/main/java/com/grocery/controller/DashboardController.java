package com.grocery.controller;

import com.grocery.dao.BillDAO;
import com.grocery.dao.CustomerDAO;
import com.grocery.dao.ProductDAO;
import com.grocery.dao.SettingsDAO;
import com.grocery.model.Bill;
import com.grocery.model.Product;
import com.grocery.util.CurrencyUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;

import java.util.List;

public class DashboardController {

    @FXML private Label todaySalesLabel;
    @FXML private Label todayProfitLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label lowStockLabel;

    // Recent Bills table + search
    @FXML private TableView<Bill> recentBillsTable;
    @FXML private TableColumn<Bill, String> colBillNo;
    @FXML private TableColumn<Bill, String> colCustomer;
    @FXML private TableColumn<Bill, Double> colAmount;
    @FXML private TableColumn<Bill, String> colDate;
    @FXML private TableColumn<Bill, String> colPayment;
    @FXML private TextField billSearchField;

    // Low Stock table + search
    @FXML private TableView<Product> lowStockTable;
    @FXML private TableColumn<Product, String> colLsName;
    @FXML private TableColumn<Product, Double> colLsQty;
    @FXML private TableColumn<Product, String> colLsUnit;
    @FXML private TableColumn<Product, Double> colLsThreshold;
    @FXML private TextField lowStockSearchField;

    private final BillDAO billDAO = new BillDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // cached full lists for local filtering
    private List<Bill>    allRecentBills  = List.of();
    private List<Product> allLowStockItems = List.of();

    @FXML
    public void initialize() {
        setupTables();
        loadData();
    }

    private void setupTables() {
        // Recent bills table
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });

        // Low stock table
        colLsName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLsQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLsUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colLsThreshold.setCellValueFactory(new PropertyValueFactory<>("lowStockThreshold"));

        // Color low stock rows red
        lowStockTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().remove("row-danger");
                if (!empty && p != null && p.isLowStock()) {
                    getStyleClass().add("row-danger");
                }
            }
        });
    }

    private void loadData() {
        try {
            String currency = settingsDAO.get("currency", "₹");
            CurrencyUtil.setSymbol(currency);

            double todaySales  = billDAO.getTodaySales();
            double todayProfit = billDAO.getTodayProfit();
            int products  = productDAO.getTotalCount();
            int customers = customerDAO.getTotalCount();
            int lowStock  = productDAO.getLowStockCount();

            todaySalesLabel.setText(CurrencyUtil.format(todaySales));
            todayProfitLabel.setText(CurrencyUtil.format(todayProfit));
            totalProductsLabel.setText(String.valueOf(products));
            totalCustomersLabel.setText(String.valueOf(customers));
            lowStockLabel.setText(String.valueOf(lowStock));

            if (lowStock > 0) lowStockLabel.getStyleClass().add("badge-danger");

            allRecentBills  = billDAO.findRecent(200);
            allLowStockItems = productDAO.findLowStock();

            // Reset search boxes & show all
            if (billSearchField != null)     billSearchField.clear();
            if (lowStockSearchField != null) lowStockSearchField.clear();

            recentBillsTable.getItems().setAll(allRecentBills);
            lowStockTable.getItems().setAll(allLowStockItems);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Search handlers ───────────────────────────────────────────────────────

    @FXML
    private void handleBillSearch(KeyEvent e) {
        String q = billSearchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            recentBillsTable.getItems().setAll(allRecentBills);
        } else {
            var filtered = allRecentBills.stream()
                .filter(b ->
                    (b.getBillNumber() != null && b.getBillNumber().toLowerCase().contains(q)) ||
                    (b.getCustomerName() != null && b.getCustomerName().toLowerCase().contains(q)) ||
                    (b.getPaymentMethod() != null && b.getPaymentMethod().toLowerCase().contains(q)))
                .toList();
            recentBillsTable.getItems().setAll(filtered);
        }
    }

    @FXML
    private void handleLowStockSearch(KeyEvent e) {
        String q = lowStockSearchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            lowStockTable.getItems().setAll(allLowStockItems);
        } else {
            var filtered = allLowStockItems.stream()
                .filter(p ->
                    (p.getName() != null && p.getName().toLowerCase().contains(q)) ||
                    (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                .toList();
            lowStockTable.getItems().setAll(filtered);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
