package com.github.kol.oss.compiler.constant;

import com.github.kol.oss.compiler.exception.InvalidSymbolException;
import com.github.kol.oss.compiler.exception.UnknownSymbolException;

public enum TokenType {
    INTEGER,
    FLOAT,
    VARIABLE,
    FUNCTION,
    OPERATOR,
    LOGICAL_OPERATOR,
    UNARY_OPERATOR,
    LPAREN,
    RPAREN,
    COMMA,
    SKIP,
    START,
    END;

    public static TokenType fromChar(TokenType state, char character) {
        String symbol = String.valueOf(character);

        // empty symbols - always SKIP
        if (symbol.matches(SymbolRegex.SKIP))
            return SKIP;

        // numbers - INTEGER, FLOAT or VARIABLE
        if (symbol.matches(SymbolRegex.NUMBER)) {
            if (state == FLOAT || state == VARIABLE)
                return state;

            return INTEGER;
        }

        // dots - FLOAT
        if (symbol.matches(SymbolRegex.FLOAT)) {
            if (state == FLOAT)
                throw new InvalidSymbolException(character);

            return FLOAT;
        }

        // unary operation symbols - OPERATOR
        if (symbol.matches(SymbolRegex.UNARY_OPERATOR)) {
            if (state == TokenType.START || state == TokenType.OPERATOR)
                return UNARY_OPERATOR;
        }

        // operation symbols - OPERATOR
        if (symbol.matches(SymbolRegex.OPERATOR))
            return OPERATOR;

        // logical symbols - LOGICAL_OPERATOR
        if (symbol.matches(SymbolRegex.LOGICAL_OPERATOR))
            return LOGICAL_OPERATOR;

        // comma - COMMA
        if (symbol.matches(SymbolRegex.COMMA))
            return COMMA;

        // left parenthesis - LPAREN
        if (symbol.matches(SymbolRegex.LPAREN))
            return LPAREN;

        // right parenthesis - RPAREN
        if (symbol.matches(SymbolRegex.RPAREN))
            return RPAREN;

        // letters and underscores - VARIABLE
        if (symbol.matches(SymbolRegex.TEXT))
            return VARIABLE;

        throw new UnknownSymbolException(character);
    }
}
