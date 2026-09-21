package com.github.kol.oss.compiler.reworked;

import com.github.kol.oss.compiler.reworked.exception.ExceptionHandler;
import com.github.kol.oss.compiler.reworked.exception.UnknownSymbolException;
import com.github.kol.oss.compiler.reworked.token.Token;
import com.github.kol.oss.compiler.reworked.token.TokenType;

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

        return lastType != tokenType || tokenType == TokenType.OPERATOR;
    }

    private void createToken() {
        if (buffer.isEmpty())
            return;

        Token token = new Token(buffer.toString(), lastType, lastIndex);
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

    private void processError(UnknownSymbolException exception, int index) {
        exception.setIndex(index);
        exceptionHandler.add(exception);

        lastIndex = index;

        buffer.setLength(0);
        lastType = TokenType.SKIP;
    }

    private void processSymbol(char symbol, int index) {
        TokenType tokenType;
        try {
            tokenType = TokenType.fromChar(lastType, symbol);
        } catch (UnknownSymbolException exception) {
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
