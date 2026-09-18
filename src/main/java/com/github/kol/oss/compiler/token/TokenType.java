package com.github.kol.oss.compiler.token;

public enum TokenType {
    // numbers
    INT,
    FLOAT,
    // texts
    VARIABLE,
    CONSTANT,
    FUNCTION,
    // operators
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    POWER,
    // parens
    LPAREN,
    RPAREN,
    // system
    START,
    END
}
