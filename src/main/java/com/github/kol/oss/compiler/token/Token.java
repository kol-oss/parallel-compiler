package com.github.kol.oss.compiler.token;

import java.util.concurrent.atomic.AtomicLong;

public record Token(
        long id,
        String value,
        TokenType type
) {
    private static final AtomicLong NEXT_ID = new AtomicLong();

    public Token(String value, TokenType type) {
        this(NEXT_ID.getAndIncrement(), value, type);
    }
}
