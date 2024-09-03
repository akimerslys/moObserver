import io.github.cdimascio.dotenv.Dotenv;

public class Secrets {
    private static final Dotenv dotenv = Dotenv.load();

    public static String dbUser;
    public static String dbPass;
    public static String dbHost;
    public static String dbPort;
    public static String dbName;
    public static String login;
    public static String pass;
    public static String adminId;
    public static String botToken;

    static {
        try {
            dbUser = dotenv.get("DB_USER");
            dbPass = dotenv.get("DB_PASS");
            dbHost = dotenv.get("DB_HOST", "localhost");
            dbPort = dotenv.get("DB_PORT", "5432");
            dbName = dotenv.get("DB_NAME");
            login = dotenv.get("OBS_LOGIN");
            pass = dotenv.get("OBS_PASS");
            adminId = dotenv.get("ADMIN_ID");
            botToken = dotenv.get("BOT_TOKEN");

            // Optional: Print loaded values for debugging
            System.out.println("DB_USER: " + dbUser);
            System.out.println("DB_HOST: " + dbHost);
            System.out.println("DB_PORT: " + dbPort);
            System.out.println("DB_NAME: " + dbName);
            System.out.println("OBS_LOGIN: " + login);
            System.out.println("ADMIN_ID: " + adminId);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle missing .env or errors loading environment variables
        }
    }

    public static String get_db_url() {
        return "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
    }
}
