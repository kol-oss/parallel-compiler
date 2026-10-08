package com.github.kol.oss.compiler.constant;

public enum OperatorType {
    PLUS,
    MINUS,
    MULTIPLY,
    DIVISION,
    EXPONENTIATION,
    MODULO,
    AND,
    OP;

    public static OperatorType fromString(String value) {
        return switch (value) {
            case "+" -> PLUS;
            case "-" -> MINUS;
            case "*" -> MULTIPLY;
            case "/" -> DIVISION;
            case "^" -> EXPONENTIATION;
            case "%" -> MODULO;
            case "&" -> AND;
            case "|" -> OP;
            default -> throw new IllegalArgumentException("Unknown operator: " + value);
        };
    }

    public static String toString(OperatorType operator) {
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
