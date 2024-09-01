import io.socket.client.Ack;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class hj {
    Socket socket;
    Random r = new Random();
    static int randomValue;

    public hj(Socket socket) {
        this.socket = socket;
    }

    public void emitEvent(String eventName, Object ... args) {

        if (args != null && args.length > 0 && args[0] != null) {
        args[0] = this.transformObject(args[0], 0);
        }

        this.socket.emit(eventName, args);
    }

    public Object transformObject(Object obj, int value) {
        if (obj.getClass().equals(JSONObject.class)) {
            JSONObject jsonObject = copyJSONObject((JSONObject)obj);
            if (value == 0) {
                value = r.nextInt(30) + 120;
            }
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                keys.add(key);
            }
            for (String key : keys) {
                if (jsonObject.get(key) instanceof JSONObject) {
                    jsonObject.put(transformString(key, value), this.transformObject(jsonObject.get(key), value));
                } else {
                    jsonObject.put(transformString(key, value), transformString(jsonObject.get(key), value));
                }
                jsonObject.remove(key);
            }
            jsonObject.put("lfm5", value);
            return jsonObject;
        }
        return obj;
    }

    private String transformString(Object obj, int value) {
        if (obj instanceof String || obj instanceof Number) {
            boolean isNumber = false;
            if (obj instanceof Number) {
                obj = obj.toString();
                isNumber = true;
            }
            char[] charArray = ((String)obj).toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                charArray[i] = (char)(charArray[i] + value);
            }
            if (isNumber) {
                return new String(charArray) + " ";
            }
            return new String(charArray);
        }
        if (obj instanceof JSONArray) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < ((JSONArray)obj).length(); ++i) {
                jsonArray.put(this.transformString(((JSONArray)obj).get(i), value));
            }
            if (jsonArray.length() >= 5) {
                Object firstElement = jsonArray.get(0);
                Object secondElement = jsonArray.get(1);
                jsonArray.put(0, jsonArray.get(jsonArray.length() - 1));
                jsonArray.put(jsonArray.length() - 1, firstElement);
                jsonArray.put(1, jsonArray.get(3));
                jsonArray.put(3, secondElement);
            }
            return jsonArray.toString();
        }
        return obj.toString();
    }

    public static JSONObject decodeOBJ(JSONObject obj, int value) {
        JSONArray ja = new JSONArray();
        ja.put(obj);
        obj = decode(ja, value);
        return obj;
    }

    public static JSONObject decode(Object obj, int value){
        JSONArray o = (JSONArray) transformJSONArray(obj, value);
        JSONObject jo = o.getJSONObject(0);
        return jo;
    }
    public static Object transformJSONArray(Object obj, int value) {
        try {
            if (obj != null) {
                if (obj instanceof JSONArray) {
                    for (int i = 0; i < ((JSONArray)obj).length(); ++i) {
                        if (((JSONArray)obj).get(i).getClass().equals(JSONObject.class) || ((JSONArray)obj).get(i).getClass().equals(JSONArray.class)) {
                            transformJSONArray(((JSONArray)obj).get(i), value);
                            continue;
                        }
                        ((JSONArray)obj).put(i, decodeObject(((JSONArray)obj).get(i), value));
                    }
                    if (((JSONArray)obj).length() >= 5) {
                        Object firstElement = ((JSONArray)obj).get(0);
                        Object secondElement = ((JSONArray)obj).get(1);
                        ((JSONArray)obj).put(0, ((JSONArray)obj).get(((JSONArray)obj).length() - 1));
                        ((JSONArray)obj).put(((JSONArray)obj).length() - 1, firstElement);
                        ((JSONArray)obj).put(1, ((JSONArray)obj).get(3));
                        ((JSONArray)obj).put(3, secondElement);
                    }
                    return obj;
                }
                JSONObject jsonObject = (JSONObject)obj;
                if (value == 0) {
                    value = ((JSONObject)obj).has("lfm1") ? randomValue : ((JSONObject)obj).getInt("lfm5");
                }
                ArrayList<String> keys = new ArrayList<String>();
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    keys.add(key);
                }
                for (String key : keys) {
                    if (jsonObject.get(key) instanceof JSONObject || jsonObject.get(key) instanceof JSONArray && jsonObject.getJSONArray(key).length() >= 1 && jsonObject.getJSONArray(key).get(0) instanceof JSONObject || jsonObject.get(key) instanceof JSONArray && jsonObject.getJSONArray(key).length() >= 1 && jsonObject.getJSONArray(key).get(0) instanceof JSONArray) {
                        jsonObject.put(decodeString(key, value), transformJSONArray(jsonObject.get(key), value));
                    } else if (!(key.equals("lfm1") || key.equals("lfm2") || key.equals("lfm3") || key.equals("lfm4") || key.equals("lfm5"))) {
                        String transformedKey = decodeString(key, value);
                        if (transformedKey.equals("logo")) {
                            jsonObject.put(transformedKey, jsonObject.get(key));
                        } else {
                            jsonObject.put(transformedKey, decodeObject(jsonObject.get(key), value));
                        }
                    }
                    jsonObject.remove(key);
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException | ClassCastException e) {
            return obj;
        }
        return obj;
    }
    private static String decodeString(Object obj, int value) {
    if (obj instanceof String || obj instanceof Number) {
        boolean isNumber = false;
        if (obj instanceof Number) {
            obj = obj.toString();
            isNumber = true;
        }
        char[] charArray = ((String)obj).toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = (char)(charArray[i] - value);
        }
        if (isNumber) {
            return new String(charArray) + " ";
        }
        return new String(charArray);
    }
    return obj.toString();
    }

    public static Object decodeObject(Object obj, int value) {
        if (obj instanceof JSONArray) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < ((JSONArray)obj).length(); ++i) {
                jsonArray.put(decodeObject(((JSONArray)obj).get(i), value));
            }
            if (jsonArray.length() >= 5) {
                Object firstElement = jsonArray.get(0);
                Object secondElement = jsonArray.get(1);
                jsonArray.put(0, jsonArray.get(jsonArray.length() - 1));
                jsonArray.put(jsonArray.length() - 1, firstElement);
                jsonArray.put(1, jsonArray.get(3));
                jsonArray.put(3, secondElement);
            }
            return jsonArray;
        }
        if (obj instanceof Boolean) {
            return obj;
        }
        if (!(obj instanceof String)) {
            obj = obj.toString();
        }

        char[] cArray = ((String)obj).toCharArray();
        int n3 = 0;
        while (n3 < cArray.length) {
            if (n3 == cArray.length - 1 && cArray[n3] == ' ') {
                obj = Integer.valueOf(new String(cArray).substring(0, n3));
                return obj;
            }
            int n4 = n3++;
            cArray[n4] = (char)(cArray[n4] - value);
        }
        return new String(cArray);
    }

    public static void setRandomValue(int value) {
        randomValue = value;
    }

    public static void setRange(int minValue, int maxValue) {
        randomValue = maxValue - minValue + 150;
    }

    public Socket openSocket() {
        return this.socket.open();
    }

    public Socket connectSocket() {
        return this.socket.connect();
    }

    public Socket sendData(Object ... args) {
        return this.socket.send(args);
    }

    public Emitter emitEventWithAck(String eventName, Object[] args, Ack ack) {
        return this.socket.emit(eventName, args, ack);
    }

    public Socket closeSocket() {
        return this.socket.close();
    }

    public Socket disconnectSocket() {
        return this.socket.disconnect();
    }

    public Manager getManager() {
        return this.socket.io();
    }

    public boolean isConnected() {
        return this.socket.connected();
    }

    public String getSocketId() {
        return this.socket.id();
    }

    public hj onEvent(String eventName, Emitter.Listener listener) {
        this.socket.on(eventName, new td(listener));
        return this;
    }

    public Emitter onceEvent(String eventName, Emitter.Listener listener) {
        return this.socket.once(eventName, listener);
    }

    public Emitter removeAllListeners() {
        return this.socket.off();
    }

    public Emitter removeEventListener(String eventName) {
        return this.socket.off(eventName);
    }

    public Emitter removeSpecificListener(String eventName, Emitter.Listener listener) {
        return this.socket.off(eventName, listener);
    }

    public List<Emitter.Listener> getEventListeners(String eventName) {
        return this.socket.listeners(eventName);
    }

    public boolean hasEventListeners(String eventName) {
        return this.socket.hasListeners(eventName);
    }

    public static JSONObject copyJSONObject(JSONObject jSONObject) {
        JSONObject jSONObject2 = new JSONObject();
        for (int i2 = 0; i2 < jSONObject.names().length(); ++i2) {
            jSONObject2.put(jSONObject.names().getString(i2), jSONObject.get(jSONObject.names().getString(i2)));
        }
        return jSONObject2;
    }

    public static JSONArray copyJSONArray(JSONArray jSONArray) {
        JSONArray jSONArray2 = new JSONArray();
        for (int i2 = 0; i2 < jSONArray.length(); ++i2) {
            jSONArray2.put(jSONArray.get(i2));
        }
        return jSONArray2;
    }
}

