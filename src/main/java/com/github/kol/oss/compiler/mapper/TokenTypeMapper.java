package com.github.kol.oss.compiler.mapper;

import com.github.kol.oss.compiler.constant.MathSymbols;
import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.lexica.LexicalState;
import com.github.kol.oss.compiler.token.TokenType;

public class TokenTypeMapper {
    public static TokenType toTokenType(LexicalState state, String value) {
        if (state == LexicalState.NUMBER)
            return TokenType.INT;

        if (state == LexicalState.FRACTION)
            return TokenType.FLOAT;

        if (state == LexicalState.OPERATOR) {
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
        }

        if (state == LexicalState.STRING) {
            if (MathSymbols.CONSTANTS.contains(value))
                return TokenType.CONSTANT;
            if (MathSymbols.FUNCTIONS.contains(value))
                return TokenType.FUNCTION;

            return TokenType.VARIABLE;
        }

        throw new LexicalException(0, "Can not convert lexer state " + state + " with value " + value + " into token");
    }
}
