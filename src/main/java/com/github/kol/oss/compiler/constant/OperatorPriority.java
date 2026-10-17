package com.github.kol.oss.compiler.constant;

public class OperatorPriority {
    public static final int LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;

    public static int getPriority(OperatorValue operator) {
        return switch (operator) {
            case PLUS, MINUS, AND, OP -> LOW;
            case MULTIPLY, DIVISION, MODULO -> MEDIUM;
            case EXPONENTIATION -> HIGH;
        };
    }
}
