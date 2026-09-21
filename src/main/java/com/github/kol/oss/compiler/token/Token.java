package com.github.kol.oss.compiler.token;

public record Token(
        String value,
        TokenType type,
        int position
) {
    @Override
    public String toString() {
        return type + "(" + value + ")";
    }
}
