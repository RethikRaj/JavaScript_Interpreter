import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> vars = new HashMap<>();
    private final Environment parent;

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public Environment() {
        this(null);
    }

    // Define a new variable in THIS scope
    public void define(String name, Object value) {
        vars.put(name, value);
    }

    // Get variable — walk up scope chain
    public Object get(String name) {
        if (vars.containsKey(name)) return vars.get(name);
        if (parent != null) return parent.get(name);
        throw new RuntimeException("ReferenceError: " + name + " is not defined");
    }

    // Set variable — find where it's defined and update there
    public void set(String name, Object value) {
        if (vars.containsKey(name)) { vars.put(name, value); return; }
        if (parent != null) { parent.set(name, value); return; }
        // If not found anywhere, define globally (JS behaviour for undeclared)
        vars.put(name, value);
    }

    public boolean has(String name) {
        if (vars.containsKey(name)) return true;
        if (parent != null) return parent.has(name);
        return false;
    }
}