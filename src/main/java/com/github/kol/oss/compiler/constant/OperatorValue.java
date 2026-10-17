package com.github.kol.oss.compiler.constant;

public enum OperatorValue {
    PLUS(OperatorSymbol.PLUS),
    MINUS(OperatorSymbol.MINUS),
    MULTIPLY(OperatorSymbol.MULTIPLY),
    DIVISION(OperatorSymbol.DIVISION),
    EXPONENTIATION(OperatorSymbol.EXPONENTIATION),
    MODULO(OperatorSymbol.MODULO),
    AND(OperatorSymbol.AND),
    OP(OperatorSymbol.OR);

    private final String symbol;

    OperatorValue(String symbol) {
        this.symbol = symbol;
    }

    public String toSymbol() {
        return symbol;
    }

    public static OperatorValue fromSymbol(String symbol) {
        return switch (symbol) {
            case OperatorSymbol.PLUS -> PLUS;
            case OperatorSymbol.MINUS -> MINUS;
            case OperatorSymbol.MULTIPLY -> MULTIPLY;
            case OperatorSymbol.DIVISION -> DIVISION;
            case OperatorSymbol.EXPONENTIATION -> EXPONENTIATION;
            case OperatorSymbol.MODULO -> MODULO;
            case OperatorSymbol.AND -> AND;
            case OperatorSymbol.OR -> OP;
            default -> throw new IllegalArgumentException("Unknown operator: " + symbol);
        };
    }

    public boolean isSumFamily() {
        return this == PLUS || this == MINUS;
    }

    public boolean isProductFamily() {
        return this == MULTIPLY || this == DIVISION;
    }
}
