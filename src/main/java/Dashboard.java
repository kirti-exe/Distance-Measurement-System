import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Dashboard {
    static JLabel distanceLabel;
    static JLabel statusLabel;

    static DefaultTableModel tableModel;

    public static void start(ChartPanel chartPanel){

        JFrame frame = new JFrame("Distance Monitoring System");

        frame.setSize(900,700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        //------------------------------------------------
        // TOP PANEL
        //------------------------------------------------

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2,1));

        distanceLabel = new JLabel("Distance: -- cm", SwingConstants.CENTER);
        distanceLabel.setFont(new Font("Arial", Font.BOLD, 28));

        statusLabel = new JLabel("Status: --", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 24));

        topPanel.add(distanceLabel);
        topPanel.add(statusLabel);

        //------------------------------------------------
        // TABLE
        //------------------------------------------------

        String[] columns = {"Time", "Distance (cm)", "Status"};

        tableModel = new DefaultTableModel(columns, 0);

        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Object.class, new StatusColorRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 200));

        //------------------------------------------------
        // TABLE
        //------------------------------------------------

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);

        //------------------------------------------------
        // BUTTON PANEL
        //------------------------------------------------
        JPanel buttonPanel = new JPanel();

        JButton startButton = new JButton("Start Monitoring");
        JButton stopButton = new JButton("Stop Monitoring");

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        frame.add(buttonPanel, BorderLayout.WEST);

        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());

        frame.setVisible(true);
    }

    public static void update(double distance, String status){

        distanceLabel.setText("Distance: " + distance + " cm");
        statusLabel.setText("Status: " + status);

        if(status.equalsIgnoreCase("safe")){
            statusLabel.setForeground(Color.GREEN);
        }
        else if(status.equalsIgnoreCase("warning")){
            statusLabel.setForeground(Color.ORANGE);
        }
        else{
            statusLabel.setForeground(Color.RED);
        }

        refreshTable();
    }

    //------------------------------------------------
    // LOAD DATA FROM MYSQL
    //------------------------------------------------
    public static void refreshTable(){

        try{

            Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT `timestamp`, distance, status FROM distance_record ORDER BY id DESC LIMIT 10"
            );

            tableModel.setRowCount(0);

            while(rs.next()){
                Object[] row = {
                        rs.getString("timestamp"),
                        rs.getDouble("distance"),
                        rs.getString("status")
                };
                tableModel.addRow(row);
            }

            conn.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //------------------------------------------------
    // START / STOP METHODS
    //------------------------------------------------
    public static void startMonitoring(){
        ArduinoCOM3Reader.monitoring = true;
    }

    public static void stopMonitoring(){
        ArduinoCOM3Reader.monitoring = false;
    }
}