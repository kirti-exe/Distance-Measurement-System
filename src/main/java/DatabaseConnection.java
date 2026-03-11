import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static Connection connection;

    public static Connection getConnection(){

        try{

            if(connection == null || connection.isClosed()){
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/distance_system?useSSL=false",
                        "root",
                        "root123"
                );

                System.out.println("MySQL Connected");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return connection;
    }
}
