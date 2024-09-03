import org.json.JSONException;
import org.json.JSONObject;

public class MoTools {

    public static JSONObject getLogin(String login, String pass, String sid) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("login", login);
            jSONObject.put("password", pass);
            jSONObject.put("isUsePassword", true);
            jSONObject.put("version", "184.0");
            jSONObject.put("color", false);
            jSONObject.put("i", sid + " ! null ! null ! http://www.google.com ! unknown ! Java/18.0.1.1 ! unknown");
            jSONObject.put("steamId", "");
            jSONObject.put("o", "2068136186");
            jSONObject.put("p1", ""); // ipv4
            jSONObject.put("p2", ""); // ipv6
            jSONObject.put("m1", "");
            jSONObject.put("m2", "");
            jSONObject.put("me", false);
        } catch (JSONException jSONException) {
            jSONException.printStackTrace();
        }
        return jSONObject;
    }

    public static int getRank(int mmr) {
        int lvl;
        if (mmr < 180) {
            int n4 = 30;
            lvl = mmr / n4;
        } else if (mmr < 380) {
            int n6 = 40;
            lvl = 6 + (mmr - 180) / n6;
        } else if (mmr < 630) {
            int n8 = 50;
            lvl = 11 + (mmr - 380) / n8;
        } else if (mmr < 1230) {
            int n10 = 60;
            lvl = 16 + (mmr - 630) / n10;
        } else if (mmr < 1730) {
            int n12 = 100;
            lvl = 26 + (mmr - 1230) / n12;
        } else if (mmr < 5230) {
            int n14 = 700;
            lvl = 31 + (mmr - 1730) / n14;
        } else if (mmr < 10230) {
            int n16 = 1000;
            lvl = 36 + (mmr - 5230) / n16;
        } else if (mmr < 12730) {
            int n18 = 500;
            lvl = 41 + (mmr - 10230) / n18;
        } else {
            lvl = 45;
        }
        return lvl;
    }
}
