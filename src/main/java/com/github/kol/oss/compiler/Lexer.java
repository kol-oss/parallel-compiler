package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final ExceptionHandler exceptionHandler;

    private StringBuilder buffer = new StringBuilder();
    private List<Token> tokens = new ArrayList<>();

    private TokenType lastType = TokenType.SKIP;
    private int lastIndex = 0;

    public Lexer(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private boolean canCreateToken(TokenType tokenType) {
        if (tokenType == TokenType.SKIP)
            return true;

        if (lastType == TokenType.VARIABLE && tokenType == TokenType.LPAREN) {
            lastType = TokenType.FUNCTION;
            return true;
        }

        if (lastType == TokenType.INTEGER && tokenType == TokenType.FLOAT) {
            lastType = TokenType.FLOAT;
            return false;
        }

        return lastType != tokenType || tokenType == TokenType.OPERATOR;
    }

    private void createToken() {
        if (buffer.isEmpty())
            return;

        String value = buffer.toString();
        Token token = new Token(value, lastType, lastIndex - value.length() + 1);
        tokens.add(token);

        buffer.setLength(0);
        lastType = TokenType.SKIP;
    }

    private void clean() {
        buffer = new StringBuilder();
        tokens = new ArrayList<>();

        lastType = TokenType.SKIP;
        lastIndex = 0;
    }

    private void processError(PositionedException exception, int index) {
        exception.setIndex(index);
        exceptionHandler.add(exception);

        createToken();

        lastIndex = index;

        buffer.setLength(0);
        lastType = TokenType.SKIP;
    }

    private void processSymbol(char symbol, int index) {
        TokenType tokenType;
        try {
            tokenType = TokenType.fromChar(lastType, symbol);
        } catch (PositionedException exception) {
            processError(exception, index);
            return;
        }

        if (canCreateToken(tokenType))
            createToken();

        lastIndex = index;
        lastType = tokenType;

        if (tokenType != TokenType.SKIP)
            buffer.append(symbol);
    }

    public List<Token> process(String expression) {
        clean();

        for (int i = 0; i < expression.length(); i++) {
            char symbol = expression.charAt(i);
            processSymbol(symbol, i);
        }

        createToken();
        return tokens;
    }
}
