package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.*;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Syntax {
    private final ExceptionHandler exceptionHandler;
    private final Set<Integer> functionParenthesisCount = new HashSet<>();
    private TokenType lastType = TokenType.START;
    private int parenthesisCount = 0;

    public Syntax(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private void checkTransition(Token token) {
        TokenType tokenType = token.type();
        if (!SyntaxTransitions.isAllowedTransition(lastType, tokenType)) {
            InvalidTransitionException exception = new InvalidTransitionException(lastType, tokenType);
            exception.setIndex(token.position());

            exceptionHandler.add(exception);
        }
    }

    private void processToken(Token token) {
        String value = token.value();
        TokenType nextType = token.type();

        if (lastType == TokenType.START && nextType == TokenType.OPERATOR) {
            if (!value.matches("[-]")) {
                InvalidValueException exception = new InvalidValueException(lastType, nextType, value);
                exception.setIndex(token.position());

                exceptionHandler.add(exception);
            }
        }
        if (nextType == TokenType.LPAREN) {
            parenthesisCount++;

            if (lastType == TokenType.FUNCTION)
                functionParenthesisCount.add(parenthesisCount);
        } else if (nextType == TokenType.RPAREN) {
            functionParenthesisCount.remove(parenthesisCount);
            parenthesisCount--;

            if (parenthesisCount < 0) {
                InvalidParenthesisException exception = new InvalidParenthesisException(parenthesisCount);
                exception.setIndex(token.position());

                exceptionHandler.add(exception);
            }
        } else if (nextType == TokenType.COMMA) {
            if (!functionParenthesisCount.contains(parenthesisCount)) {
                InvalidCommaException exception = new InvalidCommaException();
                exception.setIndex(token.position());

                exceptionHandler.add(exception);
            }
        }

        checkTransition(token);
    }

    public void process(List<Token> tokens) {
        for (Token token : tokens) {
            processToken(token);
            lastType = token.type();
        }

        Token lastToken = tokens.getLast();
        checkTransition(new Token(lastToken.value(), TokenType.END, lastToken.position()));
    }
}
