package com.grocery.app;

import com.grocery.dao.DatabaseManager;
import com.grocery.util.AlertUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Main JavaFX Application entry point.
 * Initialises the database on first launch and displays the login screen.
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try {
            // Initialise the embedded SQLite database (creates tables + seeds defaults on first run)
            DatabaseManager.getInstance().initialize();

            // Load the login screen
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 750);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm());

            stage.setTitle("Grocery Shop Management System");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(650);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            AlertUtil.showError("Startup Error",
                    "Failed to start the application. Please contact support.\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Clean up DB connection on exit
        DatabaseManager.getInstance().close();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
