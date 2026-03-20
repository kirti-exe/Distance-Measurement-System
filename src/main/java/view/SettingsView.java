package view;

import model.AppConfig;
import model.DistanceModel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class SettingsView extends JDialog {

    private final DistanceModel model;

    private final JSpinner criticalSpinner;
    private final JSpinner warningSpinner;

    public SettingsView(JFrame parent, DistanceModel model){
        super(parent, "Settings - Distance Thresholds", true);
        this.model = model;

        setSize(380, 420);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Color.WHITE);

        criticalSpinner = new JSpinner(new SpinnerNumberModel(
//                AppConfig.CRITICAL_THRESHOLD,
                10.0,
                1.0,
                499.0,
                1.0
        ));

        warningSpinner = new JSpinner(new SpinnerNumberModel(
                AppConfig.WARNING_THRESHOLD,
                2.0,
                500.0,
                1.0
        ));

        setLayout(new BorderLayout());
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,8,10,8);
        gbc.anchor = GridBagConstraints.WEST;

        //----Critical row------------------------------------------------------
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel critDot = new JLabel("●");
        critDot.setForeground(new Color(0xE11D48));
        form.add(critDot, gbc);

        gbc.gridx = 1;
        JLabel critLabel = new JLabel("Critical below");
        critLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(critLabel, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(criticalSpinner, gbc);

        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("cm"), gbc);

        //----warning row------------------------------------------------
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel warnDot = new JLabel("●");
        warnDot.setForeground(new Color(0xD59E0b));
        form.add(warnDot, gbc);

        gbc.gridx = 1;
        JLabel warnLabel = new JLabel("Warning below");
        warnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(warnLabel, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        form.add(warningSpinner, gbc);

        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("cm"), gbc);

        return form;
    }

    private JPanel buildButtonPanel(){
        JPanel panel = new JPanel((new FlowLayout(FlowLayout.RIGHT, 12, 12)));
        panel.setBackground(new Color(0xF8FAFC));
        panel.setBorder(BorderFactory.createMatteBorder(1,0,0,0, new Color(0xE2E8F0)));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        saveBtn.setBackground(new Color(0x3B82F6));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveThresholds());

        panel.add(cancelBtn);
        panel.add(saveBtn);
        return panel;
    }

    private void saveThresholds(){
        double critical = (Double) criticalSpinner.getValue();
        double warning = (Double) warningSpinner.getValue();

        if(critical >= warning){
            JOptionPane.showMessageDialog(this,
                    "Critical must be less than warning.\n" +
                    "Example: Critical = 10 cm, Warning = 30 cm",
                    "Invalid Thresholds",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        model.setThresholds(critical, warning);

        JOptionPane.showMessageDialog(this,
                String.format("Thresholds updated!\nCritical: below %.0f cm\nWarning:  below %.0f cm",
                        critical, warning),
                "Saved",
                JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }




}
