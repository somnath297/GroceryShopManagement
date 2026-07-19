package com.grocery.service;

import com.grocery.dao.DatabaseManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BackupService {

    /**
     * Create a backup copy of grocery.db in the Backups/ folder.
     * Returns the path of the created backup file.
     */
    public String createBackup() throws Exception {
        // Determine backup folder
        String backupDir = DatabaseManager.getAppDir() + File.separator + "Backups";
        new File(backupDir).mkdirs();

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        String backupFileName = "backup_" + dateStr + ".db";
        File backupFile = new File(backupDir, backupFileName);

        // If a file with the same date exists, add a timestamp suffix
        if (backupFile.exists()) {
            String ts = String.valueOf(System.currentTimeMillis());
            backupFileName = "backup_" + dateStr + "_" + ts + ".db";
            backupFile = new File(backupDir, backupFileName);
        }

        // Use SQLite backup API via a raw file copy (WAL mode is safe for hot-copy)
        File sourceFile = new File(DatabaseManager.getDbPath());
        Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Record backup in DB
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(String.format(
                "INSERT INTO Backups (filename, file_path) VALUES ('%s', '%s')",
                backupFileName.replace("'", "''"),
                backupFile.getAbsolutePath().replace("'", "''")
            ));
        }

        return backupFile.getAbsolutePath();
    }

    /**
     * Restore the database from a given backup file.
     * Closes the current connection, copies the backup over grocery.db, then reconnects.
     */
    public void restoreBackup(File backupFile) throws Exception {
        DatabaseManager.getInstance().close();

        File targetFile = new File(DatabaseManager.getDbPath());
        Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Reconnect
        DatabaseManager.getInstance().initialize();
    }
}
