package view;

import model.AppConfig;
import model.DistanceModel;
import model.DistanceReading;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Dashboard.java.
 *
 * Changes from original:
 *  - No static fields — all instance variables.
 *  - Implements ReadingListener — updated by the model, not the controller.
 *  - Start/Stop buttons call model.setMonitoring() directly.
 *  - Table is refreshed from model history, not a direct DB query per update.
 *  - Bug fixed: chartPanel was added to layout twice — now only once via GraphView.
 */
public class MainView implements DistanceModel.ReadingListener {

    private final DistanceModel    model;
    private final JFrame           frame;
    private final JLabel           distanceLabel;
    private final JLabel           statusLabel;
    private final DefaultTableModel tableModel;
    private final RadarView        radarView;

    public MainView(DistanceModel model, GraphView graphView, RadarView radarView) {
        this.model    = model;
        this.radarView = radarView;

        frame = new JFrame("Distance Monitoring System");
        frame.setSize(900, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // ── Status panel ───────────────────────────────────────────────────
        distanceLabel = new JLabel("Distance: -- cm", SwingConstants.CENTER);
        distanceLabel.setFont(new Font("Arial", Font.BOLD, 28));

        statusLabel = new JLabel("Status: --", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 28));

        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.add(distanceLabel);
        statusPanel.add(statusLabel);

        // ── Button panel ───────────────────────────────────────────────────
        JButton startBtn = new JButton("Start Monitoring");
        JButton stopBtn  = new JButton("Stop Monitoring");
        startBtn.addActionListener(e -> model.setMonitoring(true));
        stopBtn .addActionListener(e -> model.setMonitoring(false));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);

        JButton settingsBtn = new JButton("Settings");
        settingsBtn.addActionListener(e ->
                new SettingsView(frame,model).setVisible(true));
        buttonPanel.add(settingsBtn);

        // ── Top container ──────────────────────────────────────────────────
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(statusPanel, BorderLayout.CENTER);
        topContainer.add(buttonPanel, BorderLayout.SOUTH);

        // ── Table ──────────────────────────────────────────────────────────
        String[] cols = {"Time", "Distance (cm)", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Object.class, new StatusCellRenderer());
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 200));

        // ── Center: chart + radar ──────────────────────────────────────────
        // Fix: chartPanel added only ONCE here (was added twice in original)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(graphView.getChartPanel());
        centerPanel.add(radarView);

        // ── Layout ─────────────────────────────────────────────────────────
        frame.add(topContainer, BorderLayout.NORTH);
        frame.add(centerPanel,  BorderLayout.CENTER);
        frame.add(scrollPane,   BorderLayout.SOUTH);
    }

    public void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── ReadingListener ────────────────────────────────────────────────────
    @Override
    public void onNewReading(DistanceReading reading) {
        SwingUtilities.invokeLater(() -> updateUI(reading));
    }

    private void updateUI(DistanceReading reading) {
        double distance = reading.getDistance();
        String status   = reading.getStatus();

        distanceLabel.setText(String.format("Distance: %.1f cm", distance));
        statusLabel.setText("Status: " + status);

        switch (status) {
            case "SAFE":
                statusLabel.setForeground(new Color(0, 160, 0));
                break;
            case "WARNING":
                statusLabel.setForeground(Color.ORANGE);
                break;
            case "CRITICAL":
                statusLabel.setForeground(Color.RED);
                showCriticalAlert();
                break;
        }

        // Insert newest row at top of table
        tableModel.insertRow(0, new Object[]{
            reading.getFormattedTimestamp(),
            distance,
            status
        });
        if (tableModel.getRowCount() > 50) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
    }

    private boolean alertShowing = false;

    private void showCriticalAlert() {
        if (alertShowing) return;
        alertShowing = true;
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(frame,
            "⚠ CRITICAL OBJECT DETECTED!",
            "ALERT",
            JOptionPane.WARNING_MESSAGE);
        alertShowing = false;
    }
}
