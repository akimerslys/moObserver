import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Connection conn = null;

        try {
            // Get the database connection
            conn = Database.getConnection();
            // Create tables
            Database.createTables(conn);
            System.out.println("Tables created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        start(conn);

    }

    private static void start(Connection conn) {
        try {
            // Create a new ClientConn instance
            new ClientConn(1, Secrets.mo_login, Secrets.mo_pass, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}