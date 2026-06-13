import java.util.ArrayList;
import java.util.List;

public class JSArray {
    public final List<Object> elements;

    public JSArray(List<Object> elements) {
        this.elements = new ArrayList<>(elements);
    }

    public JSArray() {
        this.elements = new ArrayList<>();
    }

    public int length() { return elements.size(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(Interpreter.jsToString(elements.get(i)));
        }
        return sb.toString();
    }
}