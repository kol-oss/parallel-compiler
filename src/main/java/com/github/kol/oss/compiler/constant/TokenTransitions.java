package com.github.kol.oss.compiler.constant;

import java.util.Map;
import java.util.Set;

public class TokenTransitions {
    public static final Map<TokenType, Set<TokenType>> TRANSITIONS = Map.ofEntries(
            Map.entry(TokenType.START, Set.of(
                    TokenType.INTEGER,
                    TokenType.FLOAT,
                    TokenType.OPERATOR,
                    TokenType.LPAREN,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION
            )),
            Map.entry(TokenType.INTEGER, Set.of(
                    TokenType.OPERATOR,
                    TokenType.LOGICAL_OPERATOR,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.RPAREN,
                    TokenType.COMMA,
                    TokenType.END
            )),
            Map.entry(TokenType.FLOAT, Set.of(
                    TokenType.OPERATOR,
                    TokenType.LOGICAL_OPERATOR,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.RPAREN,
                    TokenType.COMMA,
                    TokenType.END
            )),
            Map.entry(TokenType.VARIABLE, Set.of(
                    TokenType.OPERATOR,
                    TokenType.LOGICAL_OPERATOR,
                    TokenType.RPAREN,
                    TokenType.COMMA,
                    TokenType.END
            )),
            Map.entry(TokenType.FUNCTION, Set.of(
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.OPERATOR, Set.of(
                    TokenType.INTEGER,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.LPAREN,
                    TokenType.OPERATOR
            )),
            Map.entry(TokenType.LOGICAL_OPERATOR, Set.of(
                    TokenType.INTEGER,
                    TokenType.FLOAT,
                    TokenType.OPERATOR,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.COMMA, Set.of(
                    TokenType.INTEGER,
                    TokenType.FLOAT,
                    TokenType.OPERATOR,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.LPAREN, Set.of(
                    TokenType.INTEGER,
                    TokenType.FLOAT,
                    TokenType.OPERATOR,
                    TokenType.VARIABLE,
                    TokenType.FUNCTION,
                    TokenType.LPAREN,
                    TokenType.RPAREN
            )),
            Map.entry(TokenType.RPAREN, Set.of(
                    TokenType.OPERATOR,
                    TokenType.COMMA,
                    TokenType.RPAREN,
                    TokenType.END
            ))
    );

    public static boolean isAllowedTransition(TokenType base, TokenType next) {
        return TRANSITIONS.get(base).contains(next);
    }
}
