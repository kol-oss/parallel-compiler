package com.github.kol.oss.compiler.constant;

import com.github.kol.oss.compiler.token.TokenType;

import java.util.Map;
import java.util.Set;

public final class SyntaxTransitions {
    public static final Map<TokenType, Set<TokenType>> TRANSITIONS = Map.ofEntries(
            Map.entry(TokenType.START, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN,
                    TokenType.MINUS
            )),
            Map.entry(TokenType.INT, Set.of(
                    TokenType.PLUS,
                    TokenType.MINUS,
                    TokenType.MULTIPLY,
                    TokenType.DIVIDE,
                    TokenType.POWER,
                    TokenType.RPAREN,
                    TokenType.END
            )),
            Map.entry(TokenType.FLOAT, Set.of(
                    TokenType.PLUS,
                    TokenType.MINUS,
                    TokenType.MULTIPLY,
                    TokenType.DIVIDE,
                    TokenType.POWER,
                    TokenType.RPAREN,
                    TokenType.END
            )),
            Map.entry(TokenType.VARIABLE, Set.of(
                    TokenType.PLUS,
                    TokenType.MINUS,
                    TokenType.MULTIPLY,
                    TokenType.DIVIDE,
                    TokenType.POWER,
                    TokenType.RPAREN,
                    TokenType.END
            )),
            Map.entry(TokenType.CONSTANT, Set.of(
                    TokenType.PLUS,
                    TokenType.MINUS,
                    TokenType.MULTIPLY,
                    TokenType.DIVIDE,
                    TokenType.POWER,
                    TokenType.RPAREN,
                    TokenType.END
            )),
            Map.entry(TokenType.FUNCTION, Set.of(
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.PLUS, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.MINUS, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.MULTIPLY, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.DIVIDE, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.POWER, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.LPAREN, Set.of(
                    TokenType.INT,
                    TokenType.FLOAT,
                    TokenType.VARIABLE,
                    TokenType.CONSTANT,
                    TokenType.FUNCTION,
                    TokenType.LPAREN
            )),
            Map.entry(TokenType.RPAREN, Set.of(
                    TokenType.PLUS,
                    TokenType.MINUS,
                    TokenType.MULTIPLY,
                    TokenType.DIVIDE,
                    TokenType.POWER,
                    TokenType.RPAREN,
                    TokenType.END
            ))
    );

    public static boolean isAllowedTransition(TokenType base, TokenType next) {
        return TRANSITIONS.get(base).contains(next);
    }
}
