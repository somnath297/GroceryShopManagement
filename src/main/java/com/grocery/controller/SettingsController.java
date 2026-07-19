package com.grocery.controller;

import com.grocery.dao.SettingsDAO;
import com.grocery.service.AuthService;
import com.grocery.util.AlertUtil;
import com.grocery.util.CurrencyUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SettingsController {

    @FXML private TextField fldShopName;
    @FXML private TextField fldOwnerName;
    @FXML private TextField fldAddress;
    @FXML private TextField fldPhone;
    @FXML private TextField fldGstNumber;
    @FXML private TextField fldCurrency;
    @FXML private TextField fldGstRate;
    @FXML private TextField fldLowStockThreshold;
    @FXML private Label settingsStatusLabel;

    @FXML private PasswordField fldOldPassword;
    @FXML private PasswordField fldNewPassword;
    @FXML private PasswordField fldConfirmPassword;
    @FXML private Label passwordStatusLabel;

    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        settingsStatusLabel.setVisible(false);
        passwordStatusLabel.setVisible(false);
        loadSettings();
    }

    private void loadSettings() {
        try {
            fldShopName.setText(settingsDAO.get("shop_name", ""));
            fldOwnerName.setText(settingsDAO.get("owner_name", ""));
            fldAddress.setText(settingsDAO.get("address", ""));
            fldPhone.setText(settingsDAO.get("phone", ""));
            fldGstNumber.setText(settingsDAO.get("gst_number", ""));
            fldCurrency.setText(settingsDAO.get("currency", "₹"));
            fldGstRate.setText(settingsDAO.get("gst_rate", "0"));
            fldLowStockThreshold.setText(settingsDAO.get("low_stock_threshold", "10"));
        } catch (Exception e) {
            AlertUtil.showError("Error", "Could not load settings.");
        }
    }

    @FXML
    private void handleSaveSettings(ActionEvent e) {
        try {
            settingsDAO.set("shop_name",           fldShopName.getText().trim());
            settingsDAO.set("owner_name",          fldOwnerName.getText().trim());
            settingsDAO.set("address",             fldAddress.getText().trim());
            settingsDAO.set("phone",               fldPhone.getText().trim());
            settingsDAO.set("gst_number",          fldGstNumber.getText().trim());
            settingsDAO.set("currency",            fldCurrency.getText().trim().isEmpty() ? "₹" : fldCurrency.getText().trim());
            settingsDAO.set("gst_rate",            fldGstRate.getText().trim());
            settingsDAO.set("low_stock_threshold", fldLowStockThreshold.getText().trim());

            // Apply currency change immediately
            CurrencyUtil.setSymbol(fldCurrency.getText().trim());

            showSettingsStatus("Settings saved successfully.", false);
        } catch (Exception ex) {
            showSettingsStatus("Could not save settings. Please try again.", true);
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent e) {
        String old    = fldOldPassword.getText();
        String newPwd = fldNewPassword.getText();
        String confirm = fldConfirmPassword.getText();

        if (old.isBlank())    { showPasswordStatus("Please enter your current password.", true); return; }
        if (newPwd.isBlank()) { showPasswordStatus("Please enter a new password.", true); return; }
        if (newPwd.length() < 6) { showPasswordStatus("New password must be at least 6 characters.", true); return; }
        if (!newPwd.equals(confirm)) { showPasswordStatus("New passwords do not match.", true); return; }

        try {
            boolean success = authService.changePassword(old, newPwd);
            if (success) {
                showPasswordStatus("Password changed successfully.", false);
                fldOldPassword.clear(); fldNewPassword.clear(); fldConfirmPassword.clear();
            } else {
                showPasswordStatus("Current password is incorrect.", true);
            }
        } catch (Exception ex) {
            showPasswordStatus("Could not change password. Please try again.", true);
        }
    }

    private void showSettingsStatus(String msg, boolean isError) {
        settingsStatusLabel.setText(msg);
        settingsStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        settingsStatusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        settingsStatusLabel.setVisible(true);
    }

    private void showPasswordStatus(String msg, boolean isError) {
        passwordStatusLabel.setText(msg);
        passwordStatusLabel.getStyleClass().removeAll("status-success", "status-error");
        passwordStatusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        passwordStatusLabel.setVisible(true);
    }
}
