import javax.swing.*;
import java.awt.*;
public class RadarPanel extends JPanel {
    static double distance = 0;

    public RadarPanel(){
        setPreferredSize(new Dimension(400,300));
        setBackground(Color.BLACK);
    }

    public static void update(double newDistance){
        distance = newDistance;
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.GREEN);

        int centerX = getWidth()/2;
        int centerY = getHeight();

        // Draw radar lines
        g2.drawLine(centerX, centerY, centerX, 0);
        g2.drawLine(centerX, centerY, 0, centerY/2);
        g2.drawLine(centerX, centerY, getWidth(), centerY/2);

        // Draw sensor
        g2.setColor(Color.WHITE);
        g2.fillOval(centerX-6, centerY-6,12,12);

        // Convert distance to screen position
        int maxDistance = 100;
        int y = centerY - (int)((distance/maxDistance) * centerY);

        // Draw detected object
        g2.setColor(Color.RED);
        g2.fillOval(centerX-5, y-5, 10, 10);
    }
}
