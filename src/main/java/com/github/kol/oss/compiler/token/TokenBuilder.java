package com.github.kol.oss.compiler.token;

import com.github.kol.oss.compiler.lexer.LexerState;
import com.github.kol.oss.compiler.lexer.StateMapper;

import java.util.ArrayList;
import java.util.List;

public class TokenBuilder {
    private final StringBuilder token = new StringBuilder();
    private final List<Token> tokens = new ArrayList<>();

    public void append(char symbol) {
        token.append(symbol);
    }

    public void createToken(LexerState state) {
        if (state == LexerState.UNKNOWN || token.isEmpty())
            return;

        String value = token.toString();

        tokens.add(new Token(value, StateMapper.toTokenType(state, value)));
        token.setLength(0);
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
