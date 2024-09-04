public class Clan {
    private final String name;
    private final int lvl;
    private final int honor;
    private final int numPlayers;
    private final int maxPlayers;
    private final String leader;
    private final String deputy;
    private final String territory;
    private final int honorTer;
    private final String items;
    private final int id;

    // Constructor
    public Clan(String name, int lvl, int honor, int numPlayers, int maxPlayers,
                String leader, String deputy, String territory, int honorTer, String items, int id) {
        this.id = id;
        this.name = name;
        this.honor = honor;
        this.lvl = lvl;
        this.numPlayers = numPlayers;
        this.maxPlayers = maxPlayers;
        this.leader = leader;
        this.deputy = deputy;
        this.territory = territory;
        this.honorTer = honorTer;
        this.items = items;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getLvl() {
        return lvl;
    }

    public int getHonor() {
        return honor;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getLeader() {
        return leader;
    }

    public String getDeputy() {
        return deputy;
    }

    public String getTerritory() {
        return territory;
    }

    public int getHonorTer() {
        return honorTer;
    }

    public String getItems() {
        return items;
    }
}
