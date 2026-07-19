package com.grocery.controller;

import com.grocery.app.MainApp;
import com.grocery.app.SessionManager;
import com.grocery.service.AuthService;
import com.grocery.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main shell controller — manages the sidebar navigation and content area.
 * Each module is a separate FXML loaded into the center content pane.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label userLabel;
    @FXML private Label shopNameLabel;

    // Sidebar nav buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnBilling;
    @FXML private Button btnProducts;
    @FXML private Button btnCustomers;
    @FXML private Button btnSuppliers;
    @FXML private Button btnInventory;
    @FXML private Button btnReports;
    @FXML private Button btnBackup;
    @FXML private Button btnSettings;

    private Button activeButton;

    @FXML
    public void initialize() {
        // Show current user
        userLabel.setText(SessionManager.getInstance().getCurrentUsername());

        // Load shop name from settings
        try {
            com.grocery.dao.SettingsDAO dao = new com.grocery.dao.SettingsDAO();
            shopNameLabel.setText(dao.get("shop_name", "Grocery Shop"));
        } catch (Exception ignored) {}

        // Load dashboard as default screen
        navigate("Dashboard.fxml", btnDashboard);
    }

    @FXML private void showDashboard(ActionEvent e)  { navigate("Dashboard.fxml",  btnDashboard);  }
    @FXML private void showBilling(ActionEvent e)    { navigate("Billing.fxml",    btnBilling);    }
    @FXML private void showProducts(ActionEvent e)   { navigate("Products.fxml",   btnProducts);   }
    @FXML private void showCustomers(ActionEvent e)  { navigate("Customers.fxml",  btnCustomers);  }
    @FXML private void showSuppliers(ActionEvent e)  { navigate("Suppliers.fxml",  btnSuppliers);  }
    @FXML private void showInventory(ActionEvent e)  { navigate("Inventory.fxml",  btnInventory);  }
    @FXML private void showReports(ActionEvent e)    { navigate("Reports.fxml",    btnReports);    }
    @FXML private void showBackup(ActionEvent e)     { navigate("Backup.fxml",     btnBackup);     }
    @FXML private void showSettings(ActionEvent e)   { navigate("Settings.fxml",   btnSettings);   }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (AlertUtil.confirm("Logout", "Are you sure you want to logout?")) {
            new AuthService().logout();
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/Login.fxml"));
                Scene scene = new Scene(loader.load(), 1100, 750);
                scene.getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm());
                Stage stage = MainApp.getPrimaryStage();
                stage.setMaximized(false);
                stage.setScene(scene);
                stage.setWidth(1100);
                stage.setHeight(750);
                stage.centerOnScreen();
            } catch (Exception ex) {
                AlertUtil.showError("Error", "Could not return to login screen.");
            }
        }
    }

    private void navigate(String fxmlFile, Button button) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxmlFile));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            // Highlight active nav button
            if (activeButton != null) activeButton.getStyleClass().remove("nav-btn-active");
            button.getStyleClass().add("nav-btn-active");
            activeButton = button;
        } catch (Exception e) {
            AlertUtil.showError("Navigation Error", "Could not load screen: " + fxmlFile);
            e.printStackTrace();
        }
    }

    /** Called by child controllers to switch screens programmatically */
    public void navigateTo(String fxmlFile) {
        Button btn = switch (fxmlFile) {
            case "Billing.fxml"    -> btnBilling;
            case "Products.fxml"   -> btnProducts;
            case "Dashboard.fxml"  -> btnDashboard;
            default -> btnDashboard;
        };
        navigate(fxmlFile, btn);
    }
}
