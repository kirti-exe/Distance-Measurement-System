import com.formdev.flatlaf.FlatLightLaf;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.awt.Component.CENTER_ALIGNMENT;

public class CleanDashboard {
    //-----Palette------------------------------------------------------
    static final Color BG = new Color(0xF8FAFC);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xE2E8F0);
    static final Color BORDER_LIGHT = new Color(0xF1F5F9);
    static final Color TEXT_MAIN = new Color(0x0F172A);
    static final Color TEXT_MUTED = new Color(0x64748B);
    static final Color TEXT_SUBTLE = new Color(0x94A3B8);

    static final Color SAFE_TEXT = new Color(0x16A34A);
    static final Color SAFE_BG = new Color(0xDCFCE7);
    static final Color SAFE_DOT = new Color(0x22C55E);
    static final Color WARN_TEXT = new Color(0xD97706);
    static final Color WARN_BG = new Color(0xFEF3C7);
    static final Color WARN_DOT = new Color(0xF59E0B);
    static final Color CRIT_TEXT = new Color(0xE11D48);
    static final Color CRIT_BG = new Color(0xFFE4E6);
    static final Color CRIT_DOT = new Color(0xF43F5E);

    //-----Shared state (static so update() can reach  it)---------------
    private static CleanDashboard instance;

    private final JFrame frame;
    private final RadialGaugePanel gaugePanel;
    private final JLabel statusBadge;
    private final JPanel alertBanner;
    private final JLabel alertLabel;
    private final JLabel avgLabel, minLabel, maxLabel, samplesLabel;
    private final DefaultTableModel tableModel;
    private final XYSeries series = new XYSeries("Distance");
    private final ChartPanel chartPanel;
    private final JButton toggleBtn;

    private double runningSum = 0;
    private double runningMin = Double.MAX_VALUE;
    private double runningMax = Double.MIN_VALUE;
    private int sampleCount = 0;
    private int tick = 0;

    //---------------------------------------------------------------
    // PUBLIC API
    //---------------------------------------------------------------

    /** Call once at startup to create and show the window. */
    public static void launch(){
        // Install FlatLaf - safe to call even if already installed
        try{FlatLightLaf.setup(); } catch (Exception ignored) {}
        UIManager.put("Table.rowHeight", 36);

        SwingUtilities.invokeLater(() -> {
            instance = new CleanDashboard();
            instance.frame.setVisible(true);
        });
    }

    /**
     * Call this wherever you call Dashboard.update(distance, status),
     * It si thread-safe - can be called from any thread
     */
    public static void update(double distance, String status){
        if(instance == null) return;
        SwingUtilities.invokeLater(() -> instance.updateUI(distance, status));
    }

    //------------------------------------------------------------------------
    // CONSTRUCTOR
    //------------------------------------------------------------------------
    private CleanDashboard(){
        frame = new JFrame("Distance Monitor - Clean View");
        frame.setSize(980, 620);
        frame.setMinimumSize(new Dimension(800, 500));
        // Don't EXIT_ON_CLOSE - just hide so original windows stays open
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        //----Seed chart with flat line----------------------------------------
        for(int i=0; i<20; i++) series.add(i, 50.0);

        //----Build component refs-----------------------------------------
        gaugePanel = new RadialGaugePanel();
        statusBadge = new JLabel("SAFE");
        alertBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        alertLabel = new JLabel("");
        avgLabel = monoLabel("--cm");
        minLabel = monoLabel("--cm");
        maxLabel = monoLabel("--cm");
        samplesLabel = monoLabel("0");
        tableModel = new DefaultTableModel(new String[]{"Timestamp", "Distance", "Status"},  0){
            public boolean isCellEditable(int r, int c) { return false; }
        };
        chartPanel = buildChart();
        toggleBtn = buildToggleBtn();

        //----Assemble frame--------------------------------------------
        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildAlertBanner(), BorderLayout.CENTER); // warpped in body below
        frame.add(buildBody(), BorderLayout.CENTER);

        // Position to the right of where the original window likely sits
        frame.setLocation(920, 80);
    }

    //-------------------------------------------------------------------
    // UI update - called on EDT
    //-------------------------------------------------------------------
    private void updateUI(double distance, String status){
        tick++;
        series.add(tick, distance);
        if(series.getItemCount() > 40) series.remove(0);

        // Running stats
        sampleCount++;
        runningSum += distance;
        runningMin = Math.min(runningMin, distance);
        runningMax = Math.min(runningMax, distance);

        // Gauge
        gaugePanel.setValue(distance);

        // Status badge
        applyStatusStyle(statusBadge, status);

        // Alert banner
        updateAlertBanner(status);

        // Chart color
        updateChartColor(status);

        // Status
        avgLabel.setText(Math.round(runningSum/sampleCount) + " cm");
        minLabel.setText(Math.round(runningMin) + " cm");
        maxLabel.setText(Math.round(runningMax) + " cm");
        samplesLabel.setText(String.valueOf(sampleCount));

        // Table - newest row at top
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        tableModel.insertRow(0, new Object[] {ts, distance + " cm", status});
        if(tableModel.getRowCount() > 50) tableModel.removeRow(tableModel.getRowCount() - 1);
    }

    //-------------------------------------------------------------------
    // Build Header
    //-------------------------------------------------------------------
    private JPanel buildHeader(){
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SURFACE);
        header.setPreferredSize(new Dimension(0, 54));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 24, 0, 24)
        ));

        // Left: icon + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(new RadarIconWidget());
        JLabel title = new JLabel("Distance Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_MAIN);
        left.add(title);
        header.add(left, BorderLayout.WEST);

        // Right: badge + button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        applyStatusStyle(statusBadge, "SAFE");
        right.add(statusBadge);
        right.add(toggleBtn);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    //-------------------------------------------------------------------
    // Build Alert Banner
    //-------------------------------------------------------------------
    private JPanel buildAlertBanner() {
        alertBanner.setBackground(CRIT_BG);
        alertBanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CRIT_DOT));
        alertBanner.setVisible(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(CRIT_DOT);
        dot.setFont(new Font("Seoge UI", Font.PLAIN, 8));
        alertBanner.add(dot);

        alertLabel.setFont(new Font("Seoge UI", Font.PLAIN, 13));
        alertLabel.setForeground(CRIT_TEXT);
        alertBanner.add(alertLabel);

        return alertBanner;
    }

    private void updateAlertBanner(String status){
        switch(status.toUpperCase()){
            case "CRITICAL":
                alertLabel.setText("Critical object detected - distance below 15 cm");
                alertBanner.setBackground(CRIT_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0,0,1,0, CRIT_DOT));
                alertLabel.setForeground(CRIT_TEXT);
                alertBanner.setVisible(true);
                break;
            case "WARNING":
                alertLabel.setText("Object in warning zone - distance below 35 cm");
                alertBanner.setBackground(WARN_BG);
                alertBanner.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, WARN_DOT));
                alertLabel.setForeground(WARN_TEXT);
                alertBanner.setVisible(true);
                break;
            default:
                alertBanner.setVisible(false);
        }
    }

    //-------------------------------------------------------------------
    // Build Body (left column + right column)
    //-------------------------------------------------------------------
    private JPanel buildBody(){
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(buildAlertBanner(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(18, 24,18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0,14);

        // Left column - fixed width
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.ipadx = 250;
        body.add(buildLeftColumn(), gbc);

        // Right column - fills remaining space
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.ipadx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildRightColumn(), gbc);

        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    //----Left Column----------------------------------------------------
    private JPanel buildLeftColumn(){
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Gauge card
        JPanel gaugeCard = card();
        gaugeCard.setLayout(new BoxLayout(gaugeCard, BoxLayout.Y_AXIS));
        gaugeCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel gaugeTitle = subtleLabel("CURRENT DISTANCE", 10);
        gaugeTitle.setAlignmentX(CENTER_ALIGNMENT);
        gaugeTitle.setBorder(new EmptyBorder(0, 0, 0, 0));
        gaugeCard.add(gaugeTitle);

        gaugePanel.setAlignmentX(CENTER_ALIGNMENT);
        gaugeCard.add(gaugePanel);
        col.add(gaugeCard);

        col.add(Box.createVerticalStrut(12));
        // Stats card;
        JPanel statsCard = card();
        statsCard.setLayout(new GridLayout(4, 1, 0 ,0));
        addStatRow(statsCard, "Average", avgLabel, true);
        addStatRow(statsCard, "Minimum", minLabel, true);
        addStatRow(statsCard, "Maximum", maxLabel, true);
        addStatRow(statsCard, "Samples", samplesLabel, false);
        col.add(statsCard);

        return col;

    }

    private void addStatRow(JPanel card, String name, JLabel valueLabel, boolean divider){
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10,16,10,16));
        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("Seogoe UI", Font.PLAIN,13));
        lbl.setForeground(TEXT_MUTED);

        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(row,BorderLayout.CENTER);
        if(divider) wrapper.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_LIGHT));
        card.add(wrapper);
    }

    //----Right Column-------------------------------------------------
    private JPanel buildRightColumn(){
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Chart card
        JPanel chartCard = card();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(14,18,14,18));

        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);
        chartHeader.setBorder(new EmptyBorder(0,0,10,0));

        JLabel chartTitle = new JLabel("Distance Timeline");
        chartTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chartTitle.setForeground(TEXT_MAIN);

        JLabel liveLabel = new JLabel("● Live");
        liveLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        liveLabel.setForeground(SAFE_DOT);

        chartHeader.add(chartTitle, BorderLayout.WEST);
        chartHeader.add(liveLabel, BorderLayout.EAST);
        chartCard.add(chartHeader, BorderLayout.NORTH);
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        col.add(chartCard);

        col.add(Box.createVerticalStrut(12));

        // Table Card
        JPanel tableCard = card();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(buildTable(), BorderLayout.CENTER);
        col.add(tableCard);

        return col;
    }

    //-------------------------------------------------------------------
    // JFreeChart
    //-------------------------------------------------------------------
    private ChartPanel buildChart(){
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
        chart.setBackgroundPaint(SURFACE);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(4,0,4,0));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(SURFACE);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(BORDER_LIGHT);
        plot.setRangeGridlineStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 1f, new float[]{4,4}, 0));
        plot.setRangeGridlinesVisible(true);

        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setVisible(false);

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(0, 110);
        range.setTickLabelFont(new Font("Consolas", Font.PLAIN, 10));
        range.setTickLabelPaint(TEXT_SUBTLE);
        range.setAxisLineVisible(false);
        range.setTickMarksVisible(false);

        // Line
        XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true, false);
        line.setSeriesPaint(0, SAFE_DOT);
        line.setSeriesStroke(0, new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        plot.setRenderer(0, line);

        // Area fill
        XYAreaRenderer area = new XYAreaRenderer();
        area.setSeriesPaint(0, new Color(34, 197, 94, 35));
        plot.setDataset(1, dataset);
        plot.setRenderer(1, area);
        plot.mapDatasetToRangeAxis(1, 0);

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(0, 140));
        cp.setMinimumDrawWidth(0);
        cp.setMinimumDrawHeight(0);
        cp.setMaximumDrawWidth(4000);
        cp.setMaximumDrawHeight(4000);
        cp.setPopupMenu(null);
        cp.setBackground(SURFACE);
        return cp;
    }

    private void updateChartColor(String status){
        Color c = status.equalsIgnoreCase("CRITICAL") ? CRIT_DOT
                : status.equalsIgnoreCase("WARNING") ? WARN_DOT
                : SAFE_DOT;
        XYPlot plot = chartPanel.getChart().getXYPlot();
        ((XYLineAndShapeRenderer) plot.getRenderer(0)).setSeriesPaint(0,c);
        plot.getRenderer(1).setSeriesPaint(0, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
    }

    //-------------------------------------------------------------------
    // Table
    //-------------------------------------------------------------------
    private JScrollPane buildTable(){
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Consolas", Font.PLAIN, 12));
        table.setForeground(TEXT_MUTED);
        table.setBackground(SURFACE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xF8FAFC));
        table.setRowHeight(34);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(new Color(0xF1F5F9));
        table.setSelectionForeground(TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setForeground(TEXT_SUBTLE);
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createMatteBorder(0,0,1,0, BORDER));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Status column badge renderer
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    //-------------------------------------------------------------------
    // Toggle button
    //-------------------------------------------------------------------
    private JButton buildToggleBtn(){
        JButton btn = new JButton("Stop Monitoring");
        btn.setFont(new Font("Seogoe UI", Font.PLAIN, 12));
        btn.setForeground(CRIT_TEXT);
        btn.setBackground(new Color(0xFEF2F2));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFECACA), 1),
                new EmptyBorder(5,14,5,14)
        ));
        btn.addActionListener(e -> {
            // Mirror the toggle to your exisitng ArduinoReader
            boolean nowMonitoring = !ArduinoCOM3Reader.monitoring;
            ArduinoCOM3Reader.monitoring = nowMonitoring;
            if(nowMonitoring){
                btn.setText("Stop Monitoring");
                btn.setForeground(CRIT_TEXT);
                btn.setBackground(new Color(0xFEF2F2));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xFECACA), 1),
                        new EmptyBorder(5,14,5,14)));
            } else {
                btn.setText("Start Monitoring");
                btn.setForeground(SAFE_TEXT);
                btn.setBackground(new Color(0xF0FDF4));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xBBF7D0), 1),
                        new EmptyBorder(5,14,5,14)));
            }
        });
        return btn;
    }

    //-------------------------------------------------------------------
    // Helpers
    //-------------------------------------------------------------------
    static void applyStatusStyle(JLabel lbl, String status){
        switch (status.toUpperCase()){
            case "CRITICAL":
                lbl.setText("● CRITICAL");
                lbl.setForeground(CRIT_TEXT);
                lbl.setBackground(CRIT_BG);
                break;
            case "WARNING":
                lbl.setText("● WARNING");
                lbl.setForeground(WARN_TEXT);
                lbl.setBackground(WARN_BG);
                break;
            default:
                lbl.setText("● SAFE");
                lbl.setForeground(SAFE_TEXT);
                lbl.setBackground(SAFE_BG);
                break;
        }
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
    }

    static JPanel card(){
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createLineBorder(BORDER,1));
        return p;
    }

    static JLabel monoLabel(String text){
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 13));
        l.setForeground(TEXT_MAIN);
        return l;
    }

    static JLabel subtleLabel(String text, int size){
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, size));
        l.setForeground(TEXT_SUBTLE);
        return l;
    }

    //-------------------------------------------------------------------
    // Radial Gauge - animated circular progress ring
    //-------------------------------------------------------------------
    static class RadialGaugePanel extends JPanel{
        private double target = 50, animated = 50;
        private final javax.swing.Timer anim;

        RadialGaugePanel(){
            setPreferredSize(new Dimension(155,155));
            setOpaque(false);
            anim = new javax.swing.Timer(16, e -> {
                animated += (target - animated) * 0.10;
                repaint();
            });
            anim.start();
        }

        void setValue(double v) { target = v; }

        @Override
        protected  void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth(), h = getHeight();
            int cx = w/2, cy = h/2;
            int r = Math.min(w,h)/2-16;
            float sw = 9f;

            double pct = Math.min(animated/100.0, 1.0);
            String status = animated <= 15 ? "CRITICAL" : animated <= 35 ? "WARNING" : "SAFE";
            Color arc = status.equals("CRITICAL") ? CRIT_DOT : status.equals("WARNING") ? WARN_DOT : SAFE_DOT;

            //Track ring
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0xF1F5F9));
            g2.drawOval(cx-r, cy-r, r*2, r*2);

            // Progress arc
            g2.setColor(arc);
            g2.drawArc(cx-r, cy-r, r*2, r*2, 90, -(int)(pct*360));

            // Value
            g2.setColor(TEXT_MAIN);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            String val = String.valueOf((int) Math.round(animated));
            g2.drawString(val, cx-fm.stringWidth(val) / 2, cy+22);

            // Unit
            g2.setColor(TEXT_SUBTLE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            g2.drawString("cm", cx-fm.stringWidth("cm")/2, cy+22);

            g2.dispose();
        }
    }

    //-------------------------------------------------------------------
    // Status badge renderer for JTable
    //-------------------------------------------------------------------
    static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            String s = val != null ? val.toString().toUpperCase() : "SAFE";
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setOpaque(true);
            if (sel) {
                setBackground(t.getSelectionBackground());
                setForeground(TEXT_MAIN);
                return this;
            }
            switch (s) {
                case "CRITICAL":
                    setForeground(CRIT_TEXT);
                    setBackground(CRIT_BG);
                    break;
                case "WARNING":
                    setForeground(WARN_TEXT);
                    setBackground(WARN_BG);
                    break;
                default:
                    setForeground(SAFE_TEXT);
                    setBackground(SAFE_BG);
                    break;
            }
            return this;
        }
    }

    //-------------------------------------------------------------------
    // Small radar icon drawn in code - no image file needed
    //-------------------------------------------------------------------
    static class RadarIconWidget extends JComponent{
        RadarIconWidget() { setPreferredSize(new Dimension(20,20)); }
        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x3B82F6));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(1, 1, 17, 17);
            g2.drawOval(5, 5, 9, 9);
            g2.fillOval(8, 8, 4, 4);
            g2.dispose();
        }
    }
}
