package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Model — owns ALL application state and business logic.
 *
 * Rules:
 *  - No Swing imports here ever.
 *  - Status classification lives here, not in the controller or view.
 *  - Views register as listeners; the model notifies them on each update.
 */
public class DistanceModel {

    // ── Listener interface ─────────────────────────────────────────────────
    public interface ReadingListener {
        void onNewReading(DistanceReading reading);
    }

    // ── State ──────────────────────────────────────────────────────────────
    private final List<DistanceReading> history  = new ArrayList<>();
    private final List<ReadingListener> listeners = new ArrayList<>();
    private boolean monitoring = true;

    // ── Listener management ────────────────────────────────────────────────
    public void addListener(ReadingListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ReadingListener listener) {
        listeners.remove(listener);
    }

    // ── Core business logic: status classification ─────────────────────────
    /**
     * Classifies a raw distance value using the thresholds in AppConfig.
     * All status strings are now uppercase and consistent.
     */
    public static String classifyStatus(double distance) {
        if (distance < AppConfig.CRITICAL_THRESHOLD) return "CRITICAL";
        if (distance < AppConfig.WARNING_THRESHOLD)  return "WARNING";
        return "SAFE";
    }

    // ── Process a new reading ──────────────────────────────────────────────
    /**
     * Called by the controller with a raw distance value.
     * The model classifies it, stores it, and notifies all listeners.
     */
    public void processReading(double distance) {
        String status = classifyStatus(distance);
        DistanceReading reading = new DistanceReading(distance, status);
        history.add(reading);
        notifyListeners(reading);
    }

    private void notifyListeners(DistanceReading reading) {
        for (ReadingListener l : listeners) {
            l.onNewReading(reading);
        }
    }

    // ── State accessors ────────────────────────────────────────────────────
    public boolean isMonitoring()            { return monitoring; }
    public void    setMonitoring(boolean m)  { this.monitoring = m; }

    public List<DistanceReading> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public DistanceReading getLatestReading() {
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }

    // ── Statistics (used by views) ─────────────────────────────────────────
    public double getAverage() {
        if (history.isEmpty()) return 0;
        return history.stream().mapToDouble(DistanceReading::getDistance).average().orElse(0);
    }

    public double getMin() {
        if (history.isEmpty()) return 0;
        return history.stream().mapToDouble(DistanceReading::getDistance).min().orElse(0);
    }

    public double getMax() {
        if (history.isEmpty()) return 0;
        return history.stream().mapToDouble(DistanceReading::getDistance).max().orElse(0);
    }

    public int getSampleCount() {
        return history.size();
    }
}
