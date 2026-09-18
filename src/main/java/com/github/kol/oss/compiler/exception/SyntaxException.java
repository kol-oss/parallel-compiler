package com.github.kol.oss.compiler.exception;

import com.github.kol.oss.compiler.token.Token;

public class SyntaxException extends RuntimeException {
    private final Token token;

    public SyntaxException(Token token, String message) {
        super(message);

        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
