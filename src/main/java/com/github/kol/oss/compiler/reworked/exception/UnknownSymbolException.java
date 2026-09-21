package com.github.kol.oss.compiler.reworked.exception;

public class UnknownSymbolException extends PositionedException {
    private final char symbol;

    public UnknownSymbolException(char symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getMessage() {
        return "Unknown symbol '" + symbol + "'";
    }
}
