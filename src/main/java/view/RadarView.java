package view;

import model.DistanceModel;
import model.DistanceReading;

import javax.swing.*;
import java.awt.*;

/**
 * RadarPanel.
 * Now an instance class (not static) and implements ReadingListener.
 */
public class RadarView extends JPanel implements DistanceModel.ReadingListener {

    private double distance = 0;

    public RadarView() {
        setPreferredSize(new Dimension(400, 300));
        setBackground(Color.BLACK);
    }

    @Override
    public void onNewReading(DistanceReading reading) {
        this.distance = reading.getDistance();
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY = getHeight();

        // Radar lines
        g2.setColor(Color.GREEN);
        g2.drawLine(centerX, centerY, centerX, 0);
        g2.drawLine(centerX, centerY, 0,           centerY / 2);
        g2.drawLine(centerX, centerY, getWidth(),  centerY / 2);

        // Sensor dot
        g2.setColor(Color.WHITE);
        g2.fillOval(centerX - 6, centerY - 6, 12, 12);

        // Detected object
        int maxDistance = 100;
        int y = centerY - (int) ((distance / maxDistance) * centerY);
        g2.setColor(Color.RED);
        g2.fillOval(centerX - 5, y - 5, 10, 10);
    }
}
