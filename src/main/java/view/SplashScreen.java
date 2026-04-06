package view;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow{

    private float opacity = 0f;
    private Timer fadeIn;
    private Timer fadeOut;
    private Timer hold;

    public SplashScreen(){
        setSize(480,300);
        setLocationRelativeTo(null);
        setBackground(new Color(0,0,0,0));
        buildUI();
    }

    public void showAndWait() {
        setOpacity(0f);
        setVisible(true);

        // Fade in
        fadeIn = new Timer(20, null);
        fadeIn.addActionListener(e -> {
            opacity += 0.05f;
            if(opacity >= 1f) {
                opacity = 1f;
                fadeIn.stop();
                startHold();
            }
            setOpacity(opacity);
        });
        fadeIn.start();
    }

    private void  startHold() {
        // Stay visible for 2.5 seconds then fade out
        hold = new Timer(2500, e -> {
           hold.stop();
           startFadeOut();
        });
        hold.setRepeats(false);
        hold.start();
    }

    private void startFadeOut() {
        fadeOut = new Timer(20, null);
        fadeOut.addActionListener(e -> {
            opacity -= 0.05f;
            if(opacity <= 0f) {
                opacity = 0f;
                fadeOut.stop();
                setVisible(false);
                dispose();
                onSplashFinished();
            }
            setOpacity(opacity);
        });
        fadeOut.start();
    }

    protected void onSplashFinished() {
        // overridden in Main
    }

    private void buildUI() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                g2.setColor(new Color(0x0F172A));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40,50,40,50));

        // Animated radar icon
        RadarAnimation radar = new RadarAnimation();
        radar.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(radar);
        panel.add(Box.createVerticalStrut(20));

        // Project name
        JLabel title = new JLabel("Ultrasonic Distance Monitor");
        title.setFont(new Font("Segoe UI", Font.BOLD,22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        // Subtitle
        JLabel sub = new JLabel("Initializing system...");
        sub.setFont(new Font("Segoe UI", Font.PLAIN,13));
        sub.setForeground(new Color(0x64748B));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(24));

        // Loading bar
        JProgressBar bar = new JProgressBar(0,100);
        bar.setIndeterminate(false);
        bar.setValue(0);
        bar.setMaximumSize(new Dimension(320, 6));
        bar.setPreferredSize(new Dimension(320, 6));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setForeground(new Color(0x3B82F6));
        bar.setBackground(new Color(0x1E293B));
        bar.setBorderPainted(false);
        panel.add(bar);

        // Animated progress bar
        Timer progress = new Timer(30, null);
        progress.addActionListener(e -> {
            int v = bar.getValue();
            if (v < 100) bar.setValue(v + 2);
            else progress.stop();
        });
        progress.start();

        setContentPane(panel);
    }

    static class RadarAnimation extends JComponent {
        private int angle = 0;
        private final Timer anim;

        RadarAnimation() {
            setPreferredSize(new Dimension(80,80));
            setMaximumSize(new Dimension(80,80));
            anim = new Timer(16, e -> {
               angle = (angle + 3) % 360;
               repaint();
            });
            anim.start();
        }

        void stop() { anim.stop(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2, cy = getHeight() / 2;
            int r = Math.min(cx,cy) - 4;

            // Rings
            g2.setColor(new Color(0x1E293B));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx - r,cy - r,r * 2,r * 2);
            g2.drawOval(cx - r/2,cy - r/2, r, r);

            // Sweep
            g2.setColor(new Color(0x3B82F6, false));
            for (int i = 0; i < 60; i++) {
                int a = (angle - i + 360) % 360;
                float alpha = (60 - i) / 60f * 0.6f;
                g2.setColor(new Color(59/255f, 130/255f, 246/255f, alpha));
                g2.setStroke(new BasicStroke(2f));
                double rad = Math.toRadians(a);
                int x2 = cx + (int)(r * Math.cos(rad));
                int y2 = cy + (int)(r * Math.sin(rad));
                g2.drawLine(cx, cy, x2, y2);
            }

            // Center dot
            g2.setColor(new Color(0x3B82F6));
            g2.fillOval(cx - 4,cy - 4,8,8);
            g2.dispose();
        }
    }
}
