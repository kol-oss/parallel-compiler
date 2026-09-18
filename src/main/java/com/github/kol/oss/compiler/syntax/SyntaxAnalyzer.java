package com.github.kol.oss.compiler.syntax;

import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyntaxAnalyzer {
    private static final Map<TokenType, Set<TokenType>> TRANSITIONS = Map.ofEntries(
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

    public void analyze(List<Token> tokens) {
        TokenType currentType = TokenType.START;
        int openBrackets = 0;

        for (Token token : tokens) {
            TokenType nextType = token.type();
            if (!TRANSITIONS.get(currentType).contains(nextType)) {
                throw new SyntaxException(token, "Not allowed transition from " + currentType + " into " + nextType);
            }

            if (nextType == TokenType.LPAREN) {
                openBrackets++;
            } else if (nextType == TokenType.RPAREN) {
                openBrackets--;

                if (openBrackets < 0) {
                    throw new SyntaxException(null, "Wrongly positioned or unnecessary closing parenthesis");
                }
            }

            currentType = nextType;
        }

        if (openBrackets > 0)
            throw new SyntaxException(null, "The number of opening parenthesis is bigger than closing");
    }
}
