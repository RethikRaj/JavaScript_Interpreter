# JS Interpreter in Java

A JavaScript interpreter built from scratch in Java , no external JS engines, no transpilers. It implements a complete pipeline: **Lexer → Parser → AST → Tree-Walking Evaluator**.

## How It Works

1. **Lexer** — Reads the raw JS source character by character and produces a stream of tokens (e.g. `let`, `num`, `=`, `7`, `;`).
2. **Parser** — Reads the token stream and builds an Abstract Syntax Tree (AST) following JavaScript grammar rules.
3. **Interpreter** — Walks the AST recursively, executing each node: evaluating expressions, managing variable scopes, handling control flow, and printing output via `console.log`.

## Requirements

- Java 17 or higher

## How to Run

### 1. Clone the repository

```bash
git clone https://github.com/RethikRaj/JavaScript_Interpreter.git
cd JavaScript_Interpreter
```

### 2. Compile all source files

```bash
javac TokenType.java Token.java Node.java Lexer.java Parser.java Environment.java JSFunction.java JSArray.java JSObject.java Interpreter.java Main.java
```

### 3. Run a Java file

```bash
java Main <path-to-file.js>
```

**Example:**

```bash
java Main tests/test1.js
```

## Example

Create a file `testcase1.js`  in tests folder

```jsx
let num = 7;
if (num % 2 === 0) {
    console.log(num + " is Even");
} else {
    console.log(num + " is Odd");
}
```

Run it:

```bash
java Main tests/testcase1.js
```

Output:

```
7 is Odd
```