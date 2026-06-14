import java.util.*;

public class Interpreter {

    // ── Signal exceptions for control flow ────────────────────────────────────
    static class ReturnSignal extends RuntimeException {
        final Object value;
        ReturnSignal(Object value) { super(null, null, true, false); this.value = value; }
    }
    static class BreakSignal extends RuntimeException {
        BreakSignal() { super(null, null, true, false); }
    }
    static class ContinueSignal extends RuntimeException {
        ContinueSignal() { super(null, null, true, false); }
    }

    // ── Global environment setup ───────────────────────────────────────────────
    private final Environment globalEnv;

    public Interpreter() {
        globalEnv = new Environment();
        setupGlobals(globalEnv);
    }

    private void setupGlobals(Environment env) {
        // Math object
        JSObject math = new JSObject();
        math.set("PI", Math.PI);
        math.set("E", Math.E);
        math.set("floor",  (JSFunction) null); // handled in callBuiltin
        math.set("ceil",   null);
        math.set("round",  null);
        math.set("abs",    null);
        math.set("sqrt",   null);
        math.set("pow",    null);
        math.set("max",    null);
        math.set("min",    null);
        math.set("random", null);
        math.set("log",    null);
        math.set("trunc",  null);
        math.set("sign",   null);
        env.define("Math", math);

        // console object
        JSObject console = new JSObject();
        env.define("console", console);

        // Number, String, Boolean, Array, Object constructors (as markers)
        env.define("Number",    "<<Number>>");
        env.define("String",    "<<String>>");
        env.define("Boolean",   "<<Boolean>>");
        env.define("Array",     "<<Array>>");
        env.define("Object",    "<<Object>>");
        env.define("Date",      "<<Date>>");
        env.define("parseInt",  "<<parseInt>>");
        env.define("parseFloat","<<parseFloat>>");
        env.define("isNaN",     "<<isNaN>>");
        env.define("isFinite",  "<<isFinite>>");
        env.define("undefined", null);
        env.define("NaN",       Double.NaN);
        env.define("Infinity",  Double.POSITIVE_INFINITY);
    }

    // ── Public entry ──────────────────────────────────────────────────────────

    public void execute(ProgramNode program) {
        for (Node stmt : program.body) {
            evalStatement(stmt, globalEnv);
        }
    }

    // ── Statement evaluator ───────────────────────────────────────────────────

    private void evalStatement(Node node, Environment env) {
        if (node instanceof VarDeclNode n) {
            Object value = n.initializer != null ? evalExpr(n.initializer, env) : null;
            env.define(n.name, value);

        } else if (node instanceof MultiVarDeclNode n) {
            for (Node decl : n.decls) evalStatement(decl, env);

        } else if (node instanceof ArrayDestructureDeclNode n) {
            Object source = evalExpr(n.initializer, env);
            List<Object> elems = (source instanceof JSArray arr) ? arr.elements : new ArrayList<>();
            for (int i = 0; i < n.names.size(); i++) {
                String name = n.names.get(i);
                if (name == null) continue; // skipped slot, e.g. [, , third]
                if (n.isRest.get(i)) {
                    // Rest element collects all remaining values into a new array
                    List<Object> rest = i < elems.size()
                            ? new ArrayList<>(elems.subList(i, elems.size()))
                            : new ArrayList<>();
                    env.define(name, new JSArray(rest));
                } else {
                    env.define(name, i < elems.size() ? elems.get(i) : null);
                }
            }

        } else if (node instanceof ObjectDestructureDeclNode n) {
            Object source = evalExpr(n.initializer, env);
            for (int i = 0; i < n.keys.size(); i++) {
                Object val = getMember(source, n.keys.get(i));
                env.define(n.names.get(i), val);
            }

        } else if (node instanceof BlockNode n) {
            Environment blockEnv = new Environment(env);
            for (Node stmt : n.statements) evalStatement(stmt, blockEnv);

        } else if (node instanceof ExpressionStatementNode n) {
            evalExpr(n.expression, env);

        } else if (node instanceof IfNode n) {
            if (isTruthy(evalExpr(n.condition, env))) {
                evalStatement(n.thenBranch, env);
            } else if (n.elseBranch != null) {
                evalStatement(n.elseBranch, env);
            }

        } else if (node instanceof WhileNode n) {
            while (isTruthy(evalExpr(n.condition, env))) {
                try { evalStatement(n.body, env); }
                catch (BreakSignal b) { break; }
                catch (ContinueSignal c) { /* continue */ }
            }

        } else if (node instanceof DoWhileNode n) {
            do {
                try { evalStatement(n.body, env); }
                catch (BreakSignal b) { break; }
                catch (ContinueSignal c) { /* continue */ }
            } while (isTruthy(evalExpr(n.condition, env)));

        } else if (node instanceof ForNode n) {
            Environment forEnv = new Environment(env);
            if (n.init != null) evalStatement(n.init, forEnv);
            while (n.condition == null || isTruthy(evalExpr(n.condition, forEnv))) {
                try { evalStatement(n.body, forEnv); }
                catch (BreakSignal b) { break; }
                catch (ContinueSignal c) { /* continue to update */ }
                if (n.update != null) evalExpr(n.update, forEnv);
            }

        } else if (node instanceof ForInNode n) {
            Object iterable = evalExpr(n.iterable, env);
            Environment forEnv = new Environment(env);
            if (iterable instanceof JSArray arr) {
                for (int i = 0; i < arr.elements.size(); i++) {
                    forEnv.define(n.varName, (double) i);
                    try { evalStatement(n.body, forEnv); }
                    catch (BreakSignal b) { break; }
                    catch (ContinueSignal c) { /* continue */ }
                }
            } else if (iterable instanceof JSObject obj) {
                for (String key : obj.props.keySet()) {
                    forEnv.define(n.varName, key);
                    try { evalStatement(n.body, forEnv); }
                    catch (BreakSignal b) { break; }
                    catch (ContinueSignal c) { /* continue */ }
                }
            }

        } else if (node instanceof FunctionDeclNode n) {
            env.define(n.name, new JSFunction(n.name, n.params, n.body, env));

        } else if (node instanceof ReturnNode n) {
            Object value = n.value != null ? evalExpr(n.value, env) : null;
            throw new ReturnSignal(value);

        } else if (node instanceof BreakNode) {
            throw new BreakSignal();

        } else if (node instanceof ContinueNode) {
            throw new ContinueSignal();

        } else if (node instanceof SwitchNode n) {
            Object disc = evalExpr(n.discriminant, env);
            boolean matched = false;
            outer:
            for (SwitchCaseNode c : n.cases) {
                if (!matched && c.test != null) {
                    matched = jsStrictEquals(disc, evalExpr(c.test, env));
                } else if (c.test == null) {
                    matched = true; // default
                }
                if (matched) {
                    try {
                        for (Node s : c.consequent) evalStatement(s, env);
                    } catch (BreakSignal b) { break outer; }
                }
            }

        } else if (node instanceof ProgramNode n) {
            for (Node stmt : n.body) evalStatement(stmt, env);
        }
    }

    // ── Expression evaluator ──────────────────────────────────────────────────

    Object evalExpr(Node node, Environment env) {

        if (node instanceof NumberLiteralNode n)  return n.value;
        if (node instanceof StringLiteralNode n)  return n.value;
        if (node instanceof BooleanLiteralNode n) return n.value;
        if (node instanceof NullLiteralNode)      return null;
        if (node instanceof UndefinedLiteralNode) return null;

        if (node instanceof IdentifierNode n) {
            return env.get(n.name);
        }

        if (node instanceof TypeofNode n) {
            try {
                Object val = evalExpr(n.operand, env);
                return jsTypeof(val);
            } catch (RuntimeException e) { return "undefined"; }
        }

        if (node instanceof ArrayLiteralNode n) {
            List<Object> elems = new ArrayList<>();
            for (Node el : n.elements) {
                if (el instanceof SpreadNode s) {
                    Object spread = evalExpr(s.expression, env);
                    if (spread instanceof JSArray arr) elems.addAll(arr.elements);
                } else {
                    elems.add(evalExpr(el, env));
                }
            }
            return new JSArray(elems);
        }

        if (node instanceof ObjectLiteralNode n) {
            JSObject obj = new JSObject();
            for (int i = 0; i < n.keys.size(); i++) {
                obj.set(n.keys.get(i), evalExpr(n.values.get(i), env));
            }
            return obj;
        }

        if (node instanceof FunctionExprNode n) {
            return new JSFunction(n.name, n.params, n.body, env);
        }

        if (node instanceof ArrowFunctionNode n) {
            return new JSFunction(null, n.params, n.body, env);
        }

        if (node instanceof AssignmentNode n) {
            return evalAssignment(n, env);
        }

        if (node instanceof BinaryOpNode n) {
            return evalBinaryOp(n, env);
        }

        if (node instanceof LogicalOpNode n) {
            Object left = evalExpr(n.left, env);
            if (n.op.equals("&&")) return isTruthy(left) ? evalExpr(n.right, env) : left;
            else return isTruthy(left) ? left : evalExpr(n.right, env);
        }

        if (node instanceof UnaryOpNode n) {
            return evalUnary(n, env);
        }

        if (node instanceof TernaryNode n) {
            return isTruthy(evalExpr(n.condition, env))
                    ? evalExpr(n.thenExpr, env)
                    : evalExpr(n.elseExpr, env);
        }

        if (node instanceof MemberAccessNode n) {
            Object obj = evalExpr(n.object, env);
            return getMember(obj, n.property);
        }

        if (node instanceof ComputedMemberAccessNode n) {
            Object obj = evalExpr(n.object, env);
            Object index = evalExpr(n.index, env);
            String key = jsToString(index);
            // If index is a whole number, use it as int index
            if (index instanceof Double d && d == Math.floor(d)) {
                return getMemberByIndex(obj, (int) d.doubleValue(), key);
            }
            return getMember(obj, key);
        }

        if (node instanceof CallNode n) {
            return evalCall(n, env);
        }

        if (node instanceof NewNode n) {
            return evalNew(n, env);
        }

        if (node instanceof SpreadNode n) {
            return evalExpr(n.expression, env);
        }

        throw new RuntimeException("Unknown node type: " + node.getClass().getSimpleName());
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    private Object evalAssignment(AssignmentNode n, Environment env) {
        Object value = evalExpr(n.value, env);

        // Array destructuring assignment: [a, b] = [b, a]
        if (n.target instanceof ArrayLiteralNode al && n.op.equals("=")) {
            List<Object> elems = (value instanceof JSArray arr) ? arr.elements : new ArrayList<>();
            for (int i = 0; i < al.elements.size(); i++) {
                Node target = al.elements.get(i);
                if (target instanceof SpreadNode s) {
                    List<Object> rest = i < elems.size()
                            ? new ArrayList<>(elems.subList(i, elems.size()))
                            : new ArrayList<>();
                    setVar(s.expression, new JSArray(rest), env);
                } else {
                    Object v = i < elems.size() ? elems.get(i) : null;
                    setVar(target, v, env);
                }
            }
            return value;
        }

        if (n.target instanceof IdentifierNode id) {
            Object result = applyAssignOp(n.op, env.has(id.name) ? env.get(id.name) : null, value);
            env.set(id.name, result);
            return result;

        } else if (n.target instanceof MemberAccessNode ma) {
            Object obj = evalExpr(ma.object, env);
            Object current = getMember(obj, ma.property);
            Object result = applyAssignOp(n.op, current, value);
            setMember(obj, ma.property, result);
            return result;

        } else if (n.target instanceof ComputedMemberAccessNode cm) {
            Object obj = evalExpr(cm.object, env);
            Object index = evalExpr(cm.index, env);
            String key = jsToString(index);
            Object current = getMember(obj, key);
            Object result = applyAssignOp(n.op, current, value);
            setMember(obj, key, result);
            return result;
        }
        throw new RuntimeException("Invalid assignment target");
    }

    private Object applyAssignOp(String op, Object current, Object value) {
        return switch (op) {
            case "="  -> value;
            case "+=" -> jsAdd(current, value);
            case "-=" -> toNumber(current) - toNumber(value);
            case "*=" -> toNumber(current) * toNumber(value);
            case "/=" -> toNumber(current) / toNumber(value);
            case "%=" -> toNumber(current) % toNumber(value);
            default -> value;
        };
    }

    // ── Binary operations ─────────────────────────────────────────────────────

    private Object evalBinaryOp(BinaryOpNode n, Environment env) {
        Object left  = evalExpr(n.left, env);
        Object right = evalExpr(n.right, env);

        return switch (n.op) {
            case "+"   -> jsAdd(left, right);
            case "-"   -> toNumber(left) - toNumber(right);
            case "*"   -> toNumber(left) * toNumber(right);
            case "/"   -> toNumber(left) / toNumber(right);
            case "%"   -> toNumber(left) % toNumber(right);
            case "**"  -> Math.pow(toNumber(left), toNumber(right));
            case "=="  -> jsLooseEquals(left, right);
            case "===" -> jsStrictEquals(left, right);
            case "!="  -> !jsLooseEquals(left, right);
            case "!==" -> !jsStrictEquals(left, right);
            case "<"   -> jsLessThan(left, right);
            case ">"   -> jsLessThan(right, left);
            case "<="  -> !jsLessThan(right, left);
            case ">="  -> !jsLessThan(left, right);
            case "instanceof" -> left instanceof JSObject; // simplified
            default -> throw new RuntimeException("Unknown operator: " + n.op);
        };
    }

    private Object jsAdd(Object left, Object right) {
        if (left instanceof String || right instanceof String)
            return jsToString(left) + jsToString(right);
        return toNumber(left) + toNumber(right);
    }

    private boolean jsLessThan(Object a, Object b) {
        if (a instanceof String sa && b instanceof String sb) return sa.compareTo(sb) < 0;
        return toNumber(a) < toNumber(b);
    }

    private boolean jsLooseEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Boolean || b instanceof Boolean)
            return toNumber(a) == toNumber(b);
        if (a instanceof Double && b instanceof Double) return a.equals(b);
        if (a instanceof String && b instanceof String) return a.equals(b);
        if ((a instanceof Double && b instanceof String) || (a instanceof String && b instanceof Double))
            return toNumber(a) == toNumber(b);
        return a == b;
    }

    private boolean jsStrictEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    // ── Unary operations ──────────────────────────────────────────────────────

    private Object evalUnary(UnaryOpNode n, Environment env) {
        if (n.op.equals("++") || n.op.equals("--")) {
            double delta = n.op.equals("++") ? 1 : -1;
            double current = toNumber(evalExpr(n.operand, env));
            double after = current + delta;
            // Write back
            setVar(n.operand, after, env);
            return n.prefix ? after : current;
        }
        Object val = evalExpr(n.operand, env);
        return switch (n.op) {
            case "!"  -> !isTruthy(val);
            case "-"  -> -toNumber(val);
            case "+"  -> toNumber(val);
            default   -> throw new RuntimeException("Unknown unary op: " + n.op);
        };
    }

    private void setVar(Node target, Object value, Environment env) {
        if (target instanceof IdentifierNode id) { env.set(id.name, value); }
        else if (target instanceof MemberAccessNode ma) {
            Object obj = evalExpr(ma.object, env);
            setMember(obj, ma.property, value);
        } else if (target instanceof ComputedMemberAccessNode cm) {
            Object obj = evalExpr(cm.object, env);
            Object index = evalExpr(cm.index, env);
            setMember(obj, jsToString(index), value);
        }
    }

    // ── Function calls ────────────────────────────────────────────────────────

    private Object evalCall(CallNode n, Environment env) {
        // console.log(...)
        if (n.callee instanceof MemberAccessNode ma) {
            Object obj = evalExpr(ma.object, env);
            String method = ma.property;

            // console.log / console.error etc.
            if (obj instanceof JSObject && ma.object instanceof IdentifierNode id
                    && id.name.equals("console")) {
                List<Object> args = evalArgs(n.args, env);
                if (method.equals("log") || method.equals("error") || method.equals("warn") || method.equals("info")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(jsToDisplayString(args.get(i)));
                    }
                    System.out.println(sb);
                }
                return null;
            }

            // Math.xxx(...)
            if (obj instanceof JSObject jsObj && ma.object instanceof IdentifierNode mid
                    && mid.name.equals("Math")) {
                List<Object> args = evalArgs(n.args, env);
                return callMath(method, args);
            }

            // Array / String methods
            List<Object> args = evalArgs(n.args, env);
            return callMethod(obj, method, args, env);
        }

        // Global function calls: parseInt, parseFloat, isNaN, etc.
        if (n.callee instanceof IdentifierNode id) {
            List<Object> args = evalArgs(n.args, env);
            return callGlobal(id.name, args, env);
        }

        // Expression callee (e.g. array of functions, IIFE)
        Object callee = evalExpr(n.callee, env);
        List<Object> args = evalArgs(n.args, env);
        return callFunction(callee, args, env);
    }

    private List<Object> evalArgs(List<Node> argNodes, Environment env) {
        List<Object> args = new ArrayList<>();
        for (Node arg : argNodes) {
            if (arg instanceof SpreadNode s) {
                Object val = evalExpr(s.expression, env);
                if (val instanceof JSArray arr) args.addAll(arr.elements);
            } else {
                args.add(evalExpr(arg, env));
            }
        }
        return args;
    }

    private Object callFunction(Object callee, List<Object> args, Environment env) {
        if (callee instanceof JSFunction fn) {
            Environment fnEnv = new Environment(fn.closure);
            for (int i = 0; i < fn.params.size(); i++) {
                fnEnv.define(fn.params.get(i), i < args.size() ? args.get(i) : null);
            }
            try {
                // Block body ({ ... }) -> run as statements, return via ReturnSignal
                // Expression body (arrow fn without braces) -> evaluate and return directly
                if (fn.body instanceof BlockNode) {
                    evalStatement(fn.body, fnEnv);
                    return null;
                } else {
                    return evalExpr(fn.body, fnEnv);
                }
            } catch (ReturnSignal r) {
                return r.value;
            }
        }
        throw new RuntimeException("TypeError: " + jsToString(callee) + " is not a function");
    }

    private Object callGlobal(String name, List<Object> args, Environment env) {
        return switch (name) {
            case "parseInt"  -> {
                if (args.isEmpty()) yield Double.NaN;
                String s = jsToString(args.get(0)).trim();
                int radix = args.size() > 1 ? (int) toNumber(args.get(1)) : 10;
                try { yield (double) Integer.parseInt(s.replaceAll("[^0-9a-fA-F\\-].*", ""), radix); }
                catch (NumberFormatException e) { yield Double.NaN; }
            }
            case "parseFloat" -> {
                if (args.isEmpty()) yield Double.NaN;
                try { yield Double.parseDouble(jsToString(args.get(0)).trim()); }
                catch (NumberFormatException e) { yield Double.NaN; }
            }
            case "isNaN"     -> args.isEmpty() || Double.isNaN(toNumber(args.get(0)));
            case "isFinite"  -> !args.isEmpty() && Double.isFinite(toNumber(args.get(0)));
            case "String"    -> args.isEmpty() ? "" : jsToString(args.get(0));
            case "Number"    -> args.isEmpty() ? 0.0 : toNumber(args.get(0));
            case "Boolean"   -> args.isEmpty() ? false : isTruthy(args.get(0));
            case "Array"     -> {
                JSArray arr = new JSArray();
                if (args.size() == 1 && args.get(0) instanceof Double d)
                    for (int i = 0; i < d.intValue(); i++) arr.elements.add(null);
                else arr.elements.addAll(args);
                yield arr;
            }
            default -> {
                Object callee = env.get(name);
                yield callFunction(callee, args, env);
            }
        };
    }

    private Object callMath(String method, List<Object> args) {
        double a = args.isEmpty() ? 0 : toNumber(args.get(0));
        return switch (method) {
            case "floor"  -> Math.floor(a);
            case "ceil"   -> Math.ceil(a);
            case "round"  -> (double) Math.round(a);
            case "abs"    -> Math.abs(a);
            case "sqrt"   -> Math.sqrt(a);
            case "log"    -> Math.log(a);
            case "trunc"  -> a >= 0 ? Math.floor(a) : Math.ceil(a);
            case "sign"   -> Math.signum(a);
            case "random" -> Math.random();
            case "pow"    -> Math.pow(a, toNumber(args.get(1)));
            case "max"    -> args.stream().mapToDouble(this::toNumber).max().orElse(Double.NEGATIVE_INFINITY);
            case "min"    -> args.stream().mapToDouble(this::toNumber).min().orElse(Double.POSITIVE_INFINITY);
            default -> throw new RuntimeException("Math." + method + " not implemented");
        };
    }

    // ── Method dispatch ───────────────────────────────────────────────────────

    private Object callMethod(Object obj, String method, List<Object> args, Environment env) {
        if (obj instanceof JSArray arr) return callArrayMethod(arr, method, args, env);
        if (obj instanceof String str)  return callStringMethod(str, method, args);
        if (obj instanceof Double d)    return callNumberMethod(d, method, args);
        if (isDate(obj)) return callDateMethod((JSObject) obj, method, args);
        if (obj instanceof JSObject jsObj) {
            Object fn = jsObj.props.get(method);
            if (fn instanceof JSFunction) return callFunction(fn, args, env);
        }
        throw new RuntimeException("TypeError: Cannot call method '" + method + "' on " + jsToString(obj));
    }

    private Object callArrayMethod(JSArray arr, String method, List<Object> args, Environment env) {
        return switch (method) {
            case "push" -> {
                arr.elements.addAll(args);
                yield (double) arr.elements.size();
            }
            case "pop" -> {
                if (arr.elements.isEmpty()) yield null;
                yield arr.elements.remove(arr.elements.size() - 1);
            }
            case "shift" -> {
                if (arr.elements.isEmpty()) yield null;
                yield arr.elements.remove(0);
            }
            case "unshift" -> {
                for (int i = args.size() - 1; i >= 0; i--) arr.elements.add(0, args.get(i));
                yield (double) arr.elements.size();
            }
            case "reverse" -> {
                Collections.reverse(arr.elements);
                yield arr;
            }
            case "join" -> {
                String sep = args.isEmpty() ? "," : jsToString(args.get(0));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.elements.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(jsToString(arr.elements.get(i)));
                }
                yield sb.toString();
            }
            case "slice" -> {
                int start = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                int end   = args.size() < 2 ? arr.elements.size() : (int) toNumber(args.get(1));
                if (start < 0) start = Math.max(0, arr.elements.size() + start);
                if (end < 0)   end   = Math.max(0, arr.elements.size() + end);
                start = Math.min(start, arr.elements.size());
                end   = Math.min(end,   arr.elements.size());
                yield new JSArray(arr.elements.subList(start, end));
            }
            case "splice" -> {
                int start = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                if (start < 0) start = Math.max(0, arr.elements.size() + start);
                int deleteCount = args.size() < 2 ? arr.elements.size() - start : (int) toNumber(args.get(1));
                List<Object> removed = new ArrayList<>();
                for (int i = 0; i < deleteCount && start < arr.elements.size(); i++)
                    removed.add(arr.elements.remove(start));
                for (int i = 2; i < args.size(); i++) arr.elements.add(start + i - 2, args.get(i));
                yield new JSArray(removed);
            }
            case "concat" -> {
                List<Object> result = new ArrayList<>(arr.elements);
                for (Object a : args) {
                    if (a instanceof JSArray other) result.addAll(other.elements);
                    else result.add(a);
                }
                yield new JSArray(result);
            }
            case "indexOf" -> {
                Object target = args.isEmpty() ? null : args.get(0);
                for (int i = 0; i < arr.elements.size(); i++)
                    if (jsStrictEquals(arr.elements.get(i), target)) yield (double) i;
                yield -1.0;
            }
            case "includes" -> {
                Object target = args.isEmpty() ? null : args.get(0);
                for (Object e : arr.elements) if (jsStrictEquals(e, target)) yield true;
                yield false;
            }
            case "sort" -> {
                if (args.isEmpty()) {
                    arr.elements.sort((a, b) -> jsToString(a).compareTo(jsToString(b)));
                } else {
                    JSFunction compareFn = (JSFunction) args.get(0);
                    arr.elements.sort((a, b) -> {
                        Object res = callFunction(compareFn, List.of(a, b), env);
                        return (int) Math.signum(toNumber(res));
                    });
                }
                yield arr;
            }
            case "forEach" -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.elements.size(); i++)
                    callFunction(fn, List.of(arr.elements.get(i), (double) i, arr), env);
                yield null;
            }
            case "map" -> {
                JSFunction fn = (JSFunction) args.get(0);
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < arr.elements.size(); i++)
                    result.add(callFunction(fn, List.of(arr.elements.get(i), (double) i, arr), env));
                yield new JSArray(result);
            }
            case "filter" -> {
                JSFunction fn = (JSFunction) args.get(0);
                List<Object> result = new ArrayList<>();
                for (int i = 0; i < arr.elements.size(); i++) {
                    Object el = arr.elements.get(i);
                    if (isTruthy(callFunction(fn, List.of(el, (double) i, arr), env))) result.add(el);
                }
                yield new JSArray(result);
            }
            case "reduce" -> {
                JSFunction fn = (JSFunction) args.get(0);
                int start = 0;
                Object acc = args.size() > 1 ? args.get(1) : arr.elements.get(start++);
                for (int i = start; i < arr.elements.size(); i++)
                    acc = callFunction(fn, List.of(acc, arr.elements.get(i), (double) i, arr), env);
                yield acc;
            }
            case "find" -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.elements.size(); i++) {
                    Object el = arr.elements.get(i);
                    if (isTruthy(callFunction(fn, List.of(el, (double) i, arr), env))) yield el;
                }
                yield null;
            }
            case "findIndex" -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.elements.size(); i++)
                    if (isTruthy(callFunction(fn, List.of(arr.elements.get(i), (double) i, arr), env))) yield (double) i;
                yield -1.0;
            }
            case "some" -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.elements.size(); i++)
                    if (isTruthy(callFunction(fn, List.of(arr.elements.get(i), (double) i, arr), env))) yield true;
                yield false;
            }
            case "every" -> {
                JSFunction fn = (JSFunction) args.get(0);
                for (int i = 0; i < arr.elements.size(); i++)
                    if (!isTruthy(callFunction(fn, List.of(arr.elements.get(i), (double) i, arr), env))) yield false;
                yield true;
            }
            case "flat" -> {
                int depth = args.isEmpty() ? 1 : (int) toNumber(args.get(0));
                yield flatArray(arr, depth);
            }
            case "fill" -> {
                Object val = args.isEmpty() ? null : args.get(0);
                int start = args.size() < 2 ? 0 : (int) toNumber(args.get(1));
                int end   = args.size() < 3 ? arr.elements.size() : (int) toNumber(args.get(2));
                for (int i = start; i < end && i < arr.elements.size(); i++) arr.elements.set(i, val);
                yield arr;
            }
            case "toString" -> arr.toString();
            case "length"   -> (double) arr.elements.size();
            default -> throw new RuntimeException("Array." + method + " not implemented");
        };
    }

    private JSArray flatArray(JSArray arr, int depth) {
        List<Object> result = new ArrayList<>();
        for (Object el : arr.elements) {
            if (el instanceof JSArray nested && depth > 0) result.addAll(flatArray(nested, depth - 1).elements);
            else result.add(el);
        }
        return new JSArray(result);
    }

    private Object callStringMethod(String str, String method, List<Object> args) {
        return switch (method) {
            case "length"       -> (double) str.length();
            case "toUpperCase"  -> str.toUpperCase();
            case "toLowerCase"  -> str.toLowerCase();
            case "trim"         -> str.trim();
            case "trimStart", "trimLeft"  -> str.stripLeading();
            case "trimEnd",   "trimRight" -> str.stripTrailing();
            case "split" -> {
                String sep = args.isEmpty() ? "" : jsToString(args.get(0));
                String[] parts = sep.isEmpty() ? str.split("") : str.split(Pattern.quote(sep), -1);
                List<Object> elems = new ArrayList<>(Arrays.asList(parts));
                yield new JSArray(elems);
            }
            case "join" -> str; // strings don't have join, but guard
            case "indexOf"      -> (double) str.indexOf(args.isEmpty() ? "" : jsToString(args.get(0)));
            case "lastIndexOf"  -> (double) str.lastIndexOf(args.isEmpty() ? "" : jsToString(args.get(0)));
            case "includes"     -> str.contains(args.isEmpty() ? "" : jsToString(args.get(0)));
            case "startsWith"   -> str.startsWith(args.isEmpty() ? "" : jsToString(args.get(0)));
            case "endsWith"     -> str.endsWith(args.isEmpty() ? "" : jsToString(args.get(0)));
            case "slice" -> {
                int start = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                int end   = args.size() < 2 ? str.length() : (int) toNumber(args.get(1));
                if (start < 0) start = Math.max(0, str.length() + start);
                if (end < 0)   end   = Math.max(0, str.length() + end);
                start = Math.min(start, str.length());
                end   = Math.min(end,   str.length());
                yield start > end ? "" : str.substring(start, end);
            }
            case "substring" -> {
                int start = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                int end   = args.size() < 2 ? str.length() : (int) toNumber(args.get(1));
                start = Math.max(0, Math.min(start, str.length()));
                end   = Math.max(0, Math.min(end, str.length()));
                if (start > end) { int t = start; start = end; end = t; }
                yield str.substring(start, end);
            }
            case "charAt"       -> {
                int i = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                yield (i >= 0 && i < str.length()) ? String.valueOf(str.charAt(i)) : "";
            }
            case "charCodeAt"   -> {
                int i = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                yield (i >= 0 && i < str.length()) ? (double) str.charAt(i) : Double.NaN;
            }
            case "replace"  -> str.replaceFirst(Pattern.quote(jsToString(args.get(0))), jsToString(args.get(1)));
            case "replaceAll" -> str.replace(jsToString(args.get(0)), jsToString(args.get(1)));
            case "repeat"   -> str.repeat((int) toNumber(args.get(0)));
            case "padStart" -> {
                int len = (int) toNumber(args.get(0));
                String pad = args.size() > 1 ? jsToString(args.get(1)) : " ";
                if (str.length() >= len) yield str;
                StringBuilder sb = new StringBuilder();
                while (sb.length() + str.length() < len) sb.append(pad);
                yield sb.substring(0, len - str.length()) + str;
            }
            case "padEnd" -> {
                int len = (int) toNumber(args.get(0));
                String pad = args.size() > 1 ? jsToString(args.get(1)) : " ";
                if (str.length() >= len) yield str;
                StringBuilder sb = new StringBuilder(str);
                while (sb.length() < len) sb.append(pad);
                yield sb.substring(0, len);
            }
            case "toString", "valueOf" -> str;
            case "match" -> {
                // simplified: return array of matches or null
                String pattern = jsToString(args.get(0));
                if (str.matches(".*" + pattern + ".*")) {
                    yield new JSArray(List.of(str));
                }
                yield null;
            }
            default -> throw new RuntimeException("String." + method + " not implemented");
        };
    }

    private Object callNumberMethod(Double num, String method, List<Object> args) {
        return switch (method) {
            case "toString" -> {
                int radix = args.isEmpty() ? 10 : (int) toNumber(args.get(0));
                if (radix == 10) yield jsToString(num);
                yield Integer.toString(num.intValue(), radix);
            }
            case "toFixed" -> {
                int digits = args.isEmpty() ? 0 : (int) toNumber(args.get(0));
                yield String.format("%." + digits + "f", num);
            }
            case "toLocaleString" -> jsToString(num);
            default -> throw new RuntimeException("Number." + method + " not implemented");
        };
    }

    // ── Date helpers ──────────────────────────────────────────────────────────

    private JSObject makeDate(List<Object> args) {
        java.util.Calendar cal;
        if (args.isEmpty()) {
            cal = java.util.Calendar.getInstance();
        } else if (args.size() == 1 && args.get(0) instanceof String s) {
            cal = java.util.Calendar.getInstance();
            java.util.Date parsed = parseDateString(s);
            cal.setTime(parsed);
        } else if (args.size() == 1) {
            cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis((long) toNumber(args.get(0)));
        } else {
            // new Date(year, month, day, hours, minutes, seconds)
            cal = java.util.Calendar.getInstance();
            cal.clear();
            int year   = (int) toNumber(args.get(0));
            int month  = args.size() > 1 ? (int) toNumber(args.get(1)) : 0;
            int day    = args.size() > 2 ? (int) toNumber(args.get(2)) : 1;
            int hour   = args.size() > 3 ? (int) toNumber(args.get(3)) : 0;
            int minute = args.size() > 4 ? (int) toNumber(args.get(4)) : 0;
            int second = args.size() > 5 ? (int) toNumber(args.get(5)) : 0;
            cal.set(year, month, day, hour, minute, second);
        }
        JSObject date = new JSObject();
        date.set("__calendar__", cal);
        return date;
    }

    private java.util.Date parseDateString(String s) {
        s = s.trim();
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM/dd/yyyy",
            "EEE MMM dd yyyy HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt);
                sdf.setLenient(false);
                return sdf.parse(s);
            } catch (java.text.ParseException ignored) {}
        }
        // Fallback: try generic Date parsing
        try { return new java.util.Date(s); } catch (Exception e) { return new java.util.Date(); }
    }

    private boolean isDate(Object obj) {
        return obj instanceof JSObject jo && jo.props.containsKey("__calendar__");
    }

    private java.util.Calendar getCal(JSObject date) {
        return (java.util.Calendar) date.props.get("__calendar__");
    }

    private static final String[] WEEKDAY_NAMES = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
    private static final String[] MONTH_NAMES   = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

    private Object callDateMethod(JSObject date, String method, List<Object> args) {
        java.util.Calendar cal = getCal(date);
        return switch (method) {
            case "getFullYear"  -> (double) cal.get(java.util.Calendar.YEAR);
            case "getMonth"     -> (double) cal.get(java.util.Calendar.MONTH); // 0-based
            case "getDate"      -> (double) cal.get(java.util.Calendar.DAY_OF_MONTH);
            case "getDay"       -> (double) (cal.get(java.util.Calendar.DAY_OF_WEEK) - 1); // 0 = Sunday
            case "getHours"     -> (double) cal.get(java.util.Calendar.HOUR_OF_DAY);
            case "getMinutes"   -> (double) cal.get(java.util.Calendar.MINUTE);
            case "getSeconds"   -> (double) cal.get(java.util.Calendar.SECOND);
            case "getMilliseconds" -> (double) cal.get(java.util.Calendar.MILLISECOND);
            case "getTime"      -> (double) cal.getTimeInMillis();
            case "getTimezoneOffset" -> (double) (-cal.get(java.util.Calendar.ZONE_OFFSET) / 60000);

            case "setFullYear"  -> { cal.set(java.util.Calendar.YEAR, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setMonth"     -> { cal.set(java.util.Calendar.MONTH, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setDate"      -> { cal.set(java.util.Calendar.DAY_OF_MONTH, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setHours"     -> { cal.set(java.util.Calendar.HOUR_OF_DAY, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setMinutes"   -> { cal.set(java.util.Calendar.MINUTE, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setSeconds"   -> { cal.set(java.util.Calendar.SECOND, (int) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }
            case "setTime"      -> { cal.setTimeInMillis((long) toNumber(args.get(0))); yield (double) cal.getTimeInMillis(); }

            case "toString", "toDateString" -> dateToString(cal, method);
            case "toTimeString" -> String.format("%02d:%02d:%02d GMT", cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND));
            case "toISOString" -> {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                yield sdf.format(cal.getTime());
            }
            case "valueOf" -> (double) cal.getTimeInMillis();
            default -> throw new RuntimeException("Date." + method + " not implemented");
        };
    }

    private String dateToString(java.util.Calendar cal, String method) {
        String day   = WEEKDAY_NAMES[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
        String month = MONTH_NAMES[cal.get(java.util.Calendar.MONTH)];
        int date     = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int year     = cal.get(java.util.Calendar.YEAR);
        if (method.equals("toDateString")) {
            return String.format("%s %s %02d %d", day, month, date, year);
        }
        // full toString
        return String.format("%s %s %02d %d %02d:%02d:%02d GMT", day, month, date, year,
                cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND));
    }

    // ── new keyword ───────────────────────────────────────────────────────────

    private Object evalNew(NewNode n, Environment env) {
        List<Object> args = evalArgs(n.args, env);

        // new Date(...) — handle before evaluating callee (Date isn't a real function)
        if (n.callee instanceof IdentifierNode id0 && id0.name.equals("Date")) {
            return makeDate(args);
        }

        // new Array(n) or new Array(a, b, ...)
        if (n.callee instanceof IdentifierNode id1 && id1.name.equals("Array")) {
            return callGlobal("Array", args, env);
        }

        Object callee = evalExpr(n.callee, env);

        // User-defined constructor
        if (callee instanceof JSFunction fn) {
            JSObject obj = new JSObject();
            Environment fnEnv = new Environment(fn.closure);
            fnEnv.define("this", obj);
            for (int i = 0; i < fn.params.size(); i++)
                fnEnv.define(fn.params.get(i), i < args.size() ? args.get(i) : null);
            try { evalStatement(fn.body, fnEnv); }
            catch (ReturnSignal r) { if (r.value instanceof JSObject) return r.value; }
            return obj;
        }
        throw new RuntimeException("TypeError: " + jsToString(callee) + " is not a constructor");
    }

    // ── Member access ─────────────────────────────────────────────────────────

    private Object getMember(Object obj, String prop) {
        if (obj instanceof JSArray arr) {
            return switch (prop) {
                case "length" -> (double) arr.elements.size();
                default -> {
                    try { int i = Integer.parseInt(prop); yield i >= 0 && i < arr.elements.size() ? arr.elements.get(i) : null; }
                    catch (NumberFormatException e) { yield prop; } // return method name for dispatch
                }
            };
        }
        if (obj instanceof String str) {
            if (prop.equals("length")) return (double) str.length();
            try { int i = Integer.parseInt(prop); return i >= 0 && i < str.length() ? String.valueOf(str.charAt(i)) : null; }
            catch (NumberFormatException e) { return prop; }
        }
        if (obj instanceof JSObject jsObj) {
            if (jsObj.props.containsKey(prop)) return jsObj.props.get(prop);
            return null;
        }
        if (obj instanceof Double d) return prop; // for number.toFixed etc.
        return null;
    }

    private Object getMemberByIndex(Object obj, int index, String key) {
        if (obj instanceof JSArray arr) {
            return index >= 0 && index < arr.elements.size() ? arr.elements.get(index) : null;
        }
        if (obj instanceof String str) {
            return index >= 0 && index < str.length() ? String.valueOf(str.charAt(index)) : null;
        }
        return getMember(obj, key);
    }

    private void setMember(Object obj, String prop, Object value) {
        if (obj instanceof JSArray arr) {
            try {
                int i = Integer.parseInt(prop);
                while (arr.elements.size() <= i) arr.elements.add(null);
                arr.elements.set(i, value);
                return;
            } catch (NumberFormatException ignored) {}
        }
        if (obj instanceof JSObject jsObj) { jsObj.set(prop, value); return; }
        throw new RuntimeException("Cannot set property '" + prop + "' on " + jsToString(obj));
    }

    // ── Type utilities ────────────────────────────────────────────────────────

    boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        if (val instanceof Double d) return d != 0 && !Double.isNaN(d);
        if (val instanceof String s) return !s.isEmpty();
        return true;
    }

    double toNumber(Object val) {
        if (val == null) return 0;
        if (val instanceof Double d) return d;
        if (val instanceof Boolean b) return b ? 1 : 0;
        if (val instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return 0;
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return Double.NaN; }
        }
        if (val instanceof JSObject jo && jo.props.containsKey("__calendar__")) {
            return (double) ((java.util.Calendar) jo.props.get("__calendar__")).getTimeInMillis();
        }
        return Double.NaN;
    }

    static String jsToString(Object val) {
        if (val == null) return "null";
        if (val instanceof Boolean b) return b.toString();
        if (val instanceof Double d) {
            if (Double.isNaN(d)) return "NaN";
            if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
            if (d == Math.floor(d) && Math.abs(d) < 1e15) return String.valueOf(d.longValue());
            return d.toString();
        }
        if (val instanceof String s) return s;
        if (val instanceof JSArray arr) return arr.toString();
        if (val instanceof JSObject obj) {
            if (obj.props.containsKey("__calendar__")) {
                return dateToStringStatic((java.util.Calendar) obj.props.get("__calendar__"));
            }
            return "[object Object]";
        }
        if (val instanceof JSFunction fn) return fn.toString();
        return val.toString();
    }

    // Static version of full Date.toString() used by jsToString (no instance access needed)
    private static String dateToStringStatic(java.util.Calendar cal) {
        String[] weekdays = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        String[] months   = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        String day   = weekdays[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
        String month = months[cal.get(java.util.Calendar.MONTH)];
        int date     = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int year     = cal.get(java.util.Calendar.YEAR);
        return String.format("%s %s %02d %d %02d:%02d:%02d GMT", day, month, date, year,
                cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND));
    }

    // Display string for console.log (booleans/null show as-is, not quoted)
    private String jsToDisplayString(Object val) {
        return jsToString(val);
    }

    private String jsTypeof(Object val) {
        if (val == null) return "undefined";
        if (val instanceof Boolean) return "boolean";
        if (val instanceof Double) return "number";
        if (val instanceof String) return "string";
        if (val instanceof JSFunction) return "function";
        if (val instanceof JSObject || val instanceof JSArray) return "object";
        return "undefined";
    }
}

// needed for string split
class Pattern {
    static String quote(String s) { return java.util.regex.Pattern.quote(s); }
}