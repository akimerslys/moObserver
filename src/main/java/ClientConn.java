import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;
import java.time.Duration;
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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private Timestamp lastTopUpdate;
    private Timestamp lastClanUpdate;
    private ScheduledFuture<?> reconntask;
    private int reconTimes;

    public ClientConn(String login, String pass, Connection conn) {
        this.login = login;
        this.pass = pass;
        this.conn = conn;
        opensocket(false);
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
                    logger.severe(ex.toString());
                    sendAdmin(ex.toString());
                    return null;
                });
    }

    public void opensocket(boolean rcon) {
        try {
            IO.Options options = new IO.Options();
            //options.proxy = proxy;
            options.reconnection = false;
            options.timeout = 30000L;
            options.reconnectionAttempts = 100;
            options.reconnectionDelay = 10000;
            if (rcon) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.severe(e.toString());
                }
            }
            this.a = new hj(IO.socket(URI.create("http://mafiaonline.jcloud.kz"), options));
            setupListeners();
        } catch (Exception e) {
            handleError(e);
            System.out.println("Failed to create socket: " + e.getMessage());
        }
    }

    private void closesocket() {
        this.reconntask.cancel(true);
        this.a.closeSocket();
        this.a = null;
    }

    private void connError(Object err) {

        this.logged = false;
        this.reconTimes++;
        if (err.toString().contains("transport") || err.toString().contains("xhr") || this.reconTimes == 15) {
            closesocket();
            opensocket(true);
            connectAndLogin();
            return;
        }
        scheduleReconnect();
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
                    future.complete(null);
                } else {
                    connError(args[0]);
                }
            }).on(Socket.EVENT_CONNECT_ERROR, args -> {
                System.out.println("Connection error: " + args[0]);
                connError(args[0]);

            }).on(Socket.EVENT_CONNECT_TIMEOUT, args -> {
                System.out.println("Connection timeout: " + args[0]);
                connError(args[0]);

            }).on(Socket.EVENT_DISCONNECT, args -> {
                System.out.println("Disconnected from server: " + args[0]);
                connError(args[0]);
            });

            this.a.socket.connect();
            System.out.println("Attempting to connect...");
        } catch (Exception exception) {
            handleError(exception);
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

    private void startScheduling() {
        scheduler.scheduleAtFixedRate(this::requestTop, 0, 1, TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(this::requestTopClan, 120, 86400, TimeUnit.SECONDS);
    }


    private void messageProcessing() {
        logger.info("started message processing");
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    Message msg = queue.take();
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
        this.a.socket.on("getClansTopResult", this::handleResultClanTop);
    }

    private void handleResultLogin(Object... args) {
        JSONObject jsonObject;
        if (args[0] == null) {
            this.a.closeSocket();
            System.out.println("BadLogin");
            return;
        }
        try {
            System.out.print(args[0] + "\n\n" + args[0].getClass());
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
                    startScheduling();
                }
            } else {
                logger.log(Level.SEVERE, "Received data is not a JSONArray.");
            }
        } catch (JSONException e) {
            logger.log(Level.SEVERE,"JSON Exception: " + e.getMessage());
            handleError(e);
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

        boolean r = queue.offer(m);

        if (!r) {
            logger.severe("Message didnt got to the queue");
        }
    }

    private void handleResultTop(Object... args) {
        logger.info(args[0].toString());
        lastTopUpdate = new Timestamp(System.currentTimeMillis());
        CompletableFuture.runAsync(() -> processRankUpdates((JSONArray) args[0]), executor)
                .exceptionally(ex -> {
                    logger.severe(ex.toString());
                    return null;
                });
    }

    private void handleResultClanTop(Object... args) {
        logger.info("Handled topClans");
        lastClanUpdate = new Timestamp(System.currentTimeMillis());
        CompletableFuture.runAsync(() -> processClanUpdates((JSONArray) args[0]), executor)
                .exceptionally(ex -> {
                    logger.severe(ex.toString());
                    return null;
                });
    }


    private Timestamp checkTime(Timestamp lastupd, int hrs) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (lastupd == null || Duration.between(lastupd.toInstant(), now.toInstant()).toHours() >= hrs && this.logged) {
            return now;
        }
        return null;
    }

    private void requestTop() {
        Timestamp now = checkTime(lastTopUpdate, 1);
        if (now != null) {
            this.a.socket.emit("Top");
        }
    }

    private void requestTopClan() {
        Timestamp now = checkTime(lastClanUpdate, 24);
        if (now != null) {
            logger.info("Updating Top Clans");
            this.a.socket.emit("getClansTop");
        }
    }

    private void processRankUpdates(JSONArray playerUpdates) {

        long startTime = System.nanoTime();
        int l = playerUpdates.length();
        logger.info("Total Players to update: " + l);

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
            logger.info("Updated players ~" + j * 50 + "/" + l);
        }

        logger.info(String.format(
                "%d PLAYERS updated in %.6f s",
                l,
                (System.nanoTime() - startTime) / 1_000_000_000.0));

    }

    private void processClanUpdates(JSONArray clanUpdates) {

        long startTime = System.nanoTime();

        int l = clanUpdates.length();
        logger.info("Total clans to update: " + l);

        int CHUNK_SIZE = 100;

        List<JSONArray> chunks = chunkArray(clanUpdates, CHUNK_SIZE);

        int j = 1;
        for (JSONArray chunk: chunks) {
            for (int i = 0; i < chunk.length(); i++) {
                JSONObject clan_ = chunk.getJSONObject(i);
                int honor = clan_.getInt("Honor");
                if (honor !=0) {
                    Clan clan = new Clan(clan_.getString("Name"),
                            clan_.getInt("Lvl"),
                            clan_.getInt("Honor"),
                            clan_.getInt("NumberPlayers"),
                            clan_.getInt("MaxPlayers"),
                            clan_.getString("Leader"),
                            clan_.getString("Deputy"),
                            clan_.getString("Territory"),
                            clan_.getInt("HonorEarnedInTerritory"),
                            clan_.getString("Items"), -1);

                    updateOrInsertClan(clan);
                }
            }
            logger.info("Updated clans ~" + j * CHUNK_SIZE + "/" + l);
            j++;
        }
        logger.info(String.format(
                "%d CLANS updated in %.6f s",
                l,
                (System.nanoTime() - startTime) / 1_000_000_000.0));
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

    private Clan getClan(String clanname) {
        String sql = "SELECT * FROM Clans WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, clanname);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Clan(
                            rs.getString("name"),
                            rs.getInt("lvl"),
                            rs.getInt("honor"),
                            rs.getInt("numberPlayers"),
                            rs.getInt("maxPlayers"),
                            rs.getString("leader"),
                            rs.getString("deputy"),
                            rs.getString("territory"),
                            rs.getInt("honorTer"),
                            rs.getString("items"),
                            rs.getInt("id")
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

    private void updateOrInsertClan(Clan clan) {
        String sql = "UPDATE Clans SET honor = ?, numberPlayers = ?, maxPlayers = ?, leader = ?, deputy = ?, lvl = ?, territory = ?, honorTer = ?, items = ? WHERE name = ?";
        boolean toUpdate = false;

        Clan c = getClan(clan.getName());
        if (c != null) {
            int id = c.getId();
            toUpdate |= checkChange(id, 0, clan.getHonor(), c.getHonor(), true);
            toUpdate |= checkChange(id, 1, clan.getNumPlayers(), c.getNumPlayers(), true);
            toUpdate |= checkChange(id, 2, clan.getMaxPlayers(), c.getMaxPlayers(), true);
            toUpdate |= checkChange(id, 3, clan.getLeader(), c.getLeader(), true);
            toUpdate |= checkChange(id, 4, clan.getDeputy(), c.getDeputy(), true);
            toUpdate |= checkChange(id, 5, clan.getLvl(), c.getLvl(), true);
            toUpdate |= checkChange(id, 6, clan.getTerritory(), c.getTerritory(), true);
            toUpdate |= checkChange(id, 7, clan.getHonorTer(), c.getHonorTer(), true);
            toUpdate |= checkChange(id, 8, clan.getItems(), c.getItems(), true);

        } else {
            sql = "INSERT INTO Clans (name, honor, numberPlayers, maxPlayers, leader, deputy, lvl, territory, honorTer, items) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        if (toUpdate || c == null) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int nameI = toUpdate ? 10 : 1;
                int honorI = toUpdate ? 1 : 2;

                stmt.setString(nameI, clan.getName());
                stmt.setInt(honorI, clan.getHonor());
                stmt.setInt(honorI + 1, clan.getNumPlayers());
                stmt.setInt(honorI + 2, clan.getMaxPlayers());
                stmt.setString(honorI + 3, clan.getLeader());
                stmt.setString(honorI + 4, clan.getDeputy());
                stmt.setInt(honorI + 5, clan.getLvl());
                stmt.setString(honorI + 6, clan.getTerritory());
                stmt.setInt(honorI + 7, clan.getHonorTer());
                stmt.setString(honorI + 8, clan.getItems());
                stmt.executeUpdate();
            } catch (SQLException e) {
                handleError(e);
            }
        }
    }

    private void updateOrInsertPlayer(String user_login, int mmr, int win, int lose, String clan, String color, boolean full) {
        String sql = "UPDATE PlayerStats SET mmr = ?, win = ?, lose = ?, clan = ?, color = ?, last_updated = ? WHERE login = ?";
        boolean toUpdate = false;

        Player p = getPlayer(user_login);
        if (p != null) {
            if (full) {
                toUpdate |= checkChange(p.getId(), 0, mmr, p.getMmr(), false);
                toUpdate |= checkChange(p.getId(), 1, win, p.getWin(), false);
                toUpdate |= checkChange(p.getId(), 2, lose, p.getLose(), false);
                toUpdate |= checkChange(p.getId(), 3, clan, p.getClan(), false);
                toUpdate |= checkChange(p.getId(), 4, color, p.getColor(), false);
            } else {
                toUpdate |= checkChange(p.getId(), 0, mmr, p.getMmr(), false);
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


    private void commitChange(int id, int changeType, String newValue, boolean clan) {
        String sql = "INSERT INTO PlayerStatsHistory (player_id, type, val) " +
                "VALUES (?, ?, ?)";

        if (clan) {
            sql = "INSERT INTO ClanHistory (clan_id, type, val) " +
                "VALUES (?, ?, ?)";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, changeType);
            stmt.setString(3, newValue);
            stmt.executeUpdate();
            logger.info("Changed " + (clan ? "CLAN" : "PLAYER") + " : #" + id + "  [" + changeType + " : " + newValue + "]");
        } catch (SQLException e) {
            handleError(e);
        }
    }

    private boolean checkChange(int playerId, int changeType, Object newValue, Object currentValue, boolean clan) {

        if (!clan && (changeType == 1 || changeType == 2)) {
            if (newValue instanceof Integer && currentValue instanceof Integer) {
                if ((int) newValue <= (int) currentValue) {
                    return false;
                }
            }
        }

        if (!Objects.equals(currentValue, newValue)) {
            commitChange(playerId, changeType, String.valueOf(newValue), clan);
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

