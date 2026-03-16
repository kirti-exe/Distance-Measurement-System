import javax.swing.*;
import java.awt.*;

public class DigitalDistanceDisplay {
    static JLabel digitalLabel;

    public static JPanel createDisplay(){

        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        panel.setLayout(new BorderLayout());

        JLabel title = new JLabel("DISTANCE", SwingConstants.CENTER);
        title.setForeground(Color.GRAY);
        title.setFont(new Font("Arial", Font.BOLD, 16));

        digitalLabel = new JLabel("000 cm", SwingConstants.CENTER);
        digitalLabel.setForeground((Color.GREEN));
        digitalLabel.setFont(new Font("Monospaced", Font.BOLD, 60));

        panel.add(title, BorderLayout.NORTH);
        panel.add(digitalLabel, BorderLayout.CENTER);

        return panel;
    }

    public static void update(double distance){
        int value = (int)distance;

        String formatted = String.format("%03d cm", value);

        digitalLabel.setText(formatted);

        if(value < 100){
            digitalLabel.setForeground(Color.RED);
        }else if(value < 30){
            digitalLabel.setForeground(Color.ORANGE);
        }else{
            digitalLabel.setForeground(Color.GREEN);
        }
    }
}
