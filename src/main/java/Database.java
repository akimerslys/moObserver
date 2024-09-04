import java.sql.*;

public class Database {

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(Secrets.get_db_url(), Secrets.dbUser, Secrets.dbPass);
    }

    public static boolean testConnection(Connection conn) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT 1");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
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

            String createClanTable = "CREATE TABLE IF NOT EXISTS Clans (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "honor INT, " +
                    "numberPlayers SMALLINT, " +
                    "maxPlayers SMALLINT, " +
                    "leader VARCHAR(10), " +
                    "deputy VARCHAR(10), " +
                    "lvl SMALLINT, " +
                    "territory VARCHAR(30), " +
                    "honorTer INT, " +
                    "items VARCHAR(30), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";

            statement.executeUpdate(createClanTable);
            System.out.println("Clan table successfully initialized");

            String createClanHistoryTable = "CREATE TABLE IF NOT EXISTS ClanHistory (" +
                    "id SERIAL PRIMARY KEY, " +
                    "clan_id INT NOT NULL REFERENCES clans(id) ON DELETE CASCADE, " +
                    "type SMALLINT, " +
                    "val VARCHAR(64), " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            statement.executeUpdate(createClanHistoryTable);
            System.out.println("ClanHistory table initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

}
