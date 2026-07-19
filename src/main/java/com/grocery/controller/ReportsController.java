package com.grocery.controller;

import com.grocery.dao.BillDAO;
import com.grocery.dao.SettingsDAO;
import com.grocery.model.Bill;
import com.grocery.util.AlertUtil;
import com.grocery.util.CurrencyUtil;
import com.grocery.util.PrintUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportsController {

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<String> cmbReportType;

    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalProfit;
    @FXML private Label lblBillCount;

    @FXML private TableView<Bill> reportTable;
    @FXML private TableColumn<Bill, String> colBillNo;
    @FXML private TableColumn<Bill, String> colCustomer;
    @FXML private TableColumn<Bill, Double> colAmount;
    @FXML private TableColumn<Bill, String> colDate;
    @FXML private TableColumn<Bill, String> colPayment;

    @FXML private TextField billSearchField;

    private final BillDAO billDAO = new BillDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    // Cached for live search filtering and printing
    private List<Bill> allBills = List.of();
    private double lastTotalSales  = 0;
    private double lastTotalProfit = 0;
    private int    lastBillCount   = 0;

    @FXML
    public void initialize() {
        setupTable();

        // ── Bug Fix: set dates FIRST, then action handler, then load ────────
        // If we set the action handler before setValue(), it fires applyPreset()
        // before dates are initialized, causing silent early return in loadReport().
        fromDate.setValue(LocalDate.now());
        toDate.setValue(LocalDate.now());

        // Populate report type dropdown WITHOUT action handler yet
        cmbReportType.getItems().addAll("Daily", "Weekly", "Monthly", "Yearly", "Custom");
        cmbReportType.setValue("Daily");

        // NOW set the handler — safe because dates are already set
        cmbReportType.setOnAction(e -> applyPreset());

        // Load the initial report
        loadReport();
    }

    private void setupTable() {
        colBillNo.setCellValueFactory(new PropertyValueFactory<>("billNumber"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("grandTotal"));
        colAmount.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : CurrencyUtil.format(v));
            }
        });
    }

    private void applyPreset() {
        LocalDate today = LocalDate.now();
        switch (cmbReportType.getValue()) {
            case "Daily"   -> { fromDate.setValue(today); toDate.setValue(today); }
            case "Weekly"  -> { fromDate.setValue(today.minusWeeks(1)); toDate.setValue(today); }
            case "Monthly" -> { fromDate.setValue(today.withDayOfMonth(1)); toDate.setValue(today); }
            case "Yearly"  -> { fromDate.setValue(today.withDayOfYear(1)); toDate.setValue(today); }
            case "Custom"  -> { /* user picks dates manually — don't auto-load */ return; }
        }
        loadReport();
    }

    @FXML
    private void handleGenerateReport(ActionEvent e) {
        // Validate that dates are selected
        if (fromDate.getValue() == null) {
            AlertUtil.showWarning("Date Required", "Please select a From Date.");
            return;
        }
        if (toDate.getValue() == null) {
            AlertUtil.showWarning("Date Required", "Please select a To Date.");
            return;
        }
        if (fromDate.getValue().isAfter(toDate.getValue())) {
            AlertUtil.showWarning("Invalid Date Range", "'From Date' cannot be after 'To Date'.");
            return;
        }
        loadReport();
    }

    private void loadReport() {
        if (fromDate.getValue() == null || toDate.getValue() == null) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String from = fromDate.getValue().format(fmt);
        String to   = toDate.getValue().format(fmt);

        try {
            String currency = settingsDAO.get("currency", "₹");
            CurrencyUtil.setSymbol(currency);

            lastTotalSales  = billDAO.getSalesForPeriod(from, to);
            lastTotalProfit = billDAO.getProfitForPeriod(from, to);
            lastBillCount   = billDAO.getBillCountForPeriod(from, to);

            lblTotalSales.setText(CurrencyUtil.format(lastTotalSales));
            lblTotalProfit.setText(CurrencyUtil.format(lastTotalProfit));
            lblBillCount.setText(String.valueOf(lastBillCount));

            allBills = billDAO.findByDateRange(from, to);

            // Reset search and show full list
            if (billSearchField != null) billSearchField.clear();
            reportTable.getItems().setAll(allBills);

        } catch (Exception ex) {
            AlertUtil.showError("Report Error",
                "Could not generate report.\nError: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Live search ────────────────────────────────────────────────────────────

    @FXML
    private void handleBillSearch(KeyEvent e) {
        String q = billSearchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            reportTable.getItems().setAll(allBills);
        } else {
            var filtered = allBills.stream()
                .filter(b ->
                    (b.getBillNumber()    != null && b.getBillNumber().toLowerCase().contains(q))    ||
                    (b.getCustomerName()  != null && b.getCustomerName().toLowerCase().contains(q))  ||
                    (b.getPaymentMethod() != null && b.getPaymentMethod().toLowerCase().contains(q)))
                .toList();
            reportTable.getItems().setAll(filtered);
        }
    }

    // ── Print Report (Bug Fix: actually prints HTML report now) ───────────────

    @FXML
    private void handlePrintReport(ActionEvent e) {
        if (allBills.isEmpty()) {
            AlertUtil.showWarning("No Data",
                "No bills found for the selected period.\nGenerate a report first.");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String from = fromDate.getValue().format(fmt);
        String to   = toDate.getValue().format(fmt);

        // Open the system print dialog with a styled HTML report
        PrintUtil.printReport(allBills, from, to, lastTotalSales, lastTotalProfit, lastBillCount);
    }
}
