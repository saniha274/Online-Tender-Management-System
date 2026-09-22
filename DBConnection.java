import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/tms";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "Sana@27";

    public static Connection getConnection() throws Exception {

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    public static void main(String[] args) {

        try {

            Connection connection = getConnection();

            System.out.println("Database connected successfully! ✅");

            connection.close();

        } catch (Exception e) {

            System.out.println("Database connection failed! ❌");

            e.printStackTrace();
        }
    }
}