import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DatabaseManager {

    static String url = "jdbc:mysql://localhost:3306/distance_system";
    static String user = "root";
    static String password = "root123"; // your mysql password

    public static void save(double distance, String status) {

        try {

            Connection conn = DriverManager.getConnection(url, user, password);

            String sql = "INSERT INTO distance_record(distance, status) VALUES (?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setDouble(1, distance);
            stmt.setString(2, status);

            stmt.executeUpdate();

            conn.close();

            System.out.println("✔ Data stored in MySQL");

        } catch (Exception e) {
            System.out.println("Database error");
            e.printStackTrace();
        }
    }
}