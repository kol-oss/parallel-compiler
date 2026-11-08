package com.github.kol.oss.compiler.constant;

import java.util.List;

public class VectorSystemConfig {
    public static final int ADDITION_DURATION = 1;
    public static final int SUBTRACTION_DURATION = 1;
    public static final int MULTIPLICATION_DURATION = 2;
    public static final int DIVISION_DURATION = 2;

    public static final int ADDERS = 2;
    public static final int SUBTRACTORS = 2;
    public static final int MULTIPLIERS = 2;
    public static final int DIVIDERS = 2;

    public static final int TOTAL_BLOCKS = ADDERS + SUBTRACTORS + MULTIPLIERS + DIVIDERS;

    public static final List<OperatorValue> OPERATORS = List.of(
            OperatorValue.PLUS,
            OperatorValue.MINUS,
            OperatorValue.MULTIPLY,
            OperatorValue.DIVISION
    );

    public static int blocks(OperatorValue operator) {
        return switch (operator) {
            case PLUS -> ADDERS;
            case MINUS -> SUBTRACTORS;
            case MULTIPLY -> MULTIPLIERS;
            case DIVISION -> DIVIDERS;
            default -> 0;
        };
    }

    public static int duration(OperatorValue operator) {
        return switch (operator) {
            case PLUS -> ADDITION_DURATION;
            case MINUS -> SUBTRACTION_DURATION;
            case MULTIPLY -> MULTIPLICATION_DURATION;
            case DIVISION -> DIVISION_DURATION;
            default -> 0;
        };
    }
}
