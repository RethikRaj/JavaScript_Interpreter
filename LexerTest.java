public class LexerTest {
    public static void main(String[] args) {
        String test = "let num = 7;";
        System.out.print("TOKENS: ");
        new Lexer(test).tokenize().forEach(t -> System.out.print(t + " "));
    }
}