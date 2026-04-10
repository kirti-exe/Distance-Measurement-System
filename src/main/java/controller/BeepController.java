package controller;

import model.AppConfig;
import model.DistanceModel;
import model.DistanceReading;

import javax.sound.sampled.*;

public class BeepController implements DistanceModel.ReadingListener {

    private double  currentDistance = 100;
    private boolean running         = false;

    @Override
    public void onNewReading(DistanceReading reading) {
        currentDistance = reading.getDistance();
    }

    public void start() {
        running = true;
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    int delay = getBeepDelay();
                    if (delay > 0) {
                        float volume = getVolume();
                        int durationMs = getDuration();
                        playTone(880, durationMs, volume);
                        Thread.sleep(delay);
                    } else {
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "BeepThread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() { running = false; }

    // ── Beep delay based on distance ───────────────────────────────────
    private int getBeepDelay() {
        double critical = AppConfig.CRITICAL_THRESHOLD;
        double warning  = AppConfig.WARNING_THRESHOLD;

        if (currentDistance < critical) {
            return 150;   // very fast
        } else if (currentDistance < critical + (warning - critical) / 2) {
            return 400;   // fast
        } else if (currentDistance < warning) {
            return 700;   // medium
        } else {
            return 0;     // silent
        }
    }

    // ── Volume grows as object gets closer (0.0 to 1.0) ───────────────
    private float getVolume() {
        double critical = AppConfig.CRITICAL_THRESHOLD;
        double warning  = AppConfig.WARNING_THRESHOLD;

        if (currentDistance < critical) {
            return 1.0f;          // max volume — critical
        } else if (currentDistance < warning) {
            // Gradually increase from 0.3 to 0.9 as distance decreases
            double range = warning - critical;
            double pos   = warning - currentDistance;
            return 0.3f + (float)(pos / range) * 0.6f;
        } else {
            return 0f;            // silent
        }
    }

    // ── Beep duration — longer at critical ────────────────────────────
    private int getDuration() {
        double critical = AppConfig.CRITICAL_THRESHOLD;
        if (currentDistance < critical) return 120;
        return 80;
    }

    // ── Generate and play a tone at given frequency and volume ─────────
    private void playTone(int frequency, int durationMs, float volume) {
        try {
            // Audio format: 44100 Hz, 16-bit, mono
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) return;

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);

            // Set volume via FloatControl
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(
                        FloatControl.Type.MASTER_GAIN);
                // Convert 0.0-1.0 volume to decibels
                float dB = (float)(Math.log10(Math.max(volume, 0.0001)) * 20);
                dB = Math.max(gain.getMinimum(), Math.min(dB, gain.getMaximum()));
                gain.setValue(dB);
            }

            line.start();

            // Generate sine wave samples
            int samples    = (int)(44100 * durationMs / 1000.0);
            byte[] buffer  = new byte[samples * 2];
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * frequency / 44100;
                short  sample = (short)(Math.sin(angle) * 32767 * volume);
                buffer[i * 2]     = (byte)(sample & 0xFF);
                buffer[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();

        } catch (Exception e) {
            // Fallback to system beep if audio fails
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }
}