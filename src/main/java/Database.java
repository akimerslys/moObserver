import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Secrets.get_db_url(), Secrets.dbUser, Secrets.dbPass);
    }

    public static void createTables(Connection conn) {
        try (Statement statement = conn.createStatement()) {

            // Create PlayerMsg table
            String createPlayerMsgTable = "CREATE TABLE IF NOT EXISTS Messages (" +
                    "id SERIAL PRIMARY KEY, " +
                    "author VARCHAR(10) NOT NULL, " +
                    "message VARCHAR(255) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            statement.executeUpdate(createPlayerMsgTable);
            System.out.println("PlayerMsg table initialized successfully.");

            // Create PlayerStats table
            // login, mmr, win, lose, clan, color, last_updated
            String createPlayerStatsTable = "CREATE TABLE IF NOT EXISTS PlayerStats (" +
                    "id SERIAL PRIMARY KEY, " +
                    "login VARCHAR(10) UNIQUE NOT NULL, " +
                    "mmr INT NOT NULL, " +
                    "win INT, " +
                    "lose INT, " +
                    "clan VARCHAR(64), " +
                    "color VARCHAR(12), " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            statement.executeUpdate(createPlayerStatsTable);
            System.out.println("PlayerStats table initialized successfully.");

            // Create PlayerStatsHistory table
            String createPlayerStatsHistoryTable = "CREATE TABLE IF NOT EXISTS PlayerStatsHistory (" +
                    "id SERIAL PRIMARY KEY, " +
                    "player_id INT NOT NULL REFERENCES PlayerStats(id) ON DELETE CASCADE, " +
                    "changetype VARCHAR(12), " +
                    "changeval VARCHAR(64), " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            statement.executeUpdate(createPlayerStatsHistoryTable);
            System.out.println("PlayerStatsHistory table initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

}
