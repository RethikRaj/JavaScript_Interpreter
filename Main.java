import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main <file.js>");
            System.exit(1);
        }

        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(args[0])));
        } catch (IOException e) {
            System.err.println("Error reading file: " + args[0]);
            System.exit(1);
            return;
        }

        try {
            var tokens = new Lexer(source).tokenize();
            var ast    = new Parser(tokens).parse();
            new Interpreter().execute(ast);
        } catch (Exception e) {
            System.err.println("Runtime error: " + e.getMessage());
            System.exit(1);
        }
    }
}