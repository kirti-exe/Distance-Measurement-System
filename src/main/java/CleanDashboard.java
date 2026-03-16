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
    private final JLabel statusbadge;
    private final JPanel alerBanner;
    private final JLabel alerLabel;
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
        UIManger.put("Table.rowHeight", 36);

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
        if(instance == null) return
        SwingUtilities.invokeLater(() -> instance.updateUI(distance, status));
    }

    //------------------------------------------------------------------------
    // CONSTRUCTOR
    //------------------------------------------------------------------------
    private CleanDashboard(){
        frame = new JFrame("Distance Monitor - Clean View");
        frame.setSize(980, 620);
        frame.setMinimumSize(new Dimension(800, 500));
        // Dont EXIT_ON_CLOSE - just hide so orginal windows stays open
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        //----Seed chart with flat line----------------------------------------
        for(int i=0; i<20; i++) series.add(i, 50.0);

        //----Build component refs-----------------------------------------
        gaugePanel = new RadialGaugePanel();
        statusBadge = new JLabel("SAFE");
        alertBanner - newJPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
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
        right.add(statusbadge);
        right.add(toggleBtn);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    //-------------------------------------------------------------------
    // Build Alert Banner
    //-------------------------------------------------------------------
    private JPanel buildAlertBanner() {
        alertBanner.setBackground(CRIT_BG);
    }

    private void updateAlertBanner(String status){

    }

    //-------------------------------------------------------------------
    // Build Body (left column + right column)
    //-------------------------------------------------------------------
    private JPanel buildBody(){

    }

    //----Left Column----------------------------------------------------
    private JPanel buildLeftColumn(){

    }

    private void addStatRow(JPanel card, String name, JLabel valueLabel, boolean divider){

    }

    //----Right Column-------------------------------------------------
    private JPanel buildRightColumn(){

    }

    //-------------------------------------------------------------------
    // JFreeChart
    //-------------------------------------------------------------------
    private ChartPanel buildChart(){

    }

    private void updateChartColor(String status){

    }

    //-------------------------------------------------------------------
    // Table
    //-------------------------------------------------------------------
    private JScrollPane buildTable(){

    }

    //-------------------------------------------------------------------
    // Toggle button
    //-------------------------------------------------------------------
    private JButton buildToggleBtn(){

    }

    //-------------------------------------------------------------------
    // Helpers
    //-------------------------------------------------------------------
    static void applyStatusStyle(JLabel lbl, String status){

    }

    static JPanel card(){

    }

    static JPanel monoLabel(String text){

    }

    static JLabel subtleLabel(String text, int size){

    }

    //-------------------------------------------------------------------
    // Radial Gauge - animated circular progress ring
    //-------------------------------------------------------------------
    static class RadialGaugePanel extends JPanel{

    }

    //-------------------------------------------------------------------
    // Status badge renderer for JTable
    //-------------------------------------------------------------------
    static class StatusBadgeRenderer extends DefaultTableCellRenderer {

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
