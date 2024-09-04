public class Message {
    private final String author;
    private final int mmr;
    private final int countWin;
    private final int countLoose;
    private final String message;
    private final String clan;
    private final String colorNick;

    public Message(String author, int mmr, int countWin, int countLoose, String message, String clan, String colorNick) {
        this.author = author;
        this.mmr = mmr;
        this.countWin = countWin;
        this.countLoose = countLoose;
        this.message = message;
        this.clan = clan;
        this.colorNick = colorNick;
    }

    public String getAuthor() { return author; }
    public int getMmr() { return mmr; }
    public int getCountWin() { return countWin; }
    public int getCountLoose() { return countLoose; }
    public int getWin() { return countWin; }
    public int getLose() { return countLoose; }
    public String getMessage() { return message; }
    public String getClan() { return clan; }
    public String getColorNick() { return colorNick; }
}
