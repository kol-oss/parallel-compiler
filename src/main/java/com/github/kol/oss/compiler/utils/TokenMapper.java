package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorType;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

public class TokenMapper {
    public static OperatorType tokenToOperator(Token token) {
        TokenType tokenType = token.type();
        if (tokenType != TokenType.OPERATOR)
            throw new IllegalArgumentException("Can not create operator from token that has type " + tokenType);

        String value = token.value();
        return switch (value) {
            case "+" -> OperatorType.PLUS;
            case "-" -> OperatorType.MINUS;
            case "*" -> OperatorType.MULTIPLY;
            case "/" -> OperatorType.DIVISION;
            case "^" -> OperatorType.EXPONENTIATION;
            case "%" -> OperatorType.MODULO;
            case "&" -> OperatorType.AND;
            case "|" -> OperatorType.OP;
            default -> throw new IllegalArgumentException("Unknown operator: " + value);
        };
    }
}
