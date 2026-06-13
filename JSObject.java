import java.util.LinkedHashMap;
import java.util.Map;

public class JSObject {
    public final Map<String, Object> props = new LinkedHashMap<>();

    public void set(String key, Object value) { props.put(key, value); }

    public Object get(String key) {
        return props.getOrDefault(key, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[object Object]");
        return sb.toString();
    }
}