package controller;

import model.AppConfig;
import model.DistanceReading;
import model.DistanceModel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Handles all database operations.
 *
 * Changes from previous version:
 *  - Added getConnection() so UserAuth can reuse the same Connection.
 */
public class DatabaseController implements DistanceModel.ReadingListener {

    private Connection connection;
    private boolean    connected = false;

    // ── Connect ────────────────────────────────────────────────────────────
    public boolean connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                    AppConfig.DB_URL,
                    AppConfig.DB_USER,
                    AppConfig.DB_PASSWORD
                );
                connected = true;
                System.out.println("✅ MySQL Connected");
            }
        } catch (Exception e) {
            connected = false;
            System.out.println("⚠ MySQL connection failed: " + e.getMessage());
        }
        return connected;
    }

    public boolean    isConnected()  { return connected; }

    /** Exposes the live connection for reuse by UserAuth. */
    public Connection getConnection() { return connection; }

    // ── ReadingListener — auto-save every new reading ──────────────────────
    @Override
    public void onNewReading(DistanceReading reading) {
        save(reading);
    }

    // ── Save a reading ─────────────────────────────────────────────────────
    public void save(DistanceReading reading) {
        if (!connected) return;
        try {
            String sql = "INSERT INTO distance_record(distance, status) VALUES (?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setDouble(1, reading.getDistance());
            stmt.setString(2, reading.getStatus());
            stmt.executeUpdate();
            System.out.println("✔ Data stored in MySQL");
        } catch (Exception e) {
            System.out.println("Database save error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Load recent records for table ──────────────────────────────────────
    public Object[][] loadRecentRecords(int limit) {
        if (!connected) return new Object[0][];
        try {
            String sql = "SELECT `timestamp`, distance, status " +
                         "FROM distance_record ORDER BY id DESC LIMIT " + limit;
            Statement stmt = connection.createStatement();
            ResultSet rs   = stmt.executeQuery(sql);

            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("timestamp"),
                    rs.getDouble("distance"),
                    rs.getString("status")
                });
            }
            return rows.toArray(new Object[0][]);
        } catch (Exception e) {
            e.printStackTrace();
            return new Object[0][];
        }
    }

    // ── Disconnect ─────────────────────────────────────────────────────────
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
                System.out.println("MySQL disconnected.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
