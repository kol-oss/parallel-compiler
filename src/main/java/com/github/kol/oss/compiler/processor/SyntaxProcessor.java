package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.SymbolRegex;
import com.github.kol.oss.compiler.constant.TokenTransitions;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.Token;
import com.github.kol.oss.compiler.exception.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyntaxProcessor {
    private final ExceptionHandler exceptionHandler;
    private final Set<Integer> functionParenthesisCount = new HashSet<>();
    private TokenType lastType = TokenType.START;
    private int parenthesisCount = 0;

    public SyntaxProcessor(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private void checkTransition(Token token) {
        TokenType tokenType = token.type();
        if (!TokenTransitions.isAllowedTransition(lastType, tokenType)) {
            InvalidTransitionException exception = new InvalidTransitionException(lastType, tokenType);
            exception.setIndex(token.position());

            exceptionHandler.add(exception);
        }
    }

    private void checkStructure(Token token) {
        TokenType nextType = token.type();
        if (nextType == TokenType.LPAREN) {
            parenthesisCount++;

            if (lastType == TokenType.FUNCTION)
                functionParenthesisCount.add(parenthesisCount);
        } else if (nextType == TokenType.RPAREN) {
            if (lastType == TokenType.LPAREN && !functionParenthesisCount.contains(parenthesisCount)) {
                InvalidContentException exception = new InvalidContentException();
                exception.setIndex(token.position());

                exceptionHandler.add(exception);
            }

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
    }

    private void processToken(Token token) {
        String value = token.value();
        TokenType nextType = token.type();

        if (lastType == TokenType.START && nextType == TokenType.OPERATOR) {
            if (!value.matches(SymbolRegex.START_OPERATOR)) {
                InvalidValueException exception = new InvalidValueException(lastType, nextType, value);
                exception.setIndex(token.position());

                exceptionHandler.add(exception);
            }
        }

        checkStructure(token);
        checkTransition(token);
    }

    private void clean() {
        parenthesisCount = 0;
        functionParenthesisCount.clear();

        lastType = TokenType.START;
    }

    public void process(List<Token> tokens) {
        if (tokens.isEmpty())
            return;

        clean();
        for (Token token : tokens) {
            processToken(token);
            lastType = token.type();
        }

        Token lastToken = tokens.getLast();
        if (parenthesisCount > 0) {
            InvalidParenthesisException exception = new InvalidParenthesisException(parenthesisCount);
            exception.setIndex(lastToken.position());

            exceptionHandler.add(exception);
        }

        checkTransition(new Token(lastToken.value(), TokenType.END, lastToken.position()));
    }
}
