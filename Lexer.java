import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private int pos;          // current position in source
    private final List<Token> tokens = new ArrayList<>();

    // All JS keywords mapped to their token types
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("let",        TokenType.LET),
        Map.entry("const",      TokenType.CONST),
        Map.entry("var",        TokenType.VAR),
        Map.entry("if",         TokenType.IF),
        Map.entry("else",       TokenType.ELSE),
        Map.entry("while",      TokenType.WHILE),
        Map.entry("for",        TokenType.FOR),
        Map.entry("do",         TokenType.DO),
        Map.entry("function",   TokenType.FUNCTION),
        Map.entry("return",     TokenType.RETURN),
        Map.entry("true",       TokenType.TRUE),
        Map.entry("false",      TokenType.FALSE),
        Map.entry("null",       TokenType.NULL_KW),
        Map.entry("undefined",  TokenType.UNDEFINED),
        Map.entry("new",        TokenType.NEW),
        Map.entry("this",       TokenType.THIS),
        Map.entry("switch",     TokenType.SWITCH),
        Map.entry("case",       TokenType.CASE),
        Map.entry("default",    TokenType.DEFAULT),
        Map.entry("break",      TokenType.BREAK),
        Map.entry("continue",   TokenType.CONTINUE),
        Map.entry("typeof",     TokenType.TYPEOF),
        Map.entry("instanceof", TokenType.INSTANCEOF)
    );

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
    }

    // Main entry point - tokenize everything and return the list
    public List<Token> tokenize() {
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;

            char c = source.charAt(pos);

            if (Character.isDigit(c)) {
                readNumber();
            } else if (c == '"' || c == '\'' || c == '`') {
                readString(c);
            } else if (Character.isLetter(c) || c == '_' || c == '$') {
                readIdentifierOrKeyword();
            } else {
                readSymbol();
            }
        }

        tokens.add(new Token(TokenType.EOF));
        return tokens;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private char current() {
        return source.charAt(pos);
    }

    private char peek(int offset) {
        int i = pos + offset;
        return i < source.length() ? source.charAt(i) : '\0';
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (c == '/' && peek(1) == '/') {
                // Single-line comment — skip to end of line
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            } else if (c == '/' && peek(1) == '*') {
                // Multi-line comment — skip until */
                pos += 2;
                while (pos < source.length() - 1 &&
                       !(source.charAt(pos) == '*' && source.charAt(pos + 1) == '/')) {
                    pos++;
                }
                pos += 2;
            } else {
                break;
            }
        }
    }

    // ── Readers ──────────────────────────────────────────────────────────────

    private void readNumber() {
        int start = pos;
        while (pos < source.length() && (Character.isDigit(current()) || current() == '.')) {
            pos++;
        }
        tokens.add(new Token(TokenType.NUMBER, source.substring(start, pos)));
    }

    private void readString(char quote) {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && current() != quote) {
            if (current() == '\\') {
                pos++;
                switch (current()) {
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case 'r'  -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case '\'' -> sb.append('\'');
                    case '"'  -> sb.append('"');
                    case '`'  -> sb.append('`');
                    default   -> sb.append(current());
                }
            } else if (quote == '`' && current() == '$' && peek(1) == '{') {
                // Template literal interpolation — treat as string for now, skip ${...}
                // This is a simplification; full template literals need parser support
                pos += 2; // skip ${
                int depth = 1;
                while (pos < source.length() && depth > 0) {
                    if (current() == '{') depth++;
                    else if (current() == '}') depth--;
                    if (depth > 0) sb.append(current());
                    pos++;
                }
                continue;
            } else {
                sb.append(current());
            }
            pos++;
        }
        pos++; // skip closing quote
        tokens.add(new Token(TokenType.STRING, sb.toString()));
    }

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < source.length() &&
               (Character.isLetterOrDigit(current()) || current() == '_' || current() == '$')) {
            pos++;
        }
        String word = source.substring(start, pos);
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        tokens.add(new Token(type, word));
    }

    private void readSymbol() {
        char c = current();
        pos++;

        switch (c) {
            case '+' -> {
                if (pos < source.length() && current() == '+') { pos++; tokens.add(new Token(TokenType.PLUS_PLUS)); }
                else if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.PLUS_ASSIGN)); }
                else tokens.add(new Token(TokenType.PLUS));
            }
            case '-' -> {
                if (pos < source.length() && current() == '-') { pos++; tokens.add(new Token(TokenType.MINUS_MINUS)); }
                else if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.MINUS_ASSIGN)); }
                else tokens.add(new Token(TokenType.MINUS));
            }
            case '*' -> {
                if (pos < source.length() && current() == '*') { pos++; tokens.add(new Token(TokenType.STAR_STAR)); }
                else if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.STAR_ASSIGN)); }
                else tokens.add(new Token(TokenType.STAR));
            }
            case '/' -> {
                if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.SLASH_ASSIGN)); }
                else tokens.add(new Token(TokenType.SLASH));
            }
            case '%' -> {
                if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.PERCENT_ASSIGN)); }
                else tokens.add(new Token(TokenType.PERCENT));
            }
            case '=' -> {
                if (pos < source.length() && current() == '>') { pos++; tokens.add(new Token(TokenType.ARROW)); }
                else if (pos < source.length() && current() == '=') {
                    pos++;
                    if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.EQ_EQ_EQ)); }
                    else tokens.add(new Token(TokenType.EQ_EQ));
                } else tokens.add(new Token(TokenType.ASSIGN));
            }
            case '!' -> {
                if (pos < source.length() && current() == '=') {
                    pos++;
                    if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.NOT_EQ_EQ)); }
                    else tokens.add(new Token(TokenType.NOT_EQ));
                } else tokens.add(new Token(TokenType.NOT));
            }
            case '<' -> {
                if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.LT_EQ)); }
                else tokens.add(new Token(TokenType.LT));
            }
            case '>' -> {
                if (pos < source.length() && current() == '=') { pos++; tokens.add(new Token(TokenType.GT_EQ)); }
                else tokens.add(new Token(TokenType.GT));
            }
            case '&' -> { pos++; tokens.add(new Token(TokenType.AND)); } // assumes &&
            case '|' -> { pos++; tokens.add(new Token(TokenType.OR));  } // assumes ||
            case '.' -> {
                if (pos < source.length() && current() == '.' && peek(1) == '.') {
                    pos += 2; tokens.add(new Token(TokenType.DOT_DOT_DOT));
                } else tokens.add(new Token(TokenType.DOT));
            }
            case '(' -> tokens.add(new Token(TokenType.LPAREN));
            case ')' -> tokens.add(new Token(TokenType.RPAREN));
            case '{' -> tokens.add(new Token(TokenType.LBRACE));
            case '}' -> tokens.add(new Token(TokenType.RBRACE));
            case '[' -> tokens.add(new Token(TokenType.LBRACKET));
            case ']' -> tokens.add(new Token(TokenType.RBRACKET));
            case ';' -> tokens.add(new Token(TokenType.SEMICOLON));
            case ',' -> tokens.add(new Token(TokenType.COMMA));
            case ':' -> tokens.add(new Token(TokenType.COLON));
            case '?' -> tokens.add(new Token(TokenType.QUESTION));
            default  -> { /* skip unknown characters */ }
        }
    }
}