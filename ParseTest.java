public class ParseTest {
    public static void main(String[] args) {
        String[] tests = {
            "let num = 7;",
            "if (num % 2 === 0) { console.log(\"Even\"); } else { console.log(\"Odd\"); }",
            "for (let i = 1; i <= 5; i++) { let row = \"\"; }",
            "function isArmstrong(num) { let sum = 0; return sum === num; }",
            "let arr = [1, 2, 3]; let reversed = [...arr].reverse();"
        };
        for (String src : tests) {
            try {
                var tokens = new Lexer(src).tokenize();
                var ast = new Parser(tokens).parse();
                System.out.println("OK: " + src.substring(0, Math.min(50, src.length())));
            } catch (Exception e) {
                System.out.println("FAIL: " + e.getMessage());
            }
        }
    }
}