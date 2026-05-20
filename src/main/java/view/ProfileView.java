package view;

import controller.DatabaseController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.sql.*;

public class ProfileView extends JPanel {

    private final String      username;
    private BufferedImage     profilePhoto;

    // ── Fields ─────────────────────────────────────────────────────────
    private JLabel            photoLabel;
    private JTextField        nameField;
    private JTextField        emailField;
    private JTextField        phoneField;
    private JTextArea         addressField;   // ← JTextArea not JTextField
    private JTextField        deptField;
    private JTextField        roleField;
    private JTextField        emergencyNameField;
    private JTextField        emergencyPhoneField; // ← fixed name (was getEmergencyNameField)

    public ProfileView(String username) {
        this.username = username != null ? username : "guest";
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadProfile();
    }

    // ══════════════════════════════════════════════════════════════════
    // BUILD UI
    // ══════════════════════════════════════════════════════════════════
    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Top header
        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(0, 0, 12, 0));
        content.add(title, BorderLayout.NORTH);

        // Main body: photo left, fields right
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(0, 0, 0, 24);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0; gbc.weighty = 1;
        body.add(buildPhotoPanel(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildFieldsPanel(), gbc);

        content.add(body, BorderLayout.CENTER);

        // Save button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);

        JButton saveBtn = new JButton("Save Profile");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(new Color(0x3B82F6));
        saveBtn.setOpaque(true);
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(160, 42));
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> saveProfile());
        bottom.add(saveBtn);
        content.add(bottom, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════
    // PHOTO PANEL
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildPhotoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(160, 240));

        photoLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int size = Math.min(w, h);
                int x = (w - size) / 2, y = (h - size) / 2;

                g2.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));

                if (profilePhoto != null) {
                    g2.drawImage(profilePhoto, x, y, size, size, null);
                } else {
                    g2.setColor(new Color(0xE2E8F0));
                    g2.fillOval(x, y, size, size);
                    g2.setClip(null);
                    g2.setColor(new Color(0x94A3B8));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    String txt = "No Photo";
                    g2.drawString(txt,
                            x + (size - fm.stringWidth(txt)) / 2,
                            y + size / 2 + fm.getAscent() / 2);
                }

                g2.setClip(null);
                g2.setColor(new Color(0x3B82F6));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                g2.dispose();
            }
        };
        photoLabel.setPreferredSize(new Dimension(140, 140));
        photoLabel.setMaximumSize(new Dimension(140, 140));
        photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(photoLabel);
        panel.add(Box.createVerticalStrut(12));

        JLabel userLbl = new JLabel("@" + username);
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLbl.setForeground(new Color(0x3B82F6));
        userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(userLbl);
        panel.add(Box.createVerticalStrut(12));

        JButton uploadBtn = new JButton("Upload Photo");
        uploadBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uploadBtn.setFocusPainted(false);
        uploadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        uploadBtn.addActionListener(e -> choosePhoto());
        panel.add(uploadBtn);

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════
    // FIELDS PANEL
    // ══════════════════════════════════════════════════════════════════
    private JPanel buildFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;

        // Initialize all fields
        nameField           = styledField();
        emailField          = styledField();
        phoneField          = styledField();
        deptField           = styledField();
        roleField           = styledField();
        emergencyNameField  = styledField();
        emergencyPhoneField = styledField();
        addressField        = new JTextArea(3, 20);
        addressField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addressField.setLineWrap(true);
        addressField.setWrapStyleWord(true);
        addressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                new EmptyBorder(6, 8, 6, 8)));

        // Row 0 — Full Name + Email
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(fieldLabel("Full Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.insets = new Insets(6, 16, 6, 12);
        panel.add(fieldLabel("Email"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);
        panel.add(emailField, gbc);

        // Row 1 — Phone + Department
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        panel.add(fieldLabel("Phone"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(phoneField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.insets = new Insets(6, 16, 6, 12);
        panel.add(fieldLabel("Department"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);
        panel.add(deptField, gbc);

        // Row 2 — Role + Emergency Contact Name
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        panel.add(fieldLabel("Role"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(roleField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.insets = new Insets(6, 16, 6, 12);
        panel.add(fieldLabel("Emergency Contact"), gbc);
        gbc.gridx = 3; gbc.weightx = 1;
        gbc.insets = new Insets(6, 0, 6, 0);
        panel.add(emergencyNameField, gbc);

        // Row 3 — Emergency Phone
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        panel.add(fieldLabel("Emergency Phone"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(emergencyPhoneField, gbc);

        // Row 4 — Address (full width)
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        panel.add(fieldLabel("Address"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 3;
        gbc.insets = new Insets(6, 0, 6, 0);
        JScrollPane addrScroll = new JScrollPane(addressField);
        addrScroll.setBorder(BorderFactory.createEmptyBorder());
        addrScroll.setPreferredSize(new Dimension(0, 70));
        panel.add(addrScroll, gbc);
        gbc.gridwidth = 1;

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════
    // CHOOSE PHOTO
    // ══════════════════════════════════════════════════════════════════
    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                profilePhoto = ImageIO.read(chooser.getSelectedFile());
                photoLabel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not load image.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LOAD PROFILE
    // ══════════════════════════════════════════════════════════════════
    private void loadProfile() {
        new Thread(() -> {
            try {
                Connection conn = DatabaseController.getSharedConnection();
                if (conn == null) return;

                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM user_profile WHERE username = ?");
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String name      = rs.getString("full_name");
                    String email     = rs.getString("email");
                    String phone     = rs.getString("phone");
                    String address   = rs.getString("address");
                    String dept      = rs.getString("department");
                    String role      = rs.getString("role");
                    String emName    = rs.getString("emergency_contact_name");
                    String emPhone   = rs.getString("emergency_contact_phone");
                    byte[] photoBytes = rs.getBytes("photo");

                    SwingUtilities.invokeLater(() -> {
                        if (name    != null) nameField.setText(name);
                        if (email   != null) emailField.setText(email);
                        if (phone   != null) phoneField.setText(phone);
                        if (address != null) addressField.setText(address);
                        if (dept    != null) deptField.setText(dept);
                        if (role    != null) roleField.setText(role);
                        if (emName  != null) emergencyNameField.setText(emName);
                        if (emPhone != null) emergencyPhoneField.setText(emPhone);

                        if (photoBytes != null) {
                            try {
                                profilePhoto = ImageIO.read(
                                        new ByteArrayInputStream(photoBytes));
                                photoLabel.repaint();
                            } catch (Exception ignored) {}
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println("Profile load error: " + e.getMessage());
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════
    // SAVE PROFILE
    // ══════════════════════════════════════════════════════════════════
    private void saveProfile() {
        new Thread(() -> {
            try {
                Connection conn = DatabaseController.getSharedConnection();
                if (conn == null) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this,
                                    "No database connection.", "Error",
                                    JOptionPane.ERROR_MESSAGE));
                    return;
                }

                conn.setAutoCommit(true);

                byte[] photoBytes = null;
                if (profilePhoto != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(profilePhoto, "jpg", baos);
                    photoBytes = baos.toByteArray();
                }

                String sql =
                        "INSERT INTO user_profile " +
                                "(username, full_name, email, phone, address, " +
                                "department, role, emergency_contact_name, " +
                                "emergency_contact_phone, photo) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "full_name=VALUES(full_name), " +
                                "email=VALUES(email), " +
                                "phone=VALUES(phone), " +
                                "address=VALUES(address), " +
                                "department=VALUES(department), " +
                                "role=VALUES(role), " +
                                "emergency_contact_name=VALUES(emergency_contact_name), " +
                                "emergency_contact_phone=VALUES(emergency_contact_phone), " +
                                "photo=VALUES(photo)";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, nameField.getText().trim());
                stmt.setString(3, emailField.getText().trim());
                stmt.setString(4, phoneField.getText().trim());
                stmt.setString(5, addressField.getText().trim());
                stmt.setString(6, deptField.getText().trim());
                stmt.setString(7, roleField.getText().trim());
                stmt.setString(8, emergencyNameField.getText().trim());
                stmt.setString(9, emergencyPhoneField.getText().trim());

                if (photoBytes != null) {
                    stmt.setBytes(10, photoBytes);
                } else {
                    stmt.setNull(10, java.sql.Types.BLOB);
                }

                stmt.executeUpdate();

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Profile saved successfully!",
                                "Saved", JOptionPane.INFORMATION_MESSAGE));

            } catch (Exception e) {
                System.out.println("Profile save error: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Failed to save: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════
    private JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(160, 34));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                new EmptyBorder(4, 8, 4, 8)));
        return f;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(0x64748B));
        return l;
    }
}