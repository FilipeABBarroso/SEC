package dbController;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static Connection connection;

    private static void initConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/SECDatabse",
                            "postgres", "postgres");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    public static Connection getConnection() {
        if (connection == null){
            initConnection();
        }
        return connection;

    }
}
