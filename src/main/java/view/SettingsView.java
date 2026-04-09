package view;

import controller.UserAuth;
import model.AppConfig;
import model.DistanceModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.UIManager;
import java.awt.*;
import java.util.List;

/**
 * SettingsView — tabbed dialog:
 *   Tab 1: Distance Thresholds + Simulation Speed slider
 *   Tab 2: Manage Users (add / delete accounts)
 */
public class SettingsView extends JDialog {

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color ACCENT        = new Color(0x3B82F6);
    private static final Color DANGER        = new Color(0xE11D48);


    private final DistanceModel model;
    private final UserAuth      userAuth;

    // ── Threshold widgets ──────────────────────────────────────────────────
    private JSpinner criticalSpinner;
    private JSpinner warningSpinner;

    // ── Constructors ───────────────────────────────────────────────────────
    public SettingsView(JFrame parent, DistanceModel model, UserAuth userAuth) {
        super(parent, "Settings", true);
        this.model    = model;
        this.userAuth = userAuth;
        init();
    }

    public SettingsView(JFrame parent, DistanceModel model) {
        this(parent, model, null);
    }

    private void init() {
        criticalSpinner = new JSpinner(new SpinnerNumberModel(
            AppConfig.CRITICAL_THRESHOLD, 1.0, 499.0, 1.0));
        warningSpinner = new JSpinner(new SpinnerNumberModel(
            AppConfig.WARNING_THRESHOLD, 2.0, 500.0, 1.0));

        setSize(460, 500);
        setResizable(false);
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(UIManager.getColor("Panel.background"));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.addTab("⚙  Thresholds", buildThresholdsTab());

        add(tabs, BorderLayout.CENTER);
    }

    // ----------------------------------------------------------------------
    // TAB 1 — Thresholds
    // ----------------------------------------------------------------------
    private JPanel buildThresholdsTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(24, 28, 16, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(10, 8, 10, 8);
        gbc.anchor  = GridBagConstraints.WEST;

        // ── Section label: Thresholds ──────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        JLabel threshTitle = new JLabel("Detection Thresholds");
        threshTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
//        threshTitle.setForeground(TEXT_MAIN);
        form.add(threshTitle, gbc);

        gbc.gridwidth = 1;

        // Critical row
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel critDot = new JLabel("●"); critDot.setForeground(DANGER);
        form.add(critDot, gbc);
        gbc.gridx = 1;
        JLabel critLabel = new JLabel("Critical below");
        critLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(critLabel, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(criticalSpinner, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("cm"), gbc);

        // Warning row
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel warnDot = new JLabel("●"); warnDot.setForeground(new Color(0xD59E0B));
        form.add(warnDot, gbc);
        gbc.gridx = 1;
        JLabel warnLabel = new JLabel("Warning below");
        warnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(warnLabel, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(warningSpinner, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("cm"), gbc);

        // ── Divider ────────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        gbc.insets = new Insets(14, 8, 14, 8);
        JSeparator sep = new JSeparator();
//        sep.setForeground(BORDER);
        form.add(sep, gbc);
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;

        outer.add(form,                    BorderLayout.CENTER);
        outer.add(buildThresholdButtons(), BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildThresholdButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Separator.foreground") != null
                ? UIManager.getColor("Separator.foreground")
                : new Color(0xE2E8F0)));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        saveBtn.setBackground(ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveThresholds());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void saveThresholds() {
        double critical = (Double) criticalSpinner.getValue();
        double warning  = (Double) warningSpinner.getValue();
        if (critical >= warning) {
            JOptionPane.showMessageDialog(this,
                "Critical must be less than Warning.\nExample: Critical = 10 cm, Warning = 30 cm",
                "Invalid Thresholds", JOptionPane.WARNING_MESSAGE);
            return;
        }
        model.setThresholds(critical, warning);
        JOptionPane.showMessageDialog(this,
            String.format("Thresholds updated!\nCritical: below %.0f cm\nWarning:  below %.0f cm",
                critical, warning),
            "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

}
