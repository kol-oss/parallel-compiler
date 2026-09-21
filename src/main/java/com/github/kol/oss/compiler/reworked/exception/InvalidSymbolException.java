package com.github.kol.oss.compiler.reworked.exception;

public class InvalidSymbolException extends PositionedException {
    private final char symbol;

    public InvalidSymbolException(char symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getMessage() {
        return "Can not place symbol '" + symbol + "' here";
    }
}
