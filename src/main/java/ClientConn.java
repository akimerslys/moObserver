import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.util.List;

public class ClientConn {
    private static final Logger logger = Logger.getLogger(ClientConn.class.getName());
    public hj a;
    public boolean logged = false;
    public String sid;
    public String login;
    public String pass;
    private Connection conn;
    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private Timestamp lastTopUpdate;
    private ScheduledFuture<?> reconntask;
    private int reconTimes;

    public ClientConn(String login, String pass, Connection conn) {
        this.login = login;
        this.pass = pass;
        this.conn = conn;
        opensocket();
        messageProcessing();
        connectAndLogin();
    }

    private synchronized void connectAndLogin() {
        if (this.a.isConnected() || this.a.getSocketId() != null) {
            System.out.println("Already connecting or connected, skipping new connection attempt.");
            return;
        }

        connect()
                .thenRun(() -> {
                })
                .exceptionally(ex -> {
                    System.out.println("Error during connection or login: " + ex.getMessage());
                    ex.printStackTrace();
                    sendAdmin(ex.toString());
                    return null;
                });
    }

    public void opensocket() {
        try {
            IO.Options options = new IO.Options();
            //options.proxy = proxy;
            options.reconnection = false;
            options.timeout = 30000L;
            options.reconnectionAttempts = 100;
            options.reconnectionDelay = 10000;
            this.a = new hj(IO.socket(URI.create("http://mafiaonline.jcloud.kz"), options));
            setupListeners();
        } catch (Exception e) {
            e.printStackTrace();
            handleError(e);
            System.out.println("Failed to create socket: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            this.a.socket.on(Socket.EVENT_CONNECT, args -> {
                if (a.getSocketId() != null) {
                    System.out.println("Connected to server. Sid " + a.getSocketId());
                    sid = a.getSocketId();
                    reconTimes = 0;
                    login_user();
                    waitUntilLogged();
                    future.complete(null);
                } else {
                    scheduleReconnect();
                }
            }).on(Socket.EVENT_CONNECT_ERROR, args -> {
                System.out.println("Connection error: " + args[0]);
                if (Objects.equals(args[0].toString(), "transport error")) {
                    this.a.closeSocket();
                    opensocket();
                    connectAndLogin();
                    return;
                }
                scheduleReconnect();
            }).on(Socket.EVENT_CONNECT_TIMEOUT, args -> {
                System.out.println("Connection timeout: " + args[0]);
                scheduleReconnect();
            }).on(Socket.EVENT_DISCONNECT, args -> {
                System.out.println("Disconnected from server: " + args[0]);
                if (Objects.equals(args[0].toString(), "transport error")) {
                    this.a.closeSocket();
                    opensocket();
                    connectAndLogin();
                    return;
                }
                scheduleReconnect();
            });

            this.a.socket.connect();
            System.out.println("Attempting to connect...");
        } catch (Exception exception) {
            System.out.println(exception);
            future.completeExceptionally(exception);
        }
        return future;
    }

    private void reconnect() {
        if (!this.a.isConnected() || this.a.getSocketId() == null) {
            this.reconTimes++;
            if (this.reconTimes > 10) {
                sendAdmin("To many connection retries #" + this.reconTimes);
            }
            this.a.disconnectSocket();
            this.a.connectSocket();
        }
    }

    private void scheduleReconnect() {
        if (this.reconntask  == null || this.reconntask.isDone()) {
            System.out.println("Scheduling reconnect in 10 seconds...");
            this.reconntask = scheduler.schedule(() -> {
                System.out.println("Reconnecting...");
                reconnect();
            }, 10, TimeUnit.SECONDS);
        }
    }


    public void login_user() {
        JSONObject jo = MoTools.getLogin(login, pass, sid);

        //loginFuture = new CompletableFuture<>();
        this.a.emitEvent("Login", jo);
        //return loginFuture;
    }

    private CompletableFuture<Void> waitUntilLogged() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // repeatable logged checker
        scheduler.scheduleAtFixedRate(() -> {
            if (this.logged) {
                scheduler.scheduleAtFixedRate(this::requestTop, 0, 1, TimeUnit.HOURS);
                future.complete(null);
            }
        }, 0, 10, TimeUnit.SECONDS);

        // cancel task
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new RuntimeException("Timed out waiting for login"));
            }
        }, 50, TimeUnit.SECONDS);

        return future;
    }

    private void messageProcessing() {
        logger.info("started message processing");
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    int i = 0;
                    Message msg = queue.take();
                    logger.log(Level.INFO, i + msg.getAuthor() + msg.getCountWin());
                    saveMsg(msg);
                } catch (Exception e) { //Interrupted
                    handleError(e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }


    private void setupListeners() {
        this.a.socket.on("LoginNotUniq", this::handleLoginNotUnique);
        this.a.socket.on("ResultLogin", this::handleResultLogin);
        this.a.socket.on("NewMessageRegionChat", this::handleMessage);
        this.a.socket.on("ResultTop", this::handleResultTop);
    }

    private void handleResultLogin(Object... args) {
        JSONObject jsonObject;
        if (args[0] == null) {
            this.a.closeSocket();
            System.out.println("BadLogin");
            return;
        }
        try {
            System.out.print(args[0].toString() + "\n\n" + args[0].getClass());
            if (args[0] instanceof JSONArray jsonArray) {
                if (jsonArray.length() > 0) {
                    jsonObject = jsonArray.getJSONObject(0);

                    if (jsonObject.has("Status")) {
                        String msg = jsonObject.getString("message");
                        logger.log(Level.SEVERE, "BadLogin: " + msg);
                        return;
                    }

                    logger.info("Login: " + jsonObject.getString("login"));
                    logger.info("Password: " + jsonObject.getString("password"));
                    logger.info("Money: " + jsonObject.getInt("money"));

                    this.logged = true;
                    this.a.socket.emit("EnterRegionChat");
                }
            } else {
                logger.log(Level.SEVERE, "Received data is not a JSONArray.");
            }
        } catch (JSONException e) {
            logger.log(Level.SEVERE,"JSON Exception: " + e.getMessage());
            e.printStackTrace();
            this.logged = false;
        }
    }

    private void handleLoginNotUnique(Object... objects) {
        logger.warning("Login not unique");
        this.a.closeSocket();
    }

    private void handleMessage(Object... args) {
        Message m;
        try {
            JSONObject msg = (JSONObject) args[0];
            String color = msg.getString("colorNick");
            m = new Message(
                    msg.getString("author"),
                    msg.getInt("MMR"),
                    msg.getInt("countWin"),
                    msg.getInt("countLoose"),
                    msg.getString("message"),
                    msg.getString("clan"),
                    color.substring(color.indexOf("#") + 1, color.indexOf("]")));
        } catch (org.json.JSONException e) {
            return;
        }
        String logmsg = "%s [%d|%d:%d] : %s";
        logger.info(String.format(logmsg, m.getAuthor(), m.getMmr(), m.getCountWin(), m.getCountLoose(), m.getMessage()));

        queue.offer(m);
    }

    private void handleResultTop(Object... args) {
        logger.info(args[0].toString());
        lastTopUpdate = new Timestamp(System.currentTimeMillis());
        CompletableFuture.runAsync(() -> processRankUpdates((JSONArray) args[0]), executor)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
    private void emitTop() {
            this.a.socket.emit("Top");
        }
    private void requestTop() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (lastTopUpdate == null || Duration.between(lastTopUpdate.toInstant(), now.toInstant()).toHours() >= 1 && this.logged) {
            emitTop();
        }
    }

    public void processRankUpdates(JSONArray playerUpdates) {

        List<JSONArray> chunks = chunkArray(playerUpdates, 50);

        System.out.println(playerUpdates.length());
        int j = 1;
        for (JSONArray chunk : chunks) {
            j++;
            for (int i = 0; i < chunk.length(); i++) {
                JSONObject playerUpdate = chunk.getJSONObject(i);
                String user_login = playerUpdate.getString("login");
                int newMmr = playerUpdate.getInt("mmr");
                updateOrInsertPlayer(user_login, newMmr, 0, 0, "?", "?", false);
            }
            logger.info("Updated ~" + j * 50 + " queries");
        }
    }

    private List<JSONArray> chunkArray(JSONArray array, int chunkSize) {
        List<JSONArray> chunks = new ArrayList<>();
        for (int i = 0; i < array.length(); i += chunkSize) {
            int end = Math.min(array.length(), i + chunkSize);
            JSONArray chunk = new JSONArray();
            for (int j = i; j < end; j++) {
                chunk.put(array.get(j));
            }
            chunks.add(chunk);
        }
        return chunks;
    }

    private Player getPlayer(String user_login) {
        String sql = "SELECT * FROM PlayerStats WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user_login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Create and return a Player object with data from the ResultSet
                    return new Player(
                            rs.getInt("id"),
                            rs.getString("login"),
                            rs.getInt("mmr"),
                            rs.getInt("win"),
                            rs.getInt("lose"),
                            rs.getString("clan"),
                            rs.getString("color")
                    );
                }
            }
        } catch (SQLException e) {
            handleError(e);
            testDb();
        }
        return null;
    }

    private void saveMsgToDb(Message msg) {
        String sql = "INSERT INTO messages (author, message, created_at) VALUES (?, ?, ?)";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, msg.getAuthor());
            stmt.setString(2, msg.getMessage());
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private void updateOrInsertPlayer(String user_login, int mmr, int win, int lose, String clan, String color, boolean full) {
        String sql = "UPDATE PlayerStats SET mmr = ?, win = ?, lose = ?, clan = ?, color = ?, last_updated = ? WHERE login = ?";
        boolean toUpdate = false;

        Player p = getPlayer(user_login);
        if (p != null) {
            if (full) {
                toUpdate |= checkAndLogChange(p.getId(), "win", win, p.getWin());
                toUpdate |= checkAndLogChange(p.getId(), "lose", lose, p.getLose());
                toUpdate |= checkAndLogChange(p.getId(), "clan", clan, p.getClan());
                toUpdate |= checkAndLogChange(p.getId(), "color", color, p.getColor());
            } else {
                toUpdate |= checkAndLogChange(p.getId(), "mmr", mmr, p.getMmr());
                win = p.getWin();
                lose = p.getLose();
                clan = p.getClan();
                color = p.getColor();
            }

        } else {
            sql = "INSERT INTO PlayerStats (login, mmr, win, lose, clan, color, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?)";

        }
        if (toUpdate || p == null) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int login_index = toUpdate ? 7 : 1;
                int mmr_index = toUpdate ? 1 : 2;

                stmt.setString(login_index, user_login);
                stmt.setInt(mmr_index, mmr);
                stmt.setInt(mmr_index + 1, win);
                stmt.setInt(mmr_index + 2, lose);
                stmt.setString(mmr_index + 3, clan);
                stmt.setString(mmr_index + 4, color);
                stmt.setTimestamp(mmr_index + 5, new Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                handleError(e);
            }
        }

    }


    private void commitChange(int playerId, String changeType, String newValue) {
        String insertHistorySql = "INSERT INTO PlayerStatsHistory (player_id, changetype, changeval, updated_at) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertHistorySql)) {
            stmt.setInt(1, playerId);
            stmt.setString(2, changeType);
            stmt.setString(3, newValue);
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
            logger.info("Changed for #" + playerId + " [" + changeType + " : " + newValue + "]");
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private boolean checkAndLogChange(int playerId, String changeType, Object newValue, Object currentValue) {
        if (!Objects.equals(currentValue, newValue)) {
            commitChange(playerId, changeType, String.valueOf(newValue));
            return true;
        }
        return false;
    }

    private void saveMsg(Message msg) {

        saveMsgToDb(msg);

        updateOrInsertPlayer(msg.getAuthor(), msg.getMmr(), msg.getCountWin(), msg.getCountLoose(), msg.getClan(), msg.getColorNick(), true);

    }

    private void handleError(Exception e) {
        logger.log(Level.SEVERE, e.toString());
        sendAdmin(e.toString());
    }

    private void sendAdmin(String msg) {
        Telegram.sendMsg(Secrets.adminId, msg);
    }

    private void testDb(){
        if (!Database.testConnection(conn)) {
            try {
                this.conn = Database.getConnection();
            } catch (SQLException e) {
                handleError(e);
            }
        }
    }

}

