package controller;

import com.fazecast.jSerialComm.SerialPort;
import model.AppConfig;
import model.DistanceModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * The Controller — reads sensor data (real or simulated),
 * feeds raw distance values into the Model.
 *
 * Rules:
 *  - No Swing imports here.
 *  - No status logic here — that belongs in DistanceModel.
 *  - No direct UI calls — views are notified via model listeners.
 */
public class AppController implements Runnable {

    private final DistanceModel      model;
    private final DatabaseController dbController;

    private SerialPort     port;
    private BufferedReader serialReader;
    private boolean        running = false;

    public AppController(DistanceModel model, DatabaseController dbController) {
        this.model        = model;
        this.dbController = dbController;
    }

    // ── Start / Stop ───────────────────────────────────────────────────────
    public void start() {
        if (!AppConfig.SIMULATION_MODE) {
            if (!connectSerial()) return;
        } else {
            System.out.println("⚙ Running in SIMULATION MODE");
        }
        running = true;
        Thread thread = new Thread(this, "SensorThread");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        disconnectSerial();
        dbController.disconnect();
    }

    // ── Main sensor loop ───────────────────────────────────────────────────
    @Override
    public void run() {
        Random random = new Random();

        while (running) {
            try {
                if (!model.isMonitoring()) {
                    Thread.sleep(500);
                    continue;
                }

                double distance = AppConfig.SIMULATION_MODE
                    ? simulateDistance(random)
                    : readFromSerial();

                if (distance < 0) continue; // bad serial line

                System.out.println("Distance: " + distance + " cm");

                // Hand raw value to the model — it classifies + notifies all listeners
                model.processReading(distance);

                System.out.println("--------------------------------");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ── Simulation ─────────────────────────────────────────────────────────
    private double simulateDistance(Random random) throws InterruptedException {
        Thread.sleep(AppConfig.SIMULATION_DELAY_MS);
        return random.nextInt(100);
    }

    // ── Serial ─────────────────────────────────────────────────────────────
    private boolean connectSerial() {
        port = SerialPort.getCommPort(AppConfig.SERIAL_PORT);
        port.setBaudRate(AppConfig.BAUD_RATE);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);

        if (!port.openPort()) {
            System.out.println("❌ Failed to open " + AppConfig.SERIAL_PORT);
            return false;
        }

        System.out.println("✅ Connected to " + AppConfig.SERIAL_PORT);
        serialReader = new BufferedReader(new InputStreamReader(port.getInputStream()));
        return true;
    }

    private double readFromSerial() {
        try {
            String line = serialReader.readLine();
            if (line == null || line.isEmpty()) return -1;
            return Double.parseDouble(line.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private void disconnectSerial() {
        try {
            if (serialReader != null) serialReader.close();
            if (port != null && port.isOpen()) port.closePort();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
