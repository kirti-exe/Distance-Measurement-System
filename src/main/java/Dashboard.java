import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;

public class Dashboard {
    static JLabel distanceLabel;
    static JLabel statusLabel;

    public static void start(ChartPanel chartPanel){

        JFrame frame = new JFrame("Distance Monitoring System");

        frame.setSize(900,600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2,1));

        distanceLabel = new JLabel("Distance: -- cm", SwingConstants.CENTER);
        distanceLabel.setFont(new Font("Arial", Font.BOLD, 28));

        statusLabel = new JLabel("Status: --", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 24));

        topPanel.add(distanceLabel);
        topPanel.add(statusLabel);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(chartPanel, BorderLayout.CENTER);

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
    }
}