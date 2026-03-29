package view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import controller.UserAuth;
import model.DistanceModel;
import model.DistanceReading;
import org.jfree.chart.*;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CleanView — modernised dashboard.
 *
 * New features vs original:
 *  - Dark / Light mode toggle (🌙 / ☀)
 *  - Unit toggle in header: cm | mm | m | ft
 *  - Bigger fonts on status badge, distance value, start/stop button
 *  - Smaller graph (max height 140 → 110)
 *  - Min / Max / Avg stat cards enlarged
 *  - Collapsible history table (▾ / ▸ toggle)
 *  - Alert history log tab (CRITICAL + WARNING only)
 *  - "Updated Xs ago" live label next to ● Live
 *  - Simulation speed slider in Settings (via SettingsView)
 */


public class CleanView implements DistanceModel.ReadingListener {

    // ══════════════════════════════════════════════════════════════════════
    // THEME  — two full palettes swapped on toggle
    // ══════════════════════════════════════════════════════════════════════
    private boolean darkMode = false;

    // Light palette
    private static final Color L_BG           = new Color(0xF8FAFC);
    private static final Color L_SURFACE      = Color.WHITE;
    private static final Color L_BORDER       = new Color(0xE2E8F0);
    private static final Color L_BORDER_LIGHT = new Color(0xF1F5F9);
    private static final Color L_TEXT_MAIN    = new Color(0x0F172A);
    private static final Color L_TEXT_MUTED   = new Color(0x64748B);
    private static final Color L_TEXT_SUBTLE  = new Color(0x94A3B8);

    // Dark palette
    private static final Color D_BG           = new Color(0x0F172A);
    private static final Color D_SURFACE      = new Color(0x1E293B);
    private static final Color D_BORDER       = new Color(0x334155);
    private static final Color D_BORDER_LIGHT = new Color(0x1E293B);
    private static final Color D_TEXT_MAIN    = new Color(0xF1F5F9);
    private static final Color D_TEXT_MUTED   = new Color(0x94A3B8);
    private static final Color D_TEXT_SUBTLE  = new Color(0x475569);

    // Status colours — same in both modes
    static final Color SAFE_TEXT = new Color(0x16A34A);
    static final Color SAFE_BG   = new Color(0xDCFCE7);
    static final Color SAFE_DOT  = new Color(0x22C55E);
    static final Color WARN_TEXT = new Color(0xD97706);
    static final Color WARN_BG   = new Color(0xFEF3C7);
    static final Color WARN_DOT  = new Color(0xF59E0B);
    static final Color CRIT_TEXT = new Color(0xE11D48);
    static final Color CRIT_BG   = new Color(0xFFE4E6);
    static final Color CRIT_DOT  = new Color(0xF43F5E);

    // Current palette (live references, swapped by applyTheme)
    private Color BG, SURFACE, BORDER, BORDER_LIGHT, TEXT_MAIN, TEXT_MUTED, TEXT_SUBTLE;

    // ══════════════════════════════════════════════════════════════════════
    // UNIT
    // ══════════════════════════════════════════════════════════════════════
    private enum Unit { CM, MM, M, FT }
    private Unit currentUnit = Unit.CM;

    private static final String[] UNIT_LABELS = {"cm", "mm", "m", "ft"};

    /** Convert a raw cm value to the currently selected unit. */
    private double convert(double cm) {
        switch (currentUnit) {
            case MM: return cm * 10.0;
            case M:  return cm / 100.0;
            case FT: return cm / 30.48;
            default: return cm;
        }
    }

    private String unitLabel() {
        return UNIT_LABELS[currentUnit.ordinal()];
    }

    private String fmt(double cm) {
        double v = convert(cm);
        if (currentUnit == Unit.M || currentUnit == Unit.FT)
            return String.format("%.2f %s", v, unitLabel());
        return String.format("%.0f %s", v, unitLabel());
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════════════════════════════
    private final DistanceModel model;
    private final UserAuth      userAuth;
    private int  tick = 0;
    private long lastReadingMs = 0;

    // ══════════════════════════════════════════════════════════════════════
    // COMPONENTS (built once, repainted on theme switch)
    // ══════════════════════════════════════════════════════════════════════
    private JFrame           frame;
    private RadialGaugePanel gaugePanel;
    private JLabel           statusBadge;
    private JPanel           alertBanner;
    private JLabel           alertLabel;
    private JLabel           avgLabel, minLabel, maxLabel, samplesLabel;
    private JLabel           updatedLabel;

    // History table
    private DefaultTableModel tableModel;
    private JScrollPane       tableScrollPane;
    private JPanel            tableCard;
    private JButton           collapseBtn;
    private boolean           tableVisible = true;

    // Alert history table
    private DefaultTableModel alertTableModel;

    // Chart
    private final XYSeries series    = new XYSeries("Distance");
    private ChartPanel     chartPanel;

    // Header buttons
    private JButton toggleBtn;
    private JButton settingsBtn;
    private JButton darkModeBtn;
    private JButton[] unitBtns;

    // All panels that need repainting on theme switch
    private JPanel headerPanel, bodyPanel;

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════
    public CleanView(DistanceModel model, UserAuth userAuth) {
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 16));

        this.model    = model;
        this.userAuth = userAuth;
        applyTheme();   // set BG/SURFACE/... to light values

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}

        gaugePanel    = new RadialGaugePanel();
        statusBadge   = new JLabel("● SAFE");
        alertBanner   = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        alertLabel    = new JLabel("");
        avgLabel      = monoLabel("--");
        minLabel      = monoLabel("--");
        maxLabel      = monoLabel("--");
        samplesLabel  = monoLabel("0");
        updatedLabel  = new JLabel("No data yet");
        tableModel    = new DefaultTableModel(
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
        unitBtns    = buildUnitBtns();

        frame = new JFrame("Distance Monitor");
        frame.setSize(1400, 900);
        frame.setMinimumSize(new Dimension(1200, 750));
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        headerPanel = buildHeader();
        bodyPanel   = buildBody();
        frame.add(headerPanel, BorderLayout.NORTH);
        frame.add(bodyPanel,   BorderLayout.CENTER);
        frame.setLocation(120, 80);

        // "Updated X ago" ticker
        new Timer(1000, e -> refreshUpdatedLabel()).start();
    }

    /** Backwards-compatible constructor (no UserAuth). */
    public CleanView(DistanceModel model) {
        this(model, null);
    }

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

        // All-readings table
        tableModel.insertRow(0, new Object[]{
            reading.getFormattedTimestamp(), distStr, reading.getStatus()
        });
        if (tableModel.getRowCount() > 100) tableModel.removeRow(tableModel.getRowCount() - 1);

        // Alert-only table
        if ("CRITICAL".equals(reading.getStatus()) || "WARNING".equals(reading.getStatus())) {
            alertTableModel.insertRow(0, new Object[]{
                reading.getFormattedTimestamp(), distStr, reading.getStatus()
            });
            if (alertTableModel.getRowCount() > 100)
                alertTableModel.removeRow(alertTableModel.getRowCount() - 1);
        }
    }

    // ── "Updated X ago" ───────────────────────────────────────────────────
    private void refreshUpdatedLabel() {
        if (lastReadingMs == 0) { updatedLabel.setText("No data yet"); return; }
        long secs = (System.currentTimeMillis() - lastReadingMs) / 1000;
        if (secs < 5)        updatedLabel.setText("Updated just now");
        else if (secs < 60)  updatedLabel.setText("Updated " + secs + "s ago");
        else                 updatedLabel.setText("Updated " + (secs/60) + "m ago");
    }

    // ══════════════════════════════════════════════════════════════════════
    // HEADER
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(0, 24, 0, 24)));

        // Left: logo + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        left.setOpaque(false);
        left.add(new RadarIconWidget());
        JLabel title = new JLabel("Distance Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_MAIN);
        left.add(title);

        // Right: status badge | units | dark mode | settings | start/stop
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);

        applyStatusStyle(statusBadge, "SAFE");
        right.add(statusBadge);

        // Unit toggle group
        JPanel unitGroup = buildUnitGroup();
        right.add(unitGroup);

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
                alertLabel.setText("⚠  Critical object detected — " + fmt(distanceCm));
                alertBanner.setBackground(CRIT_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0,0,1,0,CRIT_DOT));
                alertLabel.setForeground(CRIT_TEXT);
                alertBanner.setVisible(true);
                break;
            case "WARNING":
                alertLabel.setText("⚠  Object in warning zone — " + fmt(distanceCm));
                alertBanner.setBackground(WARN_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0,0,1,0,WARN_DOT));
                alertLabel.setForeground(WARN_TEXT);
                alertBanner.setVisible(true);
                break;
            default:
                alertBanner.setVisible(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // BODY
    // ══════════════════════════════════════════════════════════════════════
    private JPanel buildBody() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(buildAlertBannerPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
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

    // ── Left column: gauge + stat cards ───────────────────────────────────
    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Gauge card
        JPanel gaugeCard = card();
        gaugeCard.setLayout(new BoxLayout(gaugeCard, BoxLayout.Y_AXIS));
        gaugeCard.setBorder(new EmptyBorder(28, 28, 28, 28));
        JLabel gt = subtleLabel("CURRENT DISTANCE", 10);
        gt.setAlignmentX(Component.CENTER_ALIGNMENT);
        gt.setBorder(new EmptyBorder(0, 0, 10, 0));
        gaugeCard.add(gt);
        gaugePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gaugeCard.add(gaugePanel);
        col.add(gaugeCard);
        col.add(Box.createVerticalStrut(12));

        // Stats card — bigger font, cleaner layout
        JPanel statsCard = card();
        statsCard.setLayout(new GridLayout(4, 1, 0, 0));
        addStatRow(statsCard, "Average",  avgLabel,     true);
        addStatRow(statsCard, "Minimum",  minLabel,     true);
        addStatRow(statsCard, "Maximum",  maxLabel,     true);
        addStatRow(statsCard, "Samples",  samplesLabel, false);
        col.add(statsCard);
        return col;
    }

    private void addStatRow(JPanel card, String name, JLabel valueLabel, boolean divider) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));   // bigger
        lbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Consolas", Font.BOLD, 20)); // bigger
        valueLabel.setForeground(TEXT_MAIN);

        row.add(lbl,        BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);

        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(row, BorderLayout.CENTER);
        if (divider) w.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_LIGHT));
        card.add(w);
    }

    // ── Right column: chart + tabbed table ────────────────────────────────
    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Chart card — smaller max height
        JPanel chartCard = card();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel ct = new JLabel("Distance Timeline");
        ct.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ct.setForeground(TEXT_MAIN);

        // Live + Updated labels
        JPanel livePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        livePanel.setOpaque(false);
        JLabel live = new JLabel("● Live");
        live.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        live.setForeground(SAFE_DOT);
        updatedLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        updatedLabel.setForeground(TEXT_SUBTLE);
        livePanel.add(updatedLabel);
        livePanel.add(live);

        chartHeader.add(ct,        BorderLayout.WEST);
        chartHeader.add(livePanel, BorderLayout.EAST);

        chartPanel.setPreferredSize(new Dimension(0, 100));
        chartCard.add(chartHeader, BorderLayout.NORTH);
        chartCard.add(chartPanel,  BorderLayout.CENTER);
        col.add(chartCard);
        col.add(Box.createVerticalStrut(12));

        // Tabbed table area: All readings | Alert history
        tableCard = card();
        tableCard.setLayout(new BorderLayout());

        // Collapse toggle header
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);
        tableHeader.setBorder(new EmptyBorder(8, 14, 8, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        tabs.addTab("📋  All Readings", buildReadingsTable(tableModel, false));
        tabs.addTab("⚠  Alert History", buildReadingsTable(alertTableModel, true));

        collapseBtn = new JButton("▾  History");
        collapseBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        collapseBtn.setForeground(TEXT_MUTED);
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
        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(tabs,        BorderLayout.CENTER);

        col.add(tableCard);
        return col;
    }

    // ── Shared table builder ───────────────────────────────────────────────
    private JScrollPane buildReadingsTable(DefaultTableModel model, boolean alertOnly) {
        JTable table = new JTable(model);
        table.setFont(new Font("Consolas", Font.PLAIN, 15));
        table.setForeground(TEXT_MUTED);
        table.setBackground(SURFACE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_LIGHT);
        table.setRowHeight(40);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEFF6FF));
        table.setSelectionForeground(TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        header.setForeground(TEXT_SUBTLE);
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.LEFT);

        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHART
    // ══════════════════════════════════════════════════════════════════════
    private ChartPanel buildChart() {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        chart.setBackgroundPaint(SURFACE);
        chart.removeLegend();
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(4, 0, 4, 0));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(SURFACE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(BORDER_LIGHT);
        plot.setRangeGridlineStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 1f, new float[]{4,4}, 0));
        plot.setRangeGridlinesVisible(true);

        ((NumberAxis) plot.getDomainAxis()).setVisible(false);
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0, 110);
        range.setTickLabelFont(new Font("Consolas", Font.PLAIN, 10));
        range.setTickLabelPaint(TEXT_SUBTLE);
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
        cp.setMinimumDrawWidth(0);   cp.setMinimumDrawHeight(0);
        cp.setMaximumDrawWidth(4000); cp.setMaximumDrawHeight(4000);
        cp.setPopupMenu(null);
        cp.setBackground(SURFACE);
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
    // HEADER BUTTONS
    // ══════════════════════════════════════════════════════════════════════

    // ── Start / Stop ──────────────────────────────────────────────────────
    private JButton buildToggleBtn() {
        JButton btn = new JButton("⏹  Stop");
        btn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));   // bigger, bold
        btn.setPreferredSize(new Dimension(160,45));
        btn.setForeground(CRIT_TEXT);
        btn.setBackground(new Color(0xFEF2F2));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xFECACA), 1),
            new EmptyBorder(12, 24, 12, 24)));
        btn.addActionListener(e -> {
            boolean nowOn = !model.isMonitoring();
            model.setMonitoring(nowOn);
            if (nowOn) {
                btn.setText("⏹  Stop");
                btn.setForeground(CRIT_TEXT);
                btn.setBackground(new Color(0xFEF2F2));
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xFECACA), 2),
                    new EmptyBorder(12, 24, 12, 24)));
            } else {
                btn.setText("▶  Start");
                btn.setForeground(SAFE_TEXT);
                btn.setBackground(new Color(0xF0FDF4));
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xBBF7D0), 2),
                    new EmptyBorder(12, 24, 12, 24)));
            }
        });
        return btn;
    }

    // ── Settings ──────────────────────────────────────────────────────────
    private JButton buildSettingsBtn() {
        JButton btn = new JButton("⚙  Settings");
        btn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(160,45));
        btn.setForeground(new Color(0x3B82F6));
        btn.setBackground(new Color(0xEFF6FF));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xBFDBFE), 1),
            new EmptyBorder(6, 14, 6, 14)));
        btn.addActionListener(e ->
            new SettingsView(frame, model, userAuth).setVisible(true));
        return btn;
    }

    // ── Dark mode toggle ──────────────────────────────────────────────────
    private JButton buildDarkModeBtn() {
        JButton btn = new JButton("🌙");
        btn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
//        btn.setPreferredSize(new Dimension(140,40));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Toggle dark mode");
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(5, 10, 5, 10)));
        btn.addActionListener(e -> toggleDarkMode());
        return btn;
    }

    // ── Unit toggle buttons ───────────────────────────────────────────────
    private JButton[] buildUnitBtns() {
        JButton[] btns = new JButton[4];
        Unit[] units = Unit.values();
        for (int i = 0; i < 4; i++) {
            final Unit u = units[i];
            btns[i] = new JButton(UNIT_LABELS[i]);
            btns[i].setFont(new Font("Segoe UI", Font.PLAIN, 16));
//            btns[i].setPreferredSize(new Dimension(140,40));
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
        group.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(0, 0, 0, 0)));
        for (int i = 0; i < unitBtns.length; i++) {
            styleUnitBtn(unitBtns[i], i == 0);
            group.add(unitBtns[i]);
            if (i < unitBtns.length - 1) {
                JSeparator sep = new JSeparator(JSeparator.VERTICAL);
                sep.setPreferredSize(new Dimension(1, 22));
                sep.setForeground(BORDER);
                group.add(sep);
            }
        }
        return group;
    }

    private void styleUnitBtn(JButton btn, boolean active) {
        btn.setContentAreaFilled(active);
        if (active) {
            btn.setBackground(new Color(0x3B82F6));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(SURFACE);
            btn.setForeground(TEXT_MUTED);
        }
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        btn.setOpaque(active);
    }

    private void refreshUnitBtnStyles() {
        Unit[] units = Unit.values();
        for (int i = 0; i < unitBtns.length; i++) {
            styleUnitBtn(unitBtns[i], units[i] == currentUnit);
        }
    }

    /** Re-render all displayed values when unit changes. */
    private void refreshAllDisplayedValues() {
//        gaugePanel.setUnitLabel(unitLabel());
        if(model.getSampleCount() > 0){
            double latest = model.getHistory().get(model.getHistory().size() - 1).getDistance();
            gaugePanel.setValue(latest, unitLabel(), convert(latest));
        }
        if (model.getSampleCount() == 0) return;
        avgLabel.setText(fmt(model.getAverage()));
        minLabel.setText(fmt(model.getMin()));
        maxLabel.setText(fmt(model.getMax()));
        // Rewrite table with converted values — rebuild from model history
        tableModel.setRowCount(0);
        java.util.List<DistanceReading> history = model.getHistory();
        int start = Math.max(0, history.size() - 100);
        for (int i = history.size() - 1; i >= start; i--) {
            DistanceReading r = history.get(i);
            tableModel.addRow(new Object[]{
                r.getFormattedTimestamp(), fmt(r.getDistance()), r.getStatus()
            });
        }
        alertTableModel.setRowCount(0);
        for (int i = history.size() - 1; i >= 0; i--) {
            DistanceReading r = history.get(i);
            if ("CRITICAL".equals(r.getStatus()) || "WARNING".equals(r.getStatus())) {
                alertTableModel.addRow(new Object[]{
                    r.getFormattedTimestamp(), fmt(r.getDistance()), r.getStatus()
                });
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DARK MODE
    // ══════════════════════════════════════════════════════════════════════
    private void toggleDarkMode() {
        darkMode = !darkMode;
        applyTheme();
        darkModeBtn.setText(darkMode ? "☀" : "🌙");
        darkModeBtn.setToolTipText(darkMode ? "Switch to light mode" : "Switch to dark mode");

        SwingUtilities.invokeLater(() -> {
            try {
                if (darkMode) FlatDarkLaf.setup();
                else          FlatLightLaf.setup();
                SwingUtilities.updateComponentTreeUI(frame);
                gaugePanel.setTheme(TEXT_MAIN);
                applyThemeToComponents();
            } catch (Exception ignored) {}

            // Re-tint chart and frame background
            frame.getContentPane().setBackground(BG);
            updateChartTheme();
            frame.repaint();
        });
    }

    private void applyThemeToComponents(){
        frame.getContentPane().setBackground(BG);
        headerPanel.setBackground(SURFACE);
        bodyPanel.setBackground(BG);

        statusBadge.setForeground(TEXT_MAIN);

        //cards
        for(Component c : bodyPanel.getComponents()){
            c.setBackground(BG);
        }

        // Force repaint everything
        SwingUtilities.updateComponentTreeUI(frame);
        frame.repaint();
    }

    private void applyTheme() {
        if (darkMode) {
            BG           = D_BG;
            SURFACE      = D_SURFACE;
            BORDER       = D_BORDER;
            BORDER_LIGHT = D_BORDER_LIGHT;
            TEXT_MAIN    = D_TEXT_MAIN;
            TEXT_MUTED   = D_TEXT_MUTED;
            TEXT_SUBTLE  = D_TEXT_SUBTLE;
        } else {
            BG           = L_BG;
            SURFACE      = L_SURFACE;
            BORDER       = L_BORDER;
            BORDER_LIGHT = L_BORDER_LIGHT;
            TEXT_MAIN    = L_TEXT_MAIN;
            TEXT_MUTED   = L_TEXT_MUTED;
            TEXT_SUBTLE  = L_TEXT_SUBTLE;
        }
    }

    private void updateChartTheme() {
        XYPlot plot = chartPanel.getChart().getXYPlot();
        chartPanel.getChart().setBackgroundPaint(SURFACE);
        plot.setBackgroundPaint(SURFACE);
        plot.setRangeGridlinePaint(BORDER_LIGHT);
        NumberAxis axis = (NumberAxis) plot.getRangeAxis();
        axis.setTickLabelPaint(TEXT_SUBTLE);
        chartPanel.setBackground(SURFACE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATUS BADGE & HELPERS
    // ══════════════════════════════════════════════════════════════════════
    void applyStatusStyle(JLabel lbl, String status) {
        switch (status) {
            case "CRITICAL": lbl.setText("● CRITICAL"); lbl.setForeground(CRIT_TEXT); lbl.setBackground(CRIT_BG); break;
            case "WARNING":  lbl.setText("● WARNING");  lbl.setForeground(WARN_TEXT); lbl.setBackground(WARN_BG); break;
            default:         lbl.setText("● SAFE");     lbl.setForeground(SAFE_TEXT); lbl.setBackground(SAFE_BG);
        }
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));  // bigger
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(5, 12, 5, 12));
    }

    JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        return p;
    }

    JLabel monoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 16));
        l.setForeground(TEXT_MAIN);
        return l;
    }

    JLabel subtleLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, size));
        l.setForeground(TEXT_SUBTLE);
        return l;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RADIAL GAUGE  — now receives unit label + converted value
    // ══════════════════════════════════════════════════════════════════════
    static class RadialGaugePanel extends JPanel {
        private double target = 50, animated = 50;
        private double displayValue = 50;   // converted value for label
        private String unitLbl = "cm";
        private final Timer anim;
        private Color textColor = Color.BLACK;

        RadialGaugePanel() {
            setPreferredSize(new Dimension(320, 320));
            setOpaque(false);
            anim = new Timer(16, e -> {
                animated += (target - animated) * 0.10;
                repaint();
            });
            anim.start();
        }

        void setTheme(Color textColor){
            this.textColor = textColor;
            repaint();
        }
        /** @param rawCm  raw cm value (for arc calculation, always 0-100 scale)
         *  @param unit   label string, e.g. "ft"
         *  @param converted  converted value for the number display */
        void setValue(double rawCm, String unit, double converted) {
            target       = rawCm;
            unitLbl      = unit;
            displayValue = converted;
        }

        void setUnitLabel(String unit) { unitLbl = unit; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), cx = w/2, cy = h/2;
            int r = Math.min(w,h)/2 - 14;
            double pct = Math.min(animated/100.0, 1.0);
            String s = animated <= 15 ? "CRITICAL" : animated <= 35 ? "WARNING" : "SAFE";
            Color arc = "CRITICAL".equals(s) ? CRIT_DOT : "WARNING".equals(s) ? WARN_DOT : SAFE_DOT;

            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0xF1F5F9));
            g2.drawOval(cx-r, cy-r, r*2, r*2);
            g2.setColor(arc);
            g2.drawArc(cx-r, cy-r, r*2, r*2, 90, -(int)(pct*360));

            // Value — larger font
//            g2.setColor(new Color(0x0F172A));
            g2.setColor(textColor);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
            FontMetrics fm = g2.getFontMetrics();
            String val;
            if ("m".equals(unitLbl) || "ft".equals(unitLbl))
                val = String.format("%.2f", displayValue);
            else
                val = String.valueOf((int) Math.round(displayValue));
            g2.drawString(val, cx - fm.stringWidth(val)/2, cy + 9);

            // Unit label
            g2.setColor(new Color(0x94A3B8));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            fm = g2.getFontMetrics();
            g2.drawString(unitLbl, cx - fm.stringWidth(unitLbl)/2, cy + 26);
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
            g2.drawOval(1,1,19,19); g2.drawOval(5,5,11,11); g2.fillOval(9,9,4,4);
            g2.dispose();
        }
    }
}
