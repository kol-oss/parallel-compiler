package com.github.kol.oss.compiler.syntax;

import com.github.kol.oss.compiler.constant.SyntaxTransitions;
import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.token.Token;
import com.github.kol.oss.compiler.token.TokenType;

import java.util.List;

public class SyntaxAnalyzer {
    private TokenType currentType = TokenType.START;
    private int parenthesisCount = 0;

    private void checkTransition(Token token, TokenType nextType) {
        if (!SyntaxTransitions.isAllowedTransition(currentType, nextType)) {
            throw new SyntaxException(token, "Not allowed transition from " + currentType + " into " + nextType);
        }
    }

    private void processToken(Token token) {
        TokenType nextType = token.type();
        checkTransition(token, nextType);

        if (nextType == TokenType.LPAREN) {
            parenthesisCount++;
        } else if (nextType == TokenType.RPAREN) {
            parenthesisCount--;

            if (parenthesisCount < 0) {
                throw new SyntaxException(token, "Wrongly positioned or unnecessary closing parenthesis");
            }
        }
    }

    public void analyze(List<Token> tokens) {
        currentType = TokenType.START;
        parenthesisCount = 0;

        for (Token token : tokens) {
            processToken(token);
            currentType = token.type();
        }

        // checking the last token
        checkTransition(tokens.getLast(), TokenType.END);

        if (parenthesisCount > 0)
            throw new SyntaxException(tokens.getLast(), "Expected more closing parenthesis (" + parenthesisCount + ")");
    }
}
