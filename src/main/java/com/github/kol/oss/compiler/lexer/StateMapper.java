package com.github.kol.oss.compiler.lexer;

import com.github.kol.oss.compiler.token.TokenType;

import java.util.Set;

public class StateMapper {
    private static final Set<String> CONSTANTS = Set.of("Pi", "e");
    private static final Set<String> FUNCTIONS = Set.of("sin", "cos", "tan", "sqrt", "log");

    public static TokenType toTokenType(LexerState state, String value) {
        if (state == LexerState.INT)
            return TokenType.INT;
        else if (state == LexerState.FRACTION)
            return TokenType.FLOAT;
        else if (state == LexerState.OPERATOR) {
            switch (value) {
                case "*" -> {
                    return TokenType.MULTIPLY;
                }
                case "/" -> {
                    return TokenType.DIVIDE;
                }
                case "+" -> {
                    return TokenType.PLUS;
                }
                case "-" -> {
                    return TokenType.MINUS;
                }
                case "^" -> {
                    return TokenType.POWER;
                }
                case "(" -> {
                    return TokenType.LPAREN;
                }
                case ")" -> {
                    return TokenType.RPAREN;
                }
            }
        } else if (state == LexerState.WORD) {
            if (CONSTANTS.contains(value))
                return TokenType.CONSTANT;
            if (FUNCTIONS.contains(value))
                return TokenType.FUNCTION;

            return TokenType.VARIABLE;
        }

        throw new UnsupportedOperationException("Can not convert lexer state " + state + " with value " + value + " into token");
    }
}
