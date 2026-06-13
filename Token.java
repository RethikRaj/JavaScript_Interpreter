public class Token {
    public final TokenType type;
    public final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public Token(TokenType type) {
        this(type, null);
    }

    @Override
    public String toString() {
        return value != null ? "Token(" + type + ", " + value + ")" : "Token(" + type + ")";
    }
}