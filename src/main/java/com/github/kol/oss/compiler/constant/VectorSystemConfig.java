package com.github.kol.oss.compiler.constant;

public class VectorSystemConfig {
    public static final int ADDITION_DURATION = 1;
    public static final int SUBTRACTION_DURATION = 1;
    public static final int MULTIPLICATION_DURATION = 2;
    public static final int DIVISION_DURATION = 2;

    public static final int ADDERS = 2;
    public static final int SUBTRACTORS = 2;
    public static final int MULTIPLIERS = 1;
    public static final int DIVIDERS = 1;

    public static final int TOTAL_BLOCKS = ADDERS + SUBTRACTORS + MULTIPLIERS + DIVIDERS;

    public static int blocks(OperatorValue operator) {
        return switch (operator) {
            case PLUS -> ADDERS;
            case MINUS -> SUBTRACTORS;
            case MULTIPLY -> MULTIPLIERS;
            case DIVISION -> DIVIDERS;
            default -> 0;
        };
    }
}
