package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

public abstract class TokenNode {
    private final Token token;

    public TokenNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}