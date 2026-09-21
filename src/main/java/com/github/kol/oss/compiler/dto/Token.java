package com.github.kol.oss.compiler.dto;

import com.github.kol.oss.compiler.constant.TokenType;

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
