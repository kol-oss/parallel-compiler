package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

public abstract class TokenNode {
    private final Token token;
    private final int weight;

    protected TokenNode(Token token, int weight) {
        this.token = token;
        this.weight = weight;
    }

    public Token getToken() {
        return token;
    }

    public int getWeight() {
        return weight;
    }
}