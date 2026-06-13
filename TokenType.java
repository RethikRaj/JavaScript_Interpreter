public enum TokenType {
    // Literals
    NUMBER, STRING, BOOLEAN, NULL, UNDEFINED,

    // Identifiers & keywords
    IDENTIFIER,
    LET, CONST, VAR,
    IF, ELSE,
    WHILE, FOR, DO,
    FUNCTION, RETURN,
    NEW, THIS,
    SWITCH, CASE, DEFAULT, BREAK, CONTINUE,
    TYPEOF, INSTANCEOF,
    TRUE, FALSE,
    NULL_KW,

    // Arithmetic operators
    PLUS, MINUS, STAR, SLASH, PERCENT,
    STAR_STAR,       // **

    // Assignment operators
    ASSIGN,          // =
    PLUS_ASSIGN,     // +=
    MINUS_ASSIGN,    // -=
    STAR_ASSIGN,     // *=
    SLASH_ASSIGN,    // /=
    PERCENT_ASSIGN,  // %=

    // Comparison operators
    EQ_EQ,           // ==
    EQ_EQ_EQ,        // ===
    NOT_EQ,          // !=
    NOT_EQ_EQ,       // !==
    LT, GT,          // < >
    LT_EQ, GT_EQ,    // <= >=

    // Logical operators
    AND,             // &&
    OR,              // ||
    NOT,             // !

    // Increment / Decrement
    PLUS_PLUS,       // ++
    MINUS_MINUS,     // --

    // Punctuation
    LPAREN, RPAREN,  // ( )
    LBRACE, RBRACE,  // { }
    LBRACKET, RBRACKET, // [ ]
    SEMICOLON,       // ;
    COMMA,           // ,
    DOT,             // .
    COLON,           // :
    QUESTION,        // ?

    // Spread / Rest
    DOT_DOT_DOT,     // ...

    // Arrow function
    ARROW,           // =>

    // Special
    EOF
}