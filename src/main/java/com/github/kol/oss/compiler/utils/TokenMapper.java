package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.token.Token;

public class TokenMapper {
    public static OperatorValue tokenToOperator(Token token) {
        TokenType tokenType = token.type();
        if (!tokenType.isOperator())
            throw new IllegalArgumentException("Can not create operator from token that has type " + tokenType);

        return OperatorValue.fromSymbol(token.value());
    }
}
