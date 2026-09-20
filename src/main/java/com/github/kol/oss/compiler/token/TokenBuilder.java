package com.github.kol.oss.compiler.token;

import com.github.kol.oss.compiler.lexica.LexicalState;
import com.github.kol.oss.compiler.mapper.TokenTypeMapper;

import java.util.ArrayList;
import java.util.List;

public class TokenBuilder {
    private final StringBuilder tokenString = new StringBuilder();
    private final List<Token> tokens = new ArrayList<>();

    private final boolean isDebug;

    public TokenBuilder(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public void append(char symbol) {
        tokenString.append(symbol);
    }

    public void createToken(LexicalState state) {
        if (state == LexicalState.UNKNOWN || tokenString.isEmpty())
            return;

        String value = tokenString.toString();
        Token token = new Token(value, TokenTypeMapper.toTokenType(state, value));

        tokens.add(token);
        clearToken();

        if (isDebug)
            System.out.println("> added token \"" + token.value() + "\" with type " + token.type());
    }

    public void clearToken() {
        tokenString.setLength(0);
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
