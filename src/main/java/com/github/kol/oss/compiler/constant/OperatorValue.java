package com.github.kol.oss.compiler.constant;

public enum OperatorValue {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVISION,
    EXPONENTIATION,
    MODULO,
    AND,
    OP;

    public static String toString(OperatorValue operator) {
        return switch (operator) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVISION -> "/";
            case EXPONENTIATION -> "^";
            case MODULO -> "%";
            case AND -> "&";
            case OP -> "|";
        };
    }
}
