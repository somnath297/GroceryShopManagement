package com.grocery.controller;

import com.grocery.app.MainApp;
import com.grocery.model.User;
import com.grocery.service.AuthService;
import com.grocery.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        if (loadingSpinner != null) loadingSpinner.setVisible(false);

        // Allow Enter key to trigger login from the password field
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) handleLogin(null);
        });
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) passwordField.requestFocus();
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank()) {
            showError("Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password.isBlank()) {
            showError("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        try {
            User user = authService.login(username, password);
            if (user != null) {
                openMainWindow();
            } else {
                showError("Incorrect username or password. Please try again.");
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (Exception e) {
            AlertUtil.showError("Login Error", "Could not connect to the database. Please restart the application.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.close();
    }

    private void openMainWindow() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Main.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm());

        Stage stage = MainApp.getPrimaryStage();
        stage.setScene(scene);
        stage.setMaximized(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
