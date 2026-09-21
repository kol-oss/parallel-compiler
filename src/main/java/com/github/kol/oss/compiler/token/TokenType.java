package com.github.kol.oss.compiler.token;

import com.github.kol.oss.compiler.exception.InvalidSymbolException;
import com.github.kol.oss.compiler.exception.UnknownSymbolException;

public enum TokenType {
    INTEGER,
    FLOAT,
    VARIABLE,
    FUNCTION,
    OPERATOR,
    LOGICAL_OPERATOR,
    LPAREN,
    RPAREN,
    COMMA,
    SKIP,
    START,
    END;

    public static TokenType fromChar(TokenType state, char character) {
        String symbol = String.valueOf(character);

        // empty symbols - always SKIP
        if (symbol.matches("[ ]"))
            return SKIP;

        // numbers - INTEGER, FLOAT or VARIABLE
        if (symbol.matches("[0-9]")) {
            if (state == FLOAT || state == VARIABLE)
                return state;

            return INTEGER;
        }

        // dots - FLOAT
        if (symbol.matches("[.]")) {
            if (state == FLOAT)
                throw new InvalidSymbolException(character);

            return FLOAT;
        }

        // operation symbols - OPERATOR
        if (symbol.matches("[+\\-/*^%]"))
            return OPERATOR;

        // logical symbols - LOGICAL_OPERATOR
        if (symbol.matches("[&|!]"))
            return LOGICAL_OPERATOR;

        // comma - COMMA
        if (symbol.matches("[,]"))
            return COMMA;

        // left parenthesis - LPAREN
        if (symbol.matches("[(]"))
            return LPAREN;

        // right parenthesis - RPAREN
        if (symbol.matches("[)]"))
            return RPAREN;

        // letters and underscores - VARIABLE
        if (symbol.matches("[A-Za-z_]"))
            return VARIABLE;

        throw new UnknownSymbolException(character);
    }
}
