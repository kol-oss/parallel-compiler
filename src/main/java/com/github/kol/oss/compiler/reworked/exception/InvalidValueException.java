package com.github.kol.oss.compiler.reworked.exception;

import com.github.kol.oss.compiler.reworked.token.TokenType;

public class InvalidValueException extends InvalidTransitionException {
    private final String value;

    public InvalidValueException(TokenType base, TokenType next, String value) {
        super(base, next);
        this.value = value;
    }

    @Override
    public String getMessage() {
        return "Not allowed transition from token with type " + base + " into " + next + " when token value is '" + value + "'";
    }
}
