import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Token current() { return tokens.get(pos); }

    private Token peek(int offset) {
        int i = pos + offset;
        return i < tokens.size() ? tokens.get(i) : tokens.get(tokens.size() - 1);
    }

    private Token consume() { return tokens.get(pos++); }

    private Token expect(TokenType type) {
        Token t = current();
        if (t.type != type)
            throw new RuntimeException("Expected " + type + " but got " + t.type + " ('" + t.value + "')");
        pos++;
        return t;
    }

    private boolean check(TokenType type) { return current().type == type; }

    private boolean match(TokenType type) {
        if (check(type)) { pos++; return true; }
        return false;
    }

    private void skipSemicolons() {
        while (check(TokenType.SEMICOLON)) pos++;
    }

    // ── Entry Point ───────────────────────────────────────────────────────────

    public ProgramNode parse() {
        List<Node> body = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            body.add(parseStatement());
            skipSemicolons();
        }
        return new ProgramNode(body);
    }

    // ── Statements ────────────────────────────────────────────────────────────

    private Node parseStatement() {
        return switch (current().type) {
            case LET, CONST, VAR    -> parseVarDecl();
            case IF                 -> parseIf();
            case FOR                -> parseFor();
            case WHILE              -> parseWhile();
            case DO                 -> parseDoWhile();
            case FUNCTION           -> parseFunctionDecl();
            case RETURN             -> parseReturn();
            case BREAK              -> { consume(); skipSemicolons(); yield new BreakNode(); }
            case CONTINUE           -> { consume(); skipSemicolons(); yield new ContinueNode(); }
            case LBRACE             -> parseBlock();
            case SWITCH             -> parseSwitch();
            default                 -> parseExpressionStatement();
        };
    }

    private Node parseVarDecl() {
        consume(); // let / const / var
        List<Node> decls = new ArrayList<>();
        do {
            String name = expect(TokenType.IDENTIFIER).value;
            Node init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            }
            decls.add(new VarDeclNode(name, init));
        } while (match(TokenType.COMMA));
        skipSemicolons();
        return decls.size() == 1 ? decls.get(0) : new BlockNode(decls);
    }

    private Node parseIf() {
        expect(TokenType.IF);
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        Node thenBranch = parseStatement();
        Node elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        return new IfNode(condition, thenBranch, elseBranch);
    }

    private Node parseWhile() {
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        Node body = parseStatement();
        return new WhileNode(condition, body);
    }

    private Node parseDoWhile() {
        expect(TokenType.DO);
        Node body = parseStatement();
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        skipSemicolons();
        return new DoWhileNode(body, condition);
    }

    private Node parseFor() {
        expect(TokenType.FOR);
        expect(TokenType.LPAREN);

        // Detect for...in: for (let x in arr)
        if ((check(TokenType.LET) || check(TokenType.CONST) || check(TokenType.VAR))
                && peek(1).type == TokenType.IDENTIFIER
                && peek(2).type == TokenType.IDENTIFIER
                && peek(2).value.equals("in")) {
            consume(); // let/const/var
            String varName = expect(TokenType.IDENTIFIER).value;
            expect(TokenType.IDENTIFIER); // "in"
            Node iterable = parseExpression();
            expect(TokenType.RPAREN);
            Node body = parseStatement();
            return new ForInNode(varName, iterable, body);
        }

        // Standard for(init; condition; update)
        Node init = null;
        if (!check(TokenType.SEMICOLON)) {
            if (check(TokenType.LET) || check(TokenType.CONST) || check(TokenType.VAR)) {
                init = parseVarDecl();
            } else {
                init = parseExpressionStatement();
            }
        } else {
            pos++;
        }

        Node condition = null;
        if (!check(TokenType.SEMICOLON)) condition = parseExpression();
        expect(TokenType.SEMICOLON);

        Node update = null;
        if (!check(TokenType.RPAREN)) update = parseExpression();
        expect(TokenType.RPAREN);

        Node body = parseStatement();
        return new ForNode(init, condition, update, body);
    }

    private FunctionDeclNode parseFunctionDecl() {
        expect(TokenType.FUNCTION);
        String name = expect(TokenType.IDENTIFIER).value;
        List<String> params = parseParamList();
        Node body = parseBlock();
        return new FunctionDeclNode(name, params, body);
    }

    private List<String> parseParamList() {
        expect(TokenType.LPAREN);
        List<String> params = new ArrayList<>();
        while (!check(TokenType.RPAREN)) {
            if (check(TokenType.DOT_DOT_DOT)) {
                consume(); // rest param — treat as normal identifier for now
            }
            params.add(expect(TokenType.IDENTIFIER).value);
            if (!check(TokenType.RPAREN)) expect(TokenType.COMMA);
        }
        expect(TokenType.RPAREN);
        return params;
    }

    private Node parseReturn() {
        expect(TokenType.RETURN);
        Node value = null;
        if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            value = parseExpression();
        }
        skipSemicolons();
        return new ReturnNode(value);
    }

    private BlockNode parseBlock() {
        expect(TokenType.LBRACE);
        List<Node> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            stmts.add(parseStatement());
            skipSemicolons();
        }
        expect(TokenType.RBRACE);
        return new BlockNode(stmts);
    }

    private Node parseSwitch() {
        expect(TokenType.SWITCH);
        expect(TokenType.LPAREN);
        Node discriminant = parseExpression();
        expect(TokenType.RPAREN);
        expect(TokenType.LBRACE);

        List<SwitchCaseNode> cases = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            Node test = null;
            if (match(TokenType.CASE)) {
                test = parseExpression();
                expect(TokenType.COLON);
            } else {
                expect(TokenType.DEFAULT);
                expect(TokenType.COLON);
            }
            List<Node> consequent = new ArrayList<>();
            while (!check(TokenType.CASE) && !check(TokenType.DEFAULT)
                    && !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                consequent.add(parseStatement());
                skipSemicolons();
            }
            cases.add(new SwitchCaseNode(test, consequent));
        }
        expect(TokenType.RBRACE);
        return new SwitchNode(discriminant, cases);
    }

    private Node parseExpressionStatement() {
        Node expr = parseExpression();
        skipSemicolons();
        return new ExpressionStatementNode(expr);
    }

    // ── Expressions (Pratt / precedence climbing) ─────────────────────────────

    private Node parseExpression() {
        return parseAssignment();
    }

    private Node parseAssignment() {
        Node left = parseTernary();

        TokenType t = current().type;
        String op = switch (t) {
            case ASSIGN         -> "=";
            case PLUS_ASSIGN    -> "+=";
            case MINUS_ASSIGN   -> "-=";
            case STAR_ASSIGN    -> "*=";
            case SLASH_ASSIGN   -> "/=";
            case PERCENT_ASSIGN -> "%=";
            default             -> null;
        };

        if (op != null) {
            consume();
            Node value = parseAssignment(); // right-associative
            return new AssignmentNode(op, left, value);
        }
        return left;
    }

    private Node parseTernary() {
        Node condition = parseLogicalOr();
        if (match(TokenType.QUESTION)) {
            Node thenExpr = parseExpression();
            expect(TokenType.COLON);
            Node elseExpr = parseTernary();
            return new TernaryNode(condition, thenExpr, elseExpr);
        }
        return condition;
    }

    private Node parseLogicalOr() {
        Node left = parseLogicalAnd();
        while (check(TokenType.OR)) {
            consume();
            left = new LogicalOpNode("||", left, parseLogicalAnd());
        }
        return left;
    }

    private Node parseLogicalAnd() {
        Node left = parseEquality();
        while (check(TokenType.AND)) {
            consume();
            left = new LogicalOpNode("&&", left, parseEquality());
        }
        return left;
    }

    private Node parseEquality() {
        Node left = parseComparison();
        while (check(TokenType.EQ_EQ) || check(TokenType.EQ_EQ_EQ)
                || check(TokenType.NOT_EQ) || check(TokenType.NOT_EQ_EQ)) {
            String op = current().type == TokenType.EQ_EQ ? "=="
                    : current().type == TokenType.EQ_EQ_EQ ? "==="
                    : current().type == TokenType.NOT_EQ ? "!=" : "!==";
            consume();
            left = new BinaryOpNode(op, left, parseComparison());
        }
        return left;
    }

    private Node parseComparison() {
        Node left = parseAddition();
        while (check(TokenType.LT) || check(TokenType.GT)
                || check(TokenType.LT_EQ) || check(TokenType.GT_EQ)
                || (check(TokenType.INSTANCEOF))) {
            String op = switch (current().type) {
                case LT -> "<"; case GT -> ">"; case LT_EQ -> "<="; case GT_EQ -> ">=";
                case INSTANCEOF -> "instanceof"; default -> "";
            };
            consume();
            left = new BinaryOpNode(op, left, parseAddition());
        }
        return left;
    }

    private Node parseAddition() {
        Node left = parseMultiplication();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = check(TokenType.PLUS) ? "+" : "-";
            consume();
            left = new BinaryOpNode(op, left, parseMultiplication());
        }
        return left;
    }

    private Node parseMultiplication() {
        Node left = parseExponentiation();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = check(TokenType.STAR) ? "*" : check(TokenType.SLASH) ? "/" : "%";
            consume();
            left = new BinaryOpNode(op, left, parseExponentiation());
        }
        return left;
    }

    private Node parseExponentiation() {
        Node left = parseUnary();
        if (check(TokenType.STAR_STAR)) {
            consume();
            return new BinaryOpNode("**", left, parseExponentiation()); // right-associative
        }
        return left;
    }

    private Node parseUnary() {
        if (check(TokenType.NOT)) {
            consume();
            return new UnaryOpNode("!", parseUnary(), true);
        }
        if (check(TokenType.MINUS)) {
            consume();
            return new UnaryOpNode("-", parseUnary(), true);
        }
        if (check(TokenType.PLUS)) {
            consume();
            return new UnaryOpNode("+", parseUnary(), true);
        }
        if (check(TokenType.TYPEOF)) {
            consume();
            return new TypeofNode(parseUnary());
        }
        if (check(TokenType.PLUS_PLUS)) {
            consume();
            return new UnaryOpNode("++", parsePostfix(), true);
        }
        if (check(TokenType.MINUS_MINUS)) {
            consume();
            return new UnaryOpNode("--", parsePostfix(), true);
        }
        return parsePostfix();
    }

    private Node parsePostfix() {
        Node node = parseCallAndAccess();
        if (check(TokenType.PLUS_PLUS)) {
            consume();
            return new UnaryOpNode("++", node, false);
        }
        if (check(TokenType.MINUS_MINUS)) {
            consume();
            return new UnaryOpNode("--", node, false);
        }
        return node;
    }

    private Node parseCallAndAccess() {
        Node node = parsePrimary();

        while (true) {
            if (check(TokenType.DOT)) {
                consume();
                String prop = expect(TokenType.IDENTIFIER).value;
                node = new MemberAccessNode(node, prop);
            } else if (check(TokenType.LBRACKET)) {
                consume();
                Node index = parseExpression();
                expect(TokenType.RBRACKET);
                node = new ComputedMemberAccessNode(node, index);
            } else if (check(TokenType.LPAREN)) {
                consume();
                List<Node> args = new ArrayList<>();
                while (!check(TokenType.RPAREN)) {
                    if (check(TokenType.DOT_DOT_DOT)) {
                        consume();
                        args.add(new SpreadNode(parseExpression()));
                    } else {
                        args.add(parseExpression());
                    }
                    if (!check(TokenType.RPAREN)) expect(TokenType.COMMA);
                }
                expect(TokenType.RPAREN);
                node = new CallNode(node, args);
            } else {
                break;
            }
        }
        return node;
    }

    private Node parsePrimary() {
        Token t = current();

        switch (t.type) {
            case NUMBER -> { consume(); return new NumberLiteralNode(Double.parseDouble(t.value)); }
            case STRING -> { consume(); return new StringLiteralNode(t.value); }
            case TRUE   -> { consume(); return new BooleanLiteralNode(true); }
            case FALSE  -> { consume(); return new BooleanLiteralNode(false); }
            case NULL_KW -> { consume(); return new NullLiteralNode(); }
            case UNDEFINED -> { consume(); return new UndefinedLiteralNode(); }

            case IDENTIFIER -> {
                consume();
                // Single-param arrow without parens: x => expr
                if (check(TokenType.ARROW)) {
                    consume(); // =>
                    Node body = check(TokenType.LBRACE) ? parseBlock() : parseExpression();
                    return new ArrowFunctionNode(List.of(t.value), body);
                }
                return new IdentifierNode(t.value);
            }

            case LPAREN -> {
                consume();
                // Check for arrow function: (params) =>
                if (isArrowFunctionParams()) {
                    return parseArrowFromParams();
                }
                Node expr = parseExpression();
                expect(TokenType.RPAREN);
                return expr;
            }

            case LBRACKET -> {
                consume();
                List<Node> elements = new ArrayList<>();
                while (!check(TokenType.RBRACKET)) {
                    if (check(TokenType.DOT_DOT_DOT)) {
                        consume();
                        elements.add(new SpreadNode(parseExpression()));
                    } else {
                        elements.add(parseExpression());
                    }
                    if (!check(TokenType.RBRACKET)) expect(TokenType.COMMA);
                }
                expect(TokenType.RBRACKET);
                return new ArrayLiteralNode(elements);
            }

            case LBRACE -> {
                consume();
                List<String> keys = new ArrayList<>();
                List<Node> values = new ArrayList<>();
                while (!check(TokenType.RBRACE)) {
                    String key;
                    if (check(TokenType.STRING)) { key = current().value; consume(); }
                    else if (check(TokenType.IDENTIFIER)) { key = current().value; consume(); }
                    else { key = current().value; consume(); } // number keys
                    expect(TokenType.COLON);
                    Node val = parseExpression();
                    keys.add(key);
                    values.add(val);
                    if (!check(TokenType.RBRACE)) expect(TokenType.COMMA);
                }
                expect(TokenType.RBRACE);
                return new ObjectLiteralNode(keys, values);
            }

            case FUNCTION -> {
                consume();
                String name = null;
                if (check(TokenType.IDENTIFIER)) { name = current().value; consume(); }
                List<String> params = parseParamList();
                Node body = parseBlock();
                return new FunctionExprNode(name, params, body);
            }

            case NEW -> {
                consume();
                Node callee = parseCallAndAccess();
                List<Node> args = new ArrayList<>();
                if (check(TokenType.LPAREN)) {
                    consume();
                    while (!check(TokenType.RPAREN)) {
                        args.add(parseExpression());
                        if (!check(TokenType.RPAREN)) expect(TokenType.COMMA);
                    }
                    expect(TokenType.RPAREN);
                }
                return new NewNode(callee, args);
            }

            case THIS -> { consume(); return new IdentifierNode("this"); }

            default -> throw new RuntimeException("Unexpected token in expression: " + t.type + " ('" + t.value + "')");
        }
    }

    // ── Arrow function helpers ────────────────────────────────────────────────

    private boolean isArrowFunctionParams() {
        // Look ahead from current pos (after consuming '(') to find ') =>'
        int save = pos;
        int depth = 1;
        while (pos < tokens.size() && depth > 0) {
            if (current().type == TokenType.LPAREN) depth++;
            else if (current().type == TokenType.RPAREN) depth--;
            if (depth > 0) pos++;
            else break;
        }
        boolean isArrow = pos + 1 < tokens.size() && tokens.get(pos + 1).type == TokenType.ARROW;
        pos = save;
        return isArrow;
    }

    private Node parseArrowFromParams() {
        // We've already consumed '(' — parse params then ') =>'
        List<String> params = new ArrayList<>();
        while (!check(TokenType.RPAREN)) {
            if (check(TokenType.DOT_DOT_DOT)) consume();
            params.add(expect(TokenType.IDENTIFIER).value);
            if (!check(TokenType.RPAREN)) expect(TokenType.COMMA);
        }
        expect(TokenType.RPAREN);
        expect(TokenType.ARROW);
        Node body = check(TokenType.LBRACE) ? parseBlock() : parseExpression();
        return new ArrowFunctionNode(params, body);
    }
}