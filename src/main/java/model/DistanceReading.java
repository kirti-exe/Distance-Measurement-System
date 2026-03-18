package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable data object representing a single sensor reading.
 * The Model never exposes raw primitives — always wrap in this.
 */
public class DistanceReading {

    private final double        distance;
    private final String        status;
    private final LocalDateTime timestamp;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DistanceReading(double distance, String status) {
        this.distance  = distance;
        this.status    = status;
        this.timestamp = LocalDateTime.now();
    }

    public double        getDistance()  { return distance; }
    public String        getStatus()    { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(FMT);
    }

    @Override
    public String toString() {
        return String.format("[%s] %.1f cm — %s", getFormattedTimestamp(), distance, status);
    }
}
