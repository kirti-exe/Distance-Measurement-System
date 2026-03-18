package model;

/**
 * Central configuration — change thresholds, DB credentials,
 * and simulation mode all in one place.
 */
public class AppConfig {

    // ── Sensor mode ────────────────────────────────────────────────────────
    public static boolean SIMULATION_MODE = true;
    public static String  SERIAL_PORT     = "COM3";
    public static int     BAUD_RATE       = 9600;
    public static int     SIMULATION_DELAY_MS = 2000;

    // ── Distance thresholds (cm) ───────────────────────────────────────────
    public static double CRITICAL_THRESHOLD = 10.0;
    public static double WARNING_THRESHOLD  = 30.0;

    // ── Database ───────────────────────────────────────────────────────────
    public static final String DB_URL      = "jdbc:mysql://localhost:3306/distance_system?useSSL=false";
    public static final String DB_USER     = "root";
    public static final String DB_PASSWORD = "root123";

    private AppConfig() {}
}
