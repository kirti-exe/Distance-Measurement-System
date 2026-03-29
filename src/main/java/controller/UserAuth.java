package controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * UserAuth — authenticates and registers users against the `users` table.
 * Passwords stored as SHA-256 hex. Reuses DatabaseController's connection.
 */
public class UserAuth {

    public enum RegisterResult { SUCCESS, USERNAME_TAKEN, DB_ERROR }

    private final DatabaseController dbController;

    public UserAuth(DatabaseController dbController) {
        this.dbController = dbController;
    }

    // ── Authenticate ───────────────────────────────────────────────────────
    public boolean authenticate(String username, String password) {
        if (!dbController.isConnected()) return false;
        try {
            Connection conn = dbController.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM users WHERE username = ? AND password = ? LIMIT 1");
            stmt.setString(1, username);
            stmt.setString(2, sha256(password));
            return stmt.executeQuery().next();
        } catch (Exception e) {
            System.out.println("Auth error: " + e.getMessage());
            return false;
        }
    }

    // ── Register ───────────────────────────────────────────────────────────
    public RegisterResult register(String username, String password) {
        if (!dbController.isConnected()) return RegisterResult.DB_ERROR;
        try {
            Connection conn = dbController.getConnection();
            // Check duplicate
            PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM users WHERE username = ? LIMIT 1");
            check.setString(1, username);
            if (check.executeQuery().next()) return RegisterResult.USERNAME_TAKEN;
            // Insert
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)");
            insert.setString(1, username);
            insert.setString(2, sha256(password));
            insert.executeUpdate();
            System.out.println("✔ Registered: " + username);
            return RegisterResult.SUCCESS;
        } catch (Exception e) {
            System.out.println("Register error: " + e.getMessage());
            return RegisterResult.DB_ERROR;
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────
    public boolean deleteUser(String username) {
        if (!dbController.isConnected()) return false;
        try {
            Connection conn = dbController.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM users WHERE username = ?");
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    // ── List usernames ─────────────────────────────────────────────────────
    public java.util.List<String> listUsernames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (!dbController.isConnected()) return names;
        try {
            ResultSet rs = dbController.getConnection().createStatement()
                .executeQuery("SELECT username FROM users ORDER BY created");
            while (rs.next()) names.add(rs.getString("username"));
        } catch (Exception e) {
            System.out.println("List users error: " + e.getMessage());
        }
        return names;
    }

    // ── SHA-256 ────────────────────────────────────────────────────────────
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
