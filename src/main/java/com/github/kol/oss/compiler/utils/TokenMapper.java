package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

public class TokenMapper {
    public static OperatorValue tokenToOperator(Token token) {
        TokenType tokenType = token.type();
        if (tokenType != TokenType.OPERATOR)
            throw new IllegalArgumentException("Can not create operator from token that has type " + tokenType);

        String value = token.value();
        return switch (value) {
            case "+" -> OperatorValue.PLUS;
            case "-" -> OperatorValue.MINUS;
            case "*" -> OperatorValue.MULTIPLY;
            case "/" -> OperatorValue.DIVISION;
            case "^" -> OperatorValue.EXPONENTIATION;
            case "%" -> OperatorValue.MODULO;
            case "&" -> OperatorValue.AND;
            case "|" -> OperatorValue.OP;
            default -> throw new IllegalArgumentException("Unknown operator: " + value);
        };
    }
}
