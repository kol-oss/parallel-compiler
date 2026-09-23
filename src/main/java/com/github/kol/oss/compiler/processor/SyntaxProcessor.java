package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorState;
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

    private OperatorState operatorState = OperatorState.UNARY;

    public SyntaxProcessor(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private void processError(PositionedException exception, Token token) {
        exception.setIndex(token.position());
        exceptionHandler.add(exception);
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
                processError(exception, token);
            }

            functionParenthesisCount.remove(parenthesisCount);
            parenthesisCount--;

            if (parenthesisCount < 0) {
                InvalidParenthesisException exception = new InvalidParenthesisException(parenthesisCount);
                processError(exception, token);
            }
        } else if (nextType == TokenType.COMMA) {
            if (!functionParenthesisCount.contains(parenthesisCount)) {
                InvalidCommaException exception = new InvalidCommaException();
                processError(exception, token);
            }
        }
    }

    private void checkOperation(Token token) {
        String value = token.value();
        TokenType tokenType = token.type();

        if (tokenType == TokenType.OPERATOR) {
            if (operatorState == OperatorState.NO) {
                InvalidTransitionException exception = new InvalidTransitionException(lastType, tokenType);
                processError(exception, token);
            } else if (operatorState == OperatorState.UNARY && !value.matches(SymbolRegex.UNARY_OPERATOR)) {
                InvalidValueException exception = new InvalidValueException(lastType, tokenType, value);
                processError(exception, token);
            } else {
                operatorState = operatorState == OperatorState.ALL ?
                        OperatorState.UNARY :
                        OperatorState.NO;
            }
        } else if (tokenType == TokenType.COMMA || tokenType == TokenType.LPAREN) {
            operatorState = OperatorState.UNARY;
        } else {
            operatorState = OperatorState.ALL;
        }
    }

    private void processToken(Token token) {
        // checks unary operators
        checkOperation(token);

        // checks commas and parenthesis
        checkStructure(token);

        // checks all other transitions
        checkTransition(token);
    }

    private void clean() {
        parenthesisCount = 0;
        functionParenthesisCount.clear();

        lastType = TokenType.START;
        operatorState = OperatorState.UNARY;
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
            processError(exception, tokens.getLast());
        }

        checkTransition(new Token(lastToken.value(), TokenType.END, lastToken.position()));
    }
}
