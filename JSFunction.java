import java.util.List;

public class JSFunction {
    public final String name;
    public final List<String> params;
    public final Node body;
    public final Environment closure; // scope where function was defined

    public JSFunction(String name, List<String> params, Node body, Environment closure) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
    }

    @Override
    public String toString() {
        return "[Function: " + (name != null ? name : "anonymous") + "]";
    }
}