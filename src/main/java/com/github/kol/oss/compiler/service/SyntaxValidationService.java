package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorState;
import com.github.kol.oss.compiler.constant.SymbolRegex;
import com.github.kol.oss.compiler.constant.TokenTransitions;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyntaxValidationService {
    public List<PositionedException> validate(List<Token> tokens) {
        if (tokens.isEmpty())
            return List.of();

        return new Validator().run(tokens);
    }

    private static final class Validator {
        private final List<PositionedException> diagnostics = new ArrayList<>();
        private final Set<Integer> functionParenthesisCount = new HashSet<>();

        private TokenType lastType = TokenType.START;
        private int parenthesisCount = 0;

        private OperatorState operatorState = OperatorState.UNARY;

        private List<PositionedException> run(List<Token> tokens) {
            for (Token token : tokens) {
                processToken(token);
                lastType = token.type();
            }

            Token lastToken = tokens.getLast();
            // check parens
            if (parenthesisCount > 0) {
                InvalidParenthesisException exception = new InvalidParenthesisException(parenthesisCount);
                processError(exception, lastToken);
            }

            // check end
            checkTransition(new Token(lastToken.value(), TokenType.END, lastToken.position()));
            return diagnostics;
        }

        private void processToken(Token token) {
            checkOperation(token);
            checkStructure(token);
            checkTransition(token);
        }

        private void processError(PositionedException exception, Token token) {
            exception.setIndex(token.position());
            diagnostics.add(exception);
        }

        private void checkTransition(Token token) {
            TokenType tokenType = token.type();
            if (!TokenTransitions.isAllowedTransition(lastType, tokenType)) {
                InvalidTransitionException exception = new InvalidTransitionException(lastType, tokenType);
                exception.setIndex(token.position());

                diagnostics.add(exception);
            }
        }

        private void checkStructure(Token token) {
            TokenType nextType = token.type();
            if (nextType == TokenType.LPAREN) {
                parenthesisCount++;

                if (lastType == TokenType.FUNCTION)
                    functionParenthesisCount.add(parenthesisCount);
            } else if (nextType == TokenType.RPAREN) {
                // check empty parens
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

            if (tokenType.isOperator()) {
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
    }
}
