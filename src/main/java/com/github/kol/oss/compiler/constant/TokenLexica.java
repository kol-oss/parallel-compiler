package com.github.kol.oss.compiler.constant;

import java.util.Set;

public class TokenLexica {
    public static final Set<TokenType> UNARY_TOKENS = Set.of(
            TokenType.OPERATOR,
            TokenType.LOGICAL_OPERATOR,
            TokenType.LPAREN,
            TokenType.RPAREN
    );
}
