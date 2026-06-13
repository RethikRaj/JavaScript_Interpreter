import java.util.List;
import java.util.Map;

public abstract class Node {}

// ── Statements ────────────────────────────────────────────────────────────────

class ProgramNode extends Node {
    public final List<Node> body;
    public ProgramNode(List<Node> body) { this.body = body; }
}

class VarDeclNode extends Node {
    public final String name;
    public final Node initializer;
    public VarDeclNode(String name, Node initializer) {
        this.name = name;
        this.initializer = initializer;
    }
}

class BlockNode extends Node {
    public final List<Node> statements;
    public BlockNode(List<Node> statements) { this.statements = statements; }
}

class IfNode extends Node {
    public final Node condition;
    public final Node thenBranch;
    public final Node elseBranch; // may be null
    public IfNode(Node condition, Node thenBranch, Node elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

class WhileNode extends Node {
    public final Node condition;
    public final Node body;
    public WhileNode(Node condition, Node body) {
        this.condition = condition;
        this.body = body;
    }
}

class DoWhileNode extends Node {
    public final Node body;
    public final Node condition;
    public DoWhileNode(Node body, Node condition) {
        this.body = body;
        this.condition = condition;
    }
}

class ForNode extends Node {
    public final Node init;       // may be null
    public final Node condition;  // may be null
    public final Node update;     // may be null
    public final Node body;
    public ForNode(Node init, Node condition, Node update, Node body) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}

class ForInNode extends Node {
    public final String varName;
    public final Node iterable;
    public final Node body;
    public ForInNode(String varName, Node iterable, Node body) {
        this.varName = varName;
        this.iterable = iterable;
        this.body = body;
    }
}

class FunctionDeclNode extends Node {
    public final String name;
    public final List<String> params;
    public final Node body;
    public FunctionDeclNode(String name, List<String> params, Node body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }
}

class ReturnNode extends Node {
    public final Node value; // may be null
    public ReturnNode(Node value) { this.value = value; }
}

class BreakNode extends Node {}
class ContinueNode extends Node {}

class ExpressionStatementNode extends Node {
    public final Node expression;
    public ExpressionStatementNode(Node expression) { this.expression = expression; }
}

class SwitchNode extends Node {
    public final Node discriminant;
    public final List<SwitchCaseNode> cases;
    public SwitchNode(Node discriminant, List<SwitchCaseNode> cases) {
        this.discriminant = discriminant;
        this.cases = cases;
    }
}

class SwitchCaseNode extends Node {
    public final Node test; // null = default
    public final List<Node> consequent;
    public SwitchCaseNode(Node test, List<Node> consequent) {
        this.test = test;
        this.consequent = consequent;
    }
}

// ── Expressions ───────────────────────────────────────────────────────────────

class NumberLiteralNode extends Node {
    public final double value;
    public NumberLiteralNode(double value) { this.value = value; }
}

class StringLiteralNode extends Node {
    public final String value;
    public StringLiteralNode(String value) { this.value = value; }
}

class BooleanLiteralNode extends Node {
    public final boolean value;
    public BooleanLiteralNode(boolean value) { this.value = value; }
}

class NullLiteralNode extends Node {}
class UndefinedLiteralNode extends Node {}

class IdentifierNode extends Node {
    public final String name;
    public IdentifierNode(String name) { this.name = name; }
}

class BinaryOpNode extends Node {
    public final String op;
    public final Node left;
    public final Node right;
    public BinaryOpNode(String op, Node left, Node right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}

class UnaryOpNode extends Node {
    public final String op;
    public final Node operand;
    public final boolean prefix;
    public UnaryOpNode(String op, Node operand, boolean prefix) {
        this.op = op;
        this.operand = operand;
        this.prefix = prefix;
    }
}

class LogicalOpNode extends Node {
    public final String op; // && or ||
    public final Node left;
    public final Node right;
    public LogicalOpNode(String op, Node left, Node right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }
}

class AssignmentNode extends Node {
    public final String op;  // = += -= *= /= %=
    public final Node target; // IdentifierNode or MemberAccessNode
    public final Node value;
    public AssignmentNode(String op, Node target, Node value) {
        this.op = op;
        this.target = target;
        this.value = value;
    }
}

class TernaryNode extends Node {
    public final Node condition;
    public final Node thenExpr;
    public final Node elseExpr;
    public TernaryNode(Node condition, Node thenExpr, Node elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
}

class CallNode extends Node {
    public final Node callee;
    public final List<Node> args;
    public CallNode(Node callee, List<Node> args) {
        this.callee = callee;
        this.args = args;
    }
}

class MemberAccessNode extends Node {
    public final Node object;
    public final String property;
    public MemberAccessNode(Node object, String property) {
        this.object = object;
        this.property = property;
    }
}

class ComputedMemberAccessNode extends Node {
    public final Node object;
    public final Node index;
    public ComputedMemberAccessNode(Node object, Node index) {
        this.object = object;
        this.index = index;
    }
}

class ArrayLiteralNode extends Node {
    public final List<Node> elements;
    public ArrayLiteralNode(List<Node> elements) { this.elements = elements; }
}

class ObjectLiteralNode extends Node {
    public final List<String> keys;
    public final List<Node> values;
    public ObjectLiteralNode(List<String> keys, List<Node> values) {
        this.keys = keys;
        this.values = values;
    }
}

class FunctionExprNode extends Node {
    public final String name; // may be null
    public final List<String> params;
    public final Node body;
    public FunctionExprNode(String name, List<String> params, Node body) {
        this.name = name;
        this.params = params;
        this.body = body;
    }
}

class ArrowFunctionNode extends Node {
    public final List<String> params;
    public final Node body; // BlockNode or expression
    public ArrowFunctionNode(List<String> params, Node body) {
        this.params = params;
        this.body = body;
    }
}

class SpreadNode extends Node {
    public final Node expression;
    public SpreadNode(Node expression) { this.expression = expression; }
}

class NewNode extends Node {
    public final Node callee;
    public final List<Node> args;
    public NewNode(Node callee, List<Node> args) {
        this.callee = callee;
        this.args = args;
    }
}

class TypeofNode extends Node {
    public final Node operand;
    public TypeofNode(Node operand) { this.operand = operand; }
}