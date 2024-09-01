public class Player {
    private int id;
    private String login;
    private int mmr;
    private int win;
    private int lose;
    private String clan;
    private String color;

    // Constructor
    public Player(int id, String login, int mmr, int win, int lose, String clan, String color) {
        this.id = id;
        this.login = login;
        this.mmr = mmr;
        this.win = win;
        this.lose = lose;
        this.clan = clan;
        this.color = color;
    }

    // Getters and Setters
    public int getId() {return id;}
    public void setId(int id) {this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public int getMmr() { return mmr; }
    public void setMmr(int mmr) { this.mmr = mmr; }

    public int getWin() { return win; }
    public void setWin(int win) { this.win = win; }

    public int getLose() { return lose; }
    public void setLose(int lose) { this.lose = lose; }

    public String getClan() { return clan; }
    public void setClan(String clan) { this.clan = clan; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
