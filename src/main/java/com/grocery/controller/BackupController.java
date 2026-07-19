package com.grocery.controller;

import com.grocery.dao.DatabaseManager;
import com.grocery.service.BackupService;
import com.grocery.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BackupController {

    @FXML private Label lblDbPath;
    @FXML private Label lblBackupDir;
    @FXML private Label statusLabel;
    @FXML private TableView<BackupRecord> backupTable;
    @FXML private TableColumn<BackupRecord, String> colFilename;
    @FXML private TableColumn<BackupRecord, String> colDate;
    @FXML private TableColumn<BackupRecord, String> colPath;

    private final BackupService backupService = new BackupService();

    @FXML
    public void initialize() {
        lblDbPath.setText(DatabaseManager.getDbPath());
        lblBackupDir.setText(DatabaseManager.getAppDir() + File.separator + "Backups");
        statusLabel.setVisible(false);

        setupTable();
        loadBackupHistory();
    }

    private void setupTable() {
        colFilename.setCellValueFactory(new PropertyValueFactory<>("filename"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("backupDate"));
        colPath.setCellValueFactory(new PropertyValueFactory<>("filePath"));
    }

    @FXML
    private void handleBackup(ActionEvent e) {
        if (!AlertUtil.confirm("Create Backup",
                "Create a backup of the database?\nThis will save a copy to the Backups folder.")) return;
        try {
            String path = backupService.createBackup();
            showStatus("Backup created successfully:\n" + path, false);
            loadBackupHistory();
        } catch (Exception ex) {
            showStatus("Backup failed: " + ex.getMessage(), true);
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleRestore(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File to Restore");
        chooser.setInitialDirectory(new File(DatabaseManager.getAppDir() + File.separator + "Backups"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Backup", "*.db"));

        Stage stage = (Stage) lblDbPath.getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) return;

        if (!AlertUtil.confirm("Restore Backup",
                "Restore from:\n" + selected.getName() +
                "\n\nWARNING: All current data will be replaced with the backup data. Continue?")) return;
        try {
            backupService.restoreBackup(selected);
            showStatus("Database restored successfully from: " + selected.getName(), false);
            loadBackupHistory();
        } catch (Exception ex) {
            showStatus("Restore failed: " + ex.getMessage(), true);
            AlertUtil.showError("Restore Failed", "Could not restore the backup. Please try again.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleOpenBackupFolder(ActionEvent e) {
        try {
            String dir = DatabaseManager.getAppDir() + File.separator + "Backups";
            new ProcessBuilder("explorer.exe", dir).start();
        } catch (Exception ex) {
            AlertUtil.showError("Error", "Could not open folder.");
        }
    }

    private void loadBackupHistory() {
        try {
            List<BackupRecord> records = new ArrayList<>();
            Connection conn = DatabaseManager.getInstance().getConnection();
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT filename, backup_date, file_path FROM Backups ORDER BY id DESC")) {
                while (rs.next()) {
                    records.add(new BackupRecord(
                        rs.getString("filename"),
                        rs.getString("backup_date"),
                        rs.getString("file_path")
                    ));
                }
            }
            backupTable.getItems().setAll(records);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
        statusLabel.setVisible(true);
    }

    // Simple inner record for the backup history table
    public static class BackupRecord {
        private final String filename;
        private final String backupDate;
        private final String filePath;

        public BackupRecord(String filename, String backupDate, String filePath) {
            this.filename = filename;
            this.backupDate = backupDate;
            this.filePath = filePath;
        }

        public String getFilename()   { return filename; }
        public String getBackupDate() { return backupDate; }
        public String getFilePath()   { return filePath; }
    }
}
