import java.sql.Connection;


public class Main {

    public static void main(String[] args) {
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
            new ClientConn(Secrets.login, Secrets.pass, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}