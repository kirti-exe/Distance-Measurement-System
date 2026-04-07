package view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import controller.SosController;
import controller.UserAuth;
import model.DistanceModel;
import model.DistanceReading;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/**
 * CleanView — FlatLaf handles ALL background/text/border colours.
 * No manual palette. toggleDarkMode() swaps the LaF and calls
 * SwingUtilities.updateComponentTreeUI — that's it.
 *
 * Status colours (green/amber/red) are kept because they are semantic
 * and must stay the same in both modes.
 */
public class CleanView implements DistanceModel.ReadingListener {

    // ── Status colours only — same in both modes ───────────────────────────
    private static final Color SAFE_TEXT = new Color(0x16A34A);
    private static final Color SAFE_BG   = new Color(0xDCFCE7);
    private static final Color SAFE_DOT  = new Color(0x22C55E);
    private static final Color WARN_TEXT = new Color(0xD97706);
    private static final Color WARN_BG   = new Color(0xFEF3C7);
    private static final Color WARN_DOT  = new Color(0xF59E0B);
    private static final Color CRIT_TEXT = new Color(0xE11D48);
    private static final Color CRIT_BG   = new Color(0xFFE4E6);
    private static final Color CRIT_DOT  = new Color(0xF43F5E);

    // ── Unit ───────────────────────────────────────────────────────────────
    private enum Unit { CM, MM, M, FT }
    private Unit currentUnit = Unit.CM;
    private static final String[] UNIT_LABELS = {"cm", "mm", "m", "ft"};

    private double convert(double cm) {
        switch (currentUnit) {
            case MM: return cm * 10.0;
            case M:  return cm / 100.0;
            case FT: return cm / 30.48;
            default: return cm;
        }
    }
    private String unitLabel() { return UNIT_LABELS[currentUnit.ordinal()]; }
    private String fmt(double cm) {
        double v = convert(cm);
        if (currentUnit == Unit.M || currentUnit == Unit.FT)
            return String.format("%.2f %s", v, unitLabel());
        return String.format("%.0f %s", v, unitLabel());
    }

    // ── State ──────────────────────────────────────────────────────────────
    private final DistanceModel model;
    private final UserAuth      userAuth;
    private final SosController sosController;
    private boolean darkMode    = false;
    private int     tick        = 0;
    private long    lastReadingMs = 0;

    // ── Components ─────────────────────────────────────────────────────────
    private final JFrame           frame;
    private final RadialGaugePanel gaugePanel;
    private final JLabel           statusBadge;
    private final JPanel           alertBanner;
    private final JLabel           alertLabel;
    private final JLabel           avgLabel;
    private final JLabel           minLabel;
    private final JLabel           maxLabel;
    private final JLabel           samplesLabel;
    private final JLabel           stdDevLabel, uptimeLabel, criticalCountLabel;
    private final JLabel           updatedLabel;
    private final DefaultTableModel tableModel;
    private final DefaultTableModel alertTableModel;
    private final XYSeries         series = new XYSeries("Distance");
    private final ChartPanel       chartPanel;
    private final JButton          toggleBtn;
    private final JButton          settingsBtn;
    private final JButton          darkModeBtn;
    private final JButton          sosBtn;
    private final JButton          fullscreenBtn;
    private boolean                isFullscreen = false;
    private final JButton[]        unitBtns;
    private JPanel                 tableCard;
    private JButton                collapseBtn;
    private boolean                tableVisible = true;

    private long sessionStartMs = System.currentTimeMillis();
    private int  totalReadings  = 0;

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════
    public CleanView(DistanceModel model, UserAuth userAuth, SosController sosController) {
        this.model    = model;
        this.userAuth = userAuth;
        this.sosController = sosController;

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 16));

        gaugePanel      = new RadialGaugePanel();
        statusBadge     = new JLabel("● SAFE");
        alertBanner     = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        alertLabel      = new JLabel("");
        avgLabel        = monoLabel("--");
        minLabel        = monoLabel("--");
        maxLabel        = monoLabel("--");
        samplesLabel    = monoLabel("0");
        stdDevLabel     = monoLabel("--");
        uptimeLabel     = monoLabel("--");
        criticalCountLabel = monoLabel("0");
        updatedLabel    = new JLabel("No data yet");
        tableModel      = new DefaultTableModel(
                new String[]{"Timestamp", "Distance", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        alertTableModel = new DefaultTableModel(
                new String[]{"Timestamp", "Distance", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        chartPanel  = buildChart();
        toggleBtn   = buildToggleBtn();
        settingsBtn = buildSettingsBtn();
        darkModeBtn = buildDarkModeBtn();
        sosBtn      = buildSosBtn();
        unitBtns    = buildUnitBtns();
        fullscreenBtn = buildFullscreenBtn();

        frame = new JFrame("Distance Monitor");
        frame.setSize(1400, 900);
        frame.setMinimumSize(new Dimension(1200, 750));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));
        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildBody(),   BorderLayout.CENTER);
        frame.add(buildStatusBar(), BorderLayout.SOUTH);
        frame.setLocation(120, 80);

        new Timer(1000, e -> refreshUpdatedLabel()).start();

        // F11 fullscreen toggle
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F11"), "fullscreen");
        frame.getRootPane().getActionMap().put("fullscreen", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e){
                toggleFullscreen();
            }
        });
    }

    public CleanView(DistanceModel model) { this(model, null, null); }

    public void show() { frame.setVisible(true); }

    // ══════════════════════════════════════════════════════════════════════
    // READING LISTENER
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void onNewReading(DistanceReading reading) {
        SwingUtilities.invokeLater(() -> updateUI(reading));
    }

    private void updateUI(DistanceReading reading) {
        lastReadingMs = System.currentTimeMillis();
        tick++;

        series.add(tick, reading.getDistance());
        if (series.getItemCount() > 40) series.remove(0);

        gaugePanel.setValue(reading.getDistance(), unitLabel(), convert(reading.getDistance()));
        applyStatusStyle(statusBadge, reading.getStatus());
        updateAlertBanner(reading.getStatus(), reading.getDistance());
        updateChartColor(reading.getStatus());

        String distStr = fmt(reading.getDistance());
        avgLabel.setText(fmt(model.getAverage()));
        minLabel.setText(fmt(model.getMin()));
        maxLabel.setText(fmt(model.getMax()));
        samplesLabel.setText(String.valueOf(model.getSampleCount()));

        // Std deviation
        stdDevLabel.setText(String.format("%.1f cm", model.getStdDeviation()));

        // Uptime
        long uptimeSecs = (System.currentTimeMillis() - sessionStartMs) / 1000;
        long mins = uptimeSecs / 60;
        long secs = uptimeSecs % 60;
        uptimeLabel.setText(String.format("%dm %ds", mins, secs));

        // Critical count
        criticalCountLabel.setText(String.valueOf(model.getCCriticalCount()));

        tableModel.insertRow(0, new Object[]{
                reading.getFormattedTimestamp(), distStr, reading.getStatus()});
        if (tableModel.getRowCount() > 100) tableModel.removeRow(tableModel.getRowCount() - 1);

        if ("CRITICAL".equals(reading.getStatus()) || "WARNING".equals(reading.getStatus())) {
            alertTableModel.insertRow(0, new Object[]{
                    reading.getFormattedTimestamp(), distStr, reading.getStatus()});
            if (alertTableModel.getRowCount() > 100)
                alertTableModel.removeRow(alertTableModel.getRowCount() - 1);
        }
    }

    private void refreshUpdatedLabel() {
        if (lastReadingMs == 0) { updatedLabel.setText("No data yet"); return; }
        long secs = (System.currentTimeMillis() - lastReadingMs) / 1000;
        if (secs < 5)       updatedLabel.setText("Updated just now");
        else if (secs < 60) updatedLabel.setText("Updated " + secs + "s ago");
        else                updatedLabel.setText("Updated " + (secs / 60) + "m ago");
    }

    // ══════════════════════════════════════════════════════════════════════
    // DARK MODE — FlatLaf does all the work
    // ══════════════════════════════════════════════════════════════════════
    private void toggleDarkMode() {
        darkMode = !darkMode;
        try {
            if (darkMode) FlatDarkLaf.setup();
            else          FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ignored) {}
        darkModeBtn.setText(darkMode ? "☀" : "🌙");
        darkModeBtn.setToolTipText(darkMode ? "Switch to light mode" : "Switch to dark mode");
        updateChartTheme();
        frame.repaint();
    }

    // Chart needs manual repainting since JFreeChart is not a Swing component
    private void updateChartTheme() {
        Color bg  = UIManager.getColor("Panel.background");
        Color grid = UIManager.getColor("Separator.foreground");
        Color tick = UIManager.getColor("Label.disabledForeground");
        if (bg   == null) bg   = Color.WHITE;
        if (grid == null) grid = new Color(0xE2E8F0);
        if (tick == null) tick = new Color(0x94A3B8);

        XYPlot plot = chartPanel.getChart().getXYPlot();
        chartPanel.getChart().setBackgroundPaint(bg);
        plot.setBackgroundPaint(bg);
        plot.setRangeGridlinePaint(grid);
        ((NumberAxis) plot.getRangeAxis()).setTickLabelPaint(tick);
        chartPanel.setBackground(bg);
    }

    // ══════════════════════════════════════════════════════════════════════
    // HEADER
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        UIManager.getColor("Separator.foreground") != null
                                ? UIManager.getColor("Separator.foreground")
                                : new Color(0xE2E8F0)),
                new EmptyBorder(0, 24, 0, 24)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        left.setOpaque(false);
        left.add(new RadarIconWidget());
        JLabel title = new JLabel("Distance Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);
        applyStatusStyle(statusBadge, "SAFE");
        right.add(statusBadge);
        right.add(buildUnitGroup());
        right.add(fullscreenBtn);
        right.add(sosBtn);
        right.add(darkModeBtn);
        right.add(settingsBtn);
        right.add(toggleBtn);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ALERT BANNER
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildAlertBannerPanel() {
        alertBanner.setBackground(CRIT_BG);
        alertBanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CRIT_DOT));
        alertBanner.setVisible(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(CRIT_DOT);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        alertBanner.add(dot);
        alertLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        alertLabel.setForeground(CRIT_TEXT);
        alertBanner.add(alertLabel);
        return alertBanner;
    }

    private void updateAlertBanner(String status, double distanceCm) {
        switch (status) {
            case "CRITICAL":
                alertLabel.setText("Critical object detected — " + fmt(distanceCm));
                alertBanner.setBackground(CRIT_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0,0,1,0,CRIT_DOT));
                alertLabel.setForeground(CRIT_TEXT);
                alertBanner.setVisible(true);
                showToast(frame, "⚠ Critical object detected!", CRIT_BG, CRIT_TEXT);
                break;
            case "WARNING":
                alertLabel.setText("Object in warning zone — " + fmt(distanceCm));
                alertBanner.setBackground(WARN_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0,0,1,0,WARN_DOT));
                alertLabel.setForeground(WARN_TEXT);
                alertBanner.setVisible(true);
                break;
            default:
                alertBanner.setVisible(false);
        }
    }

    // BODY
    private JPanel buildBody() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(buildAlertBannerPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0; gbc.weightx = 1.3; gbc.ipadx = 260;
        gbc.insets = new Insets(0, 0, 0, 14);
        body.add(buildLeftColumn(), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7; gbc.ipadx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildRightColumn(), gbc);

        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,16,6));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,
                UIManager.getColor("Separator.foreground") != null
                            ? UIManager.getColor("Separator.foreground")
                            : new Color(0xE2E8F0)));

        // MySQL status
        JLabel mysqlDot = new JLabel("●");
        mysqlDot.setFont(new Font("Segoe UI", Font.PLAIN,11));
        JLabel mysqlLabel = new JLabel("MySQL");
        mysqlLabel.setFont(new Font("Segoe UI", Font.PLAIN,11));

        // Arduino / Sensor status
        JLabel arduinoDot = new JLabel("●");
        arduinoDot.setFont(new Font("Segoe UI", Font.PLAIN,11));
        JLabel arduinoLabel = new JLabel("Sensor");
        arduinoLabel.setFont(new Font("Segoe UI", Font.PLAIN,11));

        // Version label
        JLabel version = new JLabel("v1.0.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN,10));
        version.setForeground(new Color(0x94A3B8));

        bar.add(mysqlDot);
        bar.add(mysqlLabel);
        bar.add(new JSeparator(JSeparator.VERTICAL) {{
            setPreferredSize(new Dimension(1,14));
        }});
        bar.add(arduinoDot);
        bar.add(arduinoLabel);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(version);

        // Live status checker - runs every 2 seconds
        new Timer(2000, e-> {
            // MySQL check
            java.sql.Connection conn =
                    controller.DatabaseController.getSharedConnection();
            boolean mysqlOk = conn != null;
            try {
                if (conn != null) mysqlOk = !conn.isClosed();
            }catch (Exception ex) {
                mysqlOk = false;
            }
            mysqlDot.setForeground(mysqlOk ? SAFE_DOT : CRIT_DOT);
            mysqlLabel.setText("MySQL: " + (mysqlOk ? "Connected" : "Disconnected"));

            // Sensor check - if model has reading in last 5 seconds
            boolean sensorOk = (System.currentTimeMillis() - lastReadingMs) < 5000
                    && lastReadingMs > 0;
            arduinoDot.setForeground(sensorOk ? SAFE_DOT :
                    model.isMonitoring() ? WARN_DOT : new Color(0x94A3B8));
            arduinoLabel.setText("Sensor: " +
                    (sensorOk ? "Active" : model.isMonitoring() ? "Waiting..." : "Stopped"));
        }).start();

        return bar;
    }

    // ── Left column ────────────────────────────────────────────────────────
    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JPanel gaugeCard = card();
        gaugeCard.setLayout(new BoxLayout(gaugeCard, BoxLayout.Y_AXIS));
        gaugeCard.setBorder(new EmptyBorder(28, 28, 28, 28));
        JLabel gt = new JLabel("CURRENT DISTANCE");
        gt.setFont(new Font("Segoe UI", Font.BOLD, 10));
        gt.setAlignmentX(Component.CENTER_ALIGNMENT);
        gt.setBorder(new EmptyBorder(0, 0, 10, 0));
        gaugeCard.add(gt);
        gaugePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gaugeCard.add(gaugePanel);
        col.add(gaugeCard);
        col.add(Box.createVerticalStrut(12));

        JPanel statsCard = card();
        statsCard.setLayout(new GridLayout(7, 1, 0, 0));
        addStatRow(statsCard, "Average", avgLabel,    true);
        addStatRow(statsCard, "Minimum", minLabel,    true);
        addStatRow(statsCard, "Maximum", maxLabel,    true);
        addStatRow(statsCard, "Std Dev", stdDevLabel,  true);
        addStatRow(statsCard, "Uptime", uptimeLabel,  true);
        addStatRow(statsCard, "Critical", criticalCountLabel,  true);
        addStatRow(statsCard, "Samples", samplesLabel, false);

        col.add(statsCard);
        return col;
    }

    private void addStatRow(JPanel card, String name, JLabel valueLabel, boolean divider) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        valueLabel.setFont(new Font("Consolas", Font.BOLD, 20));

        row.add(lbl,        BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);

        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(row, BorderLayout.CENTER);
        if (divider) w.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground")
                        : new Color(0xF1F5F9)));
        card.add(w);
    }

    // ── Right column ───────────────────────────────────────────────────────
    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JPanel chartCard = card();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel ct = new JLabel("Distance Timeline");
        ct.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel livePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        livePanel.setOpaque(false);
        JLabel live = new JLabel("● Live");
        live.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        live.setForeground(SAFE_DOT);
        updatedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        livePanel.add(updatedLabel);
        livePanel.add(live);

        chartHeader.add(ct,        BorderLayout.WEST);
        chartHeader.add(livePanel, BorderLayout.EAST);
        chartCard.add(chartHeader, BorderLayout.NORTH);
        chartCard.add(chartPanel,  BorderLayout.CENTER);
        col.add(chartCard);
        col.add(Box.createVerticalStrut(12));

        tableCard = card();
        tableCard.setLayout(new BorderLayout());

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        tableHeader.setBorder(new EmptyBorder(8, 14, 8, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("All Readings",  buildReadingsTable(tableModel));
        tabs.addTab("Alert History", buildReadingsTable(alertTableModel));
        tabs.addTab("Incident Log", buildIncidentLogPanel());
        collapseBtn = new JButton("▾  History");
        collapseBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        collapseBtn.setContentAreaFilled(false);
        collapseBtn.setBorderPainted(false);
        collapseBtn.setFocusPainted(false);
        collapseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapseBtn.addActionListener(e -> {
            tableVisible = !tableVisible;
            tabs.setVisible(tableVisible);
            collapseBtn.setText(tableVisible ? "▾  History" : "▸  History");
            tableCard.revalidate();
        });

        tableHeader.add(collapseBtn, BorderLayout.WEST);
        tableCard.add(tableHeader,   BorderLayout.NORTH);
        tableCard.add(tabs,          BorderLayout.CENTER);
        col.add(tableCard);
        return col;
    }

    private JScrollPane buildReadingsTable(DefaultTableModel mdl) {
        JTable table = new JTable(mdl);
        table.setFont(new Font("Consolas", Font.PLAIN, 15));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setRowHeight(40);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Incident Log Tab
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildIncidentLogPanel(){
        JPanel panel = new JPanel(new BorderLayout(0,8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12,16,12,16));

        // Input area
        JPanel inputRow = new JPanel(new BorderLayout(8,0));
        inputRow.setOpaque(false);

        JTextField noteField = new JTextField();
        noteField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        noteField.setPreferredSize(new Dimension(0,40));

        JButton logBtn = new JButton("Log Incident");
        logBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logBtn.setForeground(Color.WHITE);
        logBtn.setBackground(new Color(0x3B82F6));
        logBtn.setOpaque(true);
        logBtn.setFocusPainted(false);
        logBtn.setPreferredSize(new Dimension(140,40));

        inputRow.add(noteField, BorderLayout.CENTER);
        inputRow.add(logBtn, BorderLayout.EAST);

        // Log table
        DefaultTableModel logTableModel = new DefaultTableModel(
                new String[]{"Timestamp", "Distance", "Status", "Note"}, 0) {
            public boolean isCellEditable(int r, int c) {return false;}
        };

        JTable logTable = new JTable(logTableModel);
        logTable.setFont(new Font("Consolas", Font.PLAIN, 13));
        logTable.setRowHeight(36);
        logTable.setShowVerticalLines(false);
        logTable.setShowHorizontalLines(true);
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        logTable.getTableHeader().setReorderingAllowed(false);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(300);

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Log button action
        logBtn.addActionListener(e -> {
            String note = noteField.getText().trim();
            if(note.isEmpty()){
                showToast(frame, "Please write a note before logging.", WARN_BG, WARN_TEXT);
                return;
            }

            DistanceReading latest = model.getLatestReading();
            String distance = latest != null ? fmt(latest.getDistance()) : "--";
            String status = latest != null ? latest.getStatus() : "--";
            String time = latest != null
                    ? latest.getFormattedTimestamp()
                    : new java.util.Date().toString();

            // Add to table
            logTableModel.insertRow(0, new Object[]{time, distance, status, note});

            // save to mySql
            saveIncidentLog(latest, note);

            // clear field
            noteField.setText("");
            showToast(frame, "Incident logged successfully.", SAFE_BG, SAFE_TEXT);
        });

        panel.add(inputRow, BorderLayout.NORTH);
        loadIncidentLogs(logTableModel);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void saveIncidentLog(DistanceReading reading, String note){
        new Thread(() -> {
            try {
                java.sql.Connection conn = controller.DatabaseController.getSharedConnection();
//                System.out.println("Incident log connection: " + conn);
                if(conn == null){
                    System.out.println("Connection is null - cannot save incident");
                    return;
                }

                // force auto commit on
                conn.setAutoCommit(true);

                String sql = "INSERT INTO incident_log(distance, status, note) VALUES (?, ?, ?)";
                java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setDouble(1, reading != null ? reading.getDistance() : 0);
                stmt.setString(2, reading != null ? reading.getStatus() : "--");
                stmt.setString(3, note);

//                System.out.println("Incident saved to DB");
                int rows = stmt.executeUpdate();
                System.out.println("Incident rows inserted: " + rows);

            } catch(Exception e){
                System.out.println("Incident log DB error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadIncidentLogs(javax.swing.table.DefaultTableModel logTableModel) {
        new Thread(() -> {
            try{
                java.sql.Connection conn = controller.DatabaseController.getSharedConnection();
                if(conn == null) return;

                String sql = "SELECT timestamp, distance, status, note " +
                        "FROM incident_log ORDER BY timestamp DESC LIMIT 100";
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(sql);

                while(rs.next()){
                    String time = rs.getString("timestamp");
                    double distance = rs.getDouble("distance");
                    String status = rs.getString("status");
                    String note = rs.getString("note");

                    SwingUtilities.invokeLater(() ->
                            logTableModel.addRow(new Object[] {
                                    time,
                                    String.format("%.1f cm", distance),
                                    status,
                                    note
                            })
                    );
                }
                System.out.println("Incident logs loaded from DB");
            } catch (Exception e) {
                System.out.println("Failed to load incident log." + e.getMessage());
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHART
    // ══════════════════════════════════════════════════════════════════════
    private ChartPanel buildChart() {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));
        chart.removeLegend();
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(4, 0, 4, 0));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(new Color(0xE2E8F0));
        plot.setRangeGridlineStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{4, 4}, 0));
        plot.setRangeGridlinesVisible(true);

        ((NumberAxis) plot.getDomainAxis()).setVisible(false);
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0, 110);
        range.setTickLabelFont(new Font("Consolas", Font.PLAIN, 10));
        range.setAxisLineVisible(false);
        range.setTickMarksVisible(false);

        XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true, false);
        line.setSeriesPaint(0, SAFE_DOT);
        line.setSeriesStroke(0, new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        plot.setRenderer(0, line);

        XYAreaRenderer area = new XYAreaRenderer();
        area.setSeriesPaint(0, new Color(34, 197, 94, 35));
        plot.setDataset(1, dataset);
        plot.setRenderer(1, area);
        plot.mapDatasetToRangeAxis(1, 0);

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(0, 100));
        cp.setMinimumDrawWidth(0);
        cp.setMinimumDrawHeight(0);
        cp.setMaximumDrawWidth(4000);
        cp.setMaximumDrawHeight(4000);
        cp.setPopupMenu(null);
        return cp;
    }

    private void updateChartColor(String status) {
        Color c = "CRITICAL".equals(status) ? CRIT_DOT :
                "WARNING".equals(status)  ? WARN_DOT : SAFE_DOT;
        XYPlot plot = chartPanel.getChart().getXYPlot();
        ((XYLineAndShapeRenderer) plot.getRenderer(0)).setSeriesPaint(0, c);
        plot.getRenderer(1).setSeriesPaint(0, new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUTTONS
    // ══════════════════════════════════════════════════════════════════════
    private JButton buildToggleBtn() {
        JButton btn = new JButton("Stop");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setPreferredSize(new Dimension(120, 45));
        btn.setForeground(CRIT_TEXT);
        btn.setBackground(CRIT_BG);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            boolean nowOn = !model.isMonitoring();
            model.setMonitoring(nowOn);
            if (nowOn) {
                btn.setText("Stop");
                btn.setForeground(CRIT_TEXT);
                btn.setBackground(CRIT_BG);
            } else {
                btn.setText("Start");
                btn.setForeground(SAFE_TEXT);
                btn.setBackground(SAFE_BG);
            }
        });
        return btn;
    }

    private JButton buildSettingsBtn() {
        JButton btn = new JButton("Settings");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(120, 45));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e ->
                new SettingsView(frame, model, userAuth).setVisible(true));
        return btn;
    }

    private JButton buildDarkModeBtn() {
        JButton btn = new JButton("Dark");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(80, 45));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Toggle dark mode");
        btn.addActionListener(e -> {
            toggleDarkMode();
            btn.setText(darkMode ? "Light" : "Dark");
        });
        return btn;
    }

    private JButton buildSosBtn(){
        JButton btn = new JButton("SOS");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setPreferredSize(new Dimension(80,45));
        btn.setForeground(Color.WHITE);
        btn.setBackground(CRIT_DOT);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if(sosController != null){
                sosController.sendSOS();
                showToast(frame, "SOS sent to your phone!", CRIT_BG, CRIT_TEXT);
            }
        });
        return btn;
    }

    private JButton buildFullscreenBtn() {
        JButton btn = new JButton("⛶");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(50,45));
        btn.setFocusPainted(false);
        btn.setToolTipText("Fullscreen (F11)");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> toggleFullscreen());
        return btn;
    }

    private void toggleFullscreen(){
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (!isFullscreen) {
            device.setFullScreenWindow(frame);
            isFullscreen = true;
            fullscreenBtn.setText("✕");
            fullscreenBtn.setToolTipText("Exit fullscreen (F11");
        } else {
            device.setFullScreenWindow(null);
            isFullscreen = false;
            fullscreenBtn.setText("⛶");
            fullscreenBtn.setToolTipText("Fullscreen (F11)");
        }
    }

    private JButton[] buildUnitBtns() {
        JButton[] btns = new JButton[4];
        Unit[] units = Unit.values();
        for (int i = 0; i < 4; i++) {
            final Unit u = units[i];
            btns[i] = new JButton(UNIT_LABELS[i]);
            btns[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btns[i].setFocusPainted(false);
            btns[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btns[i].addActionListener(e -> {
                currentUnit = u;
                refreshUnitBtnStyles();
                refreshAllDisplayedValues();
            });
        }
        return btns;
    }

    private JPanel buildUnitGroup() {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        group.setOpaque(false);
        for (int i = 0; i < unitBtns.length; i++) {
            styleUnitBtn(unitBtns[i], i == 0);
            group.add(unitBtns[i]);
            if (i < unitBtns.length - 1) {
                JSeparator sep = new JSeparator(JSeparator.VERTICAL);
                sep.setPreferredSize(new Dimension(1, 22));
                group.add(sep);
            }
        }
        return group;
    }

    private void styleUnitBtn(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(0x3B82F6));
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
        } else {
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setForeground(UIManager.getColor("Label.foreground"));
        }
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
    }

    private void refreshUnitBtnStyles() {
        Unit[] units = Unit.values();
        for (int i = 0; i < unitBtns.length; i++)
            styleUnitBtn(unitBtns[i], units[i] == currentUnit);
    }

    private void refreshAllDisplayedValues() {
        if (model.getSampleCount() == 0) return;
        double latest = model.getHistory().get(model.getHistory().size() - 1).getDistance();
        gaugePanel.setValue(latest, unitLabel(), convert(latest));
        avgLabel.setText(fmt(model.getAverage()));
        minLabel.setText(fmt(model.getMin()));
        maxLabel.setText(fmt(model.getMax()));
        tableModel.setRowCount(0);
        List<DistanceReading> history = model.getHistory();
        int start = Math.max(0, history.size() - 100);
        for (int i = history.size() - 1; i >= start; i--) {
            DistanceReading r = history.get(i);
            tableModel.addRow(new Object[]{
                    r.getFormattedTimestamp(), fmt(r.getDistance()), r.getStatus()});
        }
        alertTableModel.setRowCount(0);
        for (int i = history.size() - 1; i >= 0; i--) {
            DistanceReading r = history.get(i);
            if ("CRITICAL".equals(r.getStatus()) || "WARNING".equals(r.getStatus())) {
                alertTableModel.addRow(new Object[]{
                        r.getFormattedTimestamp(), fmt(r.getDistance()), r.getStatus()});
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private JPanel card() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createLineBorder(
                UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground")
                        : new Color(0xE2E8F0), 1));
        return p;
    }

    private JLabel monoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 16));
        return l;
    }

    void applyStatusStyle(JLabel lbl, String status) {
        switch (status) {
            case "CRITICAL": lbl.setText("● CRITICAL"); lbl.setForeground(CRIT_TEXT); lbl.setBackground(CRIT_BG); break;
            case "WARNING":  lbl.setText("● WARNING");  lbl.setForeground(WARN_TEXT); lbl.setBackground(WARN_BG); break;
            default:         lbl.setText("● SAFE");     lbl.setForeground(SAFE_TEXT); lbl.setBackground(SAFE_BG);
        }
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(5, 12, 5, 12));
    }

    // ══════════════════════════════════════════════════════════════════════
    // RADIAL GAUGE
    // ══════════════════════════════════════════════════════════════════════
    static class RadialGaugePanel extends JPanel {
        private double target = 50, animated = 50;
        private double displayValue = 50;
        private String unitLbl = "cm";
        private final Timer anim;

        RadialGaugePanel() {
            setPreferredSize(new Dimension(320, 320));
            setOpaque(false);
            anim = new Timer(16, e -> {
                animated += (target - animated) * 0.10;
                repaint();
            });
            anim.start();
        }

        void setValue(double rawCm, String unit, double converted) {
            target       = rawCm;
            unitLbl      = unit;
            displayValue = converted;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2;
            int r = Math.min(w, h) / 2 - 14;
            double pct = Math.min(animated / 100.0, 1.0);
            String s = animated <= 15 ? "CRITICAL" : animated <= 35 ? "WARNING" : "SAFE";
            Color arc = "CRITICAL".equals(s) ? CRIT_DOT : "WARNING".equals(s) ? WARN_DOT : SAFE_DOT;

            // Track ring — use FlatLaf separator color
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Color trackColor = UIManager.getColor("Separator.foreground");
            g2.setColor(trackColor != null ? trackColor : new Color(0xE2E8F0));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);

            // Progress arc
            g2.setColor(arc);
            g2.drawArc(cx - r, cy - r, r * 2, r * 2, 90, -(int)(pct * 360));

            // Value text — use FlatLaf foreground
            Color fg = UIManager.getColor("Label.foreground");
            g2.setColor(fg != null ? fg : Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
            FontMetrics fm = g2.getFontMetrics();
            String val = ("m".equals(unitLbl) || "ft".equals(unitLbl))
                    ? String.format("%.2f", displayValue)
                    : String.valueOf((int) Math.round(displayValue));
            g2.drawString(val, cx - fm.stringWidth(val) / 2, cy + 9);

            // Unit label
            Color sub = UIManager.getColor("Label.disabledForeground");
            g2.setColor(sub != null ? sub : new Color(0x94A3B8));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            fm = g2.getFontMetrics();
            g2.drawString(unitLbl, cx - fm.stringWidth(unitLbl) / 2, cy + 28);
            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // RADAR ICON
    // ══════════════════════════════════════════════════════════════════════
    static class RadarIconWidget extends JComponent {
        RadarIconWidget() { setPreferredSize(new Dimension(22, 22)); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x3B82F6));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(1, 1, 19, 19);
            g2.drawOval(5, 5, 11, 11);
            g2.fillOval(9, 9, 4, 4);
            g2.dispose();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOAST NOTIFICATION
    // ══════════════════════════════════════════════════════════════════════
    public static void showToast(JFrame parent, String message, Color bg, Color fg){
        JWindow toast = new JWindow(parent);
        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(fg);
        label.setBackground(bg);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(12,20,12,20));
        toast.add(label);
        toast.pack();

        // Position bottom-right of parent
        int x = parent.getX() + parent.getWidth() - toast.getWidth() - 24;
        int y = parent.getY() + parent.getHeight() - toast.getHeight() - 48;
        toast.setLocation(x,y);
        toast.setVisible(true);

        // Auto dismiss after 3 seconds
        new Timer(3000, e -> {
            toast.setVisible(false);
            toast.dispose();
        }) {{ setRepeats(false); }}.start();
    }

}