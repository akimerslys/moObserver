import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;

class td
implements Emitter.Listener {
    Emitter.Listener a;

    public td(Emitter.Listener listener) {
        this.a = listener;
    }

    @Override
    public void call(Object ... objectArray) {
        System.out.println(objectArray);
        if (objectArray.length >= 1 && (objectArray != null && objectArray.length >= 1 && objectArray[0] instanceof JSONArray && ((JSONArray)objectArray[0]).length() >= 1 && ((JSONArray)objectArray[0]).get(0).getClass().equals(JSONObject.class) && !((JSONArray)objectArray[0]).getJSONObject(0).isNull("lfm5") || objectArray != null && objectArray.length >= 1 && objectArray[0] instanceof JSONObject && !((JSONObject)objectArray[0]).isNull("lfm5"))) {
            objectArray[0] = hj.transformJSONArray(objectArray[0], 0);
            System.out.println("Result Decode: " + objectArray[0]);
        }
        this.a.call(objectArray);
    }
}

