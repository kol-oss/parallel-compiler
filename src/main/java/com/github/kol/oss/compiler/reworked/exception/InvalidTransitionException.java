package com.github.kol.oss.compiler.reworked.exception;

import com.github.kol.oss.compiler.reworked.token.TokenType;

public class InvalidTransitionException extends PositionedException {
    protected final TokenType base;
    protected final TokenType next;

    public InvalidTransitionException(TokenType base, TokenType next) {
        this.base = base;
        this.next = next;
    }

    @Override
    public String getMessage() {
        return "Not allowed transition from token with type " + base + " into " + next;
    }
}
