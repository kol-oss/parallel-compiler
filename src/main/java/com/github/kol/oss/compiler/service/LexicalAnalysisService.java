package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorSymbol;
import com.github.kol.oss.compiler.constant.TokenLexica;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.LexicalResult;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.PositionedException;

import java.util.ArrayList;
import java.util.List;

public class LexicalAnalysisService {
    // convert expression into list of tokens
    public LexicalResult tokenize(String expression) {
        return new Lexer().run(expression);
    }

    private static final class Lexer {
        private final List<PositionedException> diagnostics = new ArrayList<>();

        private final StringBuilder buffer = new StringBuilder();
        private List<Token> tokens = new ArrayList<>();

        private TokenType lastType = TokenType.START;
        private int lastIndex = 0;

        private LexicalResult run(String expression) {
            for (int i = 0; i < expression.length(); i++) {
                char symbol = expression.charAt(i);
                processSymbol(symbol, i);
            }

            createToken();
            normalizeUnaryMinus();
            return new LexicalResult(tokens, diagnostics);
        }

        private boolean canCreateToken(TokenType tokenType) {
            if (tokenType == TokenType.SKIP)
                return true;

            // convert variable to function
            if (lastType == TokenType.VARIABLE && tokenType == TokenType.LPAREN) {
                lastType = TokenType.FUNCTION;
                return true;
            }

            // merge integer and dot
            if (lastType == TokenType.INTEGER && tokenType == TokenType.FLOAT) {
                lastType = TokenType.FLOAT;
                return false;
            }

            return lastType != tokenType || TokenLexica.UNARY_TOKENS.contains(tokenType);
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

        private void processError(PositionedException exception, int index) {
            exception.setIndex(index);
            diagnostics.add(exception);

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

        private void normalizeUnaryMinus() {
            tokens = normalizeUnaryMinus(tokens);
        }

        // normalize unary minus recursively
        private List<Token> normalizeUnaryMinus(List<Token> input) {
            List<Token> result = new ArrayList<>();

            for (int i = 0; i < input.size(); i++) {
                Token token = input.get(i);
                Token previous = i == 0 ? null : input.get(i - 1);

                if (!isUnaryMinus(token, previous)) {
                    result.add(token);
                    continue;
                }

                // find factor index
                int factorEnd = findFactorEnd(input, i + 1);
                List<Token> factor = normalizeUnaryMinus(input.subList(i + 1, factorEnd));

                if (isLeadingPosition(previous)) {
                    // add 0 - factor
                    result.add(new Token(NumberConstant.ZERO, TokenType.INTEGER, token.position()));
                    result.add(token);
                    result.addAll(factor);
                } else {
                    // add ( 0 - factor )
                    result.add(new Token(OperatorSymbol.LEFT_PARENTHESIS, TokenType.LPAREN, token.position()));
                    result.add(new Token(NumberConstant.ZERO, TokenType.INTEGER, token.position()));
                    result.add(token);
                    result.addAll(factor);
                    result.add(new Token(OperatorSymbol.RIGHT_PARENTHESIS, TokenType.RPAREN, token.position()));
                }

                i = factorEnd - 1;
            }

            return result;
        }

        private boolean isUnaryMinus(Token token, Token previous) {
            if (token.type() != TokenType.OPERATOR || !token.value().equals(OperatorSymbol.MINUS))
                return false;

            return previous == null ||
                    previous.type() == TokenType.LPAREN ||
                    previous.type().isOperator();
        }

        private boolean isLeadingPosition(Token previous) {
            return previous == null || previous.type() == TokenType.LPAREN;
        }

        private int findFactorEnd(List<Token> input, int start) {
            if (start >= input.size())
                return start;

            TokenType firstType = input.get(start).type();
            if (firstType == TokenType.LPAREN || firstType == TokenType.FUNCTION) {
                int depth = 0;
                for (int i = start; i < input.size(); i++) {
                    TokenType type = input.get(i).type();
                    if (type == TokenType.LPAREN)
                        depth++;
                    else if (type == TokenType.RPAREN && --depth == 0)
                        return i + 1;
                }
            }

            return start + 1;
        }
    }
}
